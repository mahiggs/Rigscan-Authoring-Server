package com.epiroc.rigscan.authoringserver.authentication

import com.auth0.jwk.GuavaCachedJwkProvider
import com.auth0.jwk.UrlJwkProvider
import com.epiroc.rigscan.authoringserver.configuration.BearerTokenHolder
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.jwt.JwtHelper
import org.springframework.security.jwt.crypto.sign.RsaVerifier
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import java.security.interfaces.RSAPublicKey
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Provider
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class AzureActiveDirectoryB2CFilter(defaultFilterProcessesUrl: String?,
                                    val properties: B2CProperties,
                                    val bearerTokenHolderProvider: Provider<BearerTokenHolder>,
                                    val bearerTokenHandler: BearerTokenHandler,
                                    val userRepository: UserRepository) : AbstractAuthenticationProcessingFilter(defaultFilterProcessesUrl) {

    init {
        authenticationManager = NoopAuthenticationManager()
    }

    override fun attemptAuthentication(request: HttpServletRequest?, response: HttpServletResponse?): Authentication {
        val parameterNames = request?.parameterNames?.toList()?.toHashSet() ?: setOf<String>()

        if (request == null || response == null) {
            throw IllegalArgumentException("Request and response must be non-null.")
        }

        // if we are receiving the result of the redirect and the token has been returned
        // then we need to process it
        if (parameterNames.contains("id_token")) {
            val idToken = request.parameterMap["id_token"] as Array<String>

            val headers = JwtHelper.headers(idToken.firstOrNull())
            val keyId = headers["kid"]
            val decodedToken = JwtHelper.decodeAndVerify(idToken.firstOrNull(), verifier(keyId!!))
            val decodedClaims = ObjectMapper().readValue(decodedToken.claims, Map::class.java)

            val storedNonce = request.session.getAttribute("authNonce")

            if (storedNonce == null || storedNonce !is String) {
                throw RuntimeException("Unable to find stored nonce in session.")
            }

            verifyClaims(decodedClaims, storedNonce)

            // if we have successfully verified the nonce, we can remove it from the session
            request.session.removeAttribute("authNonce")

            if (decodedClaims[properties.userIdClaimName] !is String) {
                throw IllegalArgumentException("Claim value is not a string: ${decodedClaims[properties.userIdClaimName]} [class=${decodedClaims[properties.userIdClaimName]!!::class.java}")
            }

            val bearerTokenHolder = this.bearerTokenHolderProvider.get()

            val dbUser = userRepository.findByUserLogin(decodedClaims[properties.userIdClaimName] as String)
                    ?: throw IllegalArgumentException("Unrecognized user: ${decodedClaims[properties.userIdClaimName]}")

            bearerTokenHolder.bearerToken = (request.parameterMap["access_token"] as Array<String>)[0]
            val expiresInAsStringArray = request.parameterMap["expires_in"] as Array<String>
            bearerTokenHolder.bearerTokenExpires = ZonedDateTime.now().plusSeconds(expiresInAsStringArray[0].toLong())

            this.bearerTokenHandler.authorizeByCode(bearerTokenHolder,
                    request.getParameter("code"))

            val user = UserBasedUserDetails(dbUser)

            return UsernamePasswordAuthenticationToken(user, null, user.authorities)
        } else {
            // otherwise we need to send the user to AAD B2C so that they can authenticate
            // create and set the nonce in the session so that we can check it when it is returned
            val nonce = UUID.randomUUID().toString()
            request.session.setAttribute("authNonce", nonce)

            val redirectParameters = mapOf(
                    "p" to properties.signInPolicyId,
                    "client_id" to properties.clientId,
                    "nonce" to nonce,
                    "redirect_uri" to properties.redirectUri,
                    "scope" to properties.scopes,
                    "response_type" to "code id_token token",
                    "prompt" to "login",
                    "response_mode" to "query"
            )

            throw UserRedirectRequiredException(properties.userAuthorizationUri, redirectParameters)
        }
    }

    private fun verifyClaims(claims: Map<*, *>, nonce: String) {
        val exp = (claims["exp"] as Int).toLong()
        val expireDate = LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC)
        val nbf = (claims["nbf"] as Int).toLong()
        val notBeforeDate = LocalDateTime.ofEpochSecond(nbf, 0, ZoneOffset.UTC)
        val now = LocalDateTime.now(ZoneOffset.UTC)

        if (nonce != claims["nonce"]) {
            throw RuntimeException("Nonce is not valid: nonce=$nonce, claimNonce=${claims["nonce"]}")
        }
        if (now.isBefore(notBeforeDate)) {
            throw RuntimeException("Token is not yet valid: nbf=$notBeforeDate")
        }
        if (expireDate.isBefore(now)) {
            throw RuntimeException("Token has expired: exp=$expireDate")
        }
        if (claims["iss"] != properties.issuer) {
            throw RuntimeException("Unrecognized issuer: expected=${properties.issuer}, actual=${claims["iss"]}")
        }
        if (claims["aud"] != properties.clientId) {
            throw RuntimeException("Invalid audience: expected=${properties.clientId}, actual=${claims["aud"]}")
        }
    }

    private fun verifier(kid: String): RsaVerifier {
        val provider = GuavaCachedJwkProvider(UrlJwkProvider(properties.jwksUri))
        val jwk = provider[kid]
        return RsaVerifier(jwk.publicKey as RSAPublicKey)
    }

    class NoopAuthenticationManager : AuthenticationManager {
        override fun authenticate(authentication: Authentication?): Authentication {
            throw UnsupportedOperationException("No authentication should be done with this AuthenticationManager");
        }
    }
}
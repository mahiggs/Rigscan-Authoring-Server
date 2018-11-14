package com.epiroc.rigscan.authoringserver.authentication

import com.auth0.jwk.GuavaCachedJwkProvider
import com.auth0.jwk.UrlJwkProvider
import com.epiroc.rigscan.authoringserver.controllers.api.UnauthorizedException
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.jwt.JwtHelper
import org.springframework.security.jwt.crypto.sign.RsaVerifier
import org.springframework.web.filter.OncePerRequestFilter
import java.security.interfaces.RSAPublicKey
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class BearerTokenHandlerFilter(private val properties: B2CProperties,
                               private val userRepository: UserRepository) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authorizationHeader = request.getHeader("Authorization")

        request.setAttribute("originalRequestURI", request.requestURI)

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)

            return
        }

        val bearerToken = authorizationHeader.replace("Bearer ", "")

        val headers = JwtHelper.headers(bearerToken)
        val keyId = headers["kid"]
        val decodedToken = JwtHelper.decodeAndVerify(bearerToken, verifier(keyId!!))
        val decodedClaims = ObjectMapper().readValue(decodedToken.claims, Map::class.java)

        verifyClaims(decodedClaims)

        // if we have successfully verified the nonce, we can remove it from the session
        request.session.removeAttribute("authNonce")

        if (decodedClaims[properties.userIdClaimName] !is String) {
            throw IllegalArgumentException("Claim value is not a string: ${decodedClaims[properties.userIdClaimName]} [class=${decodedClaims[properties.userIdClaimName]!!::class.java}")
        }

        val dbUser = userRepository.findByUserLogin(decodedClaims[properties.userIdClaimName] as String)
                ?: throw IllegalArgumentException("Unrecognized user: ${decodedClaims[properties.userIdClaimName]}")

        val user = UserBasedUserDetails(dbUser)

        val securityContext = SecurityContextHolder.getContext()
        val previousAuthentication = securityContext.authentication
        securityContext.authentication = UsernamePasswordAuthenticationToken(user, null, user.authorities)

        filterChain.doFilter(request, response)

        securityContext.authentication = previousAuthentication
    }

    private fun verifyClaims(claims: Map<*, *>) {
        val exp = (claims["exp"] as Int).toLong()
        val expireDate = LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC)
        val nbf = (claims["nbf"] as Int).toLong()
        val notBeforeDate = LocalDateTime.ofEpochSecond(nbf, 0, ZoneOffset.UTC)
        val now = LocalDateTime.now(ZoneOffset.UTC)

        if (now.isBefore(notBeforeDate)) {
            throw UnauthorizedException("Token is not yet valid: nbf=$notBeforeDate")
        }
        if (expireDate.isBefore(now)) {
            throw UnauthorizedException("Token has expired: exp=$expireDate")
        }
        if (claims["iss"] != properties.issuer) {
            throw UnauthorizedException("Unrecognized issuer: expected=${properties.issuer}, actual=${claims["iss"]}")
        }
        if (claims["aud"] != properties.clientId) {
            throw UnauthorizedException("Invalid audience: expected=${properties.clientId}, actual=${claims["aud"]}")
        }
    }

    private fun verifier(kid: String): RsaVerifier {
        val provider = GuavaCachedJwkProvider(UrlJwkProvider(properties.jwksUri))
        val jwk = provider[kid]
        return RsaVerifier(jwk.publicKey as RSAPublicKey)
    }
}
package com.epiroc.rigscan.authoringserver.authentication

import com.epiroc.rigscan.authoringserver.configuration.BearerTokenHolder
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Lists
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class BearerTokenHandler(val client: CloseableHttpClient, val properties: B2CProperties, val mapper: ObjectMapper) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BearerTokenHandler::class.java)
    }

    fun authorizeByCode(bearerTokenHolder: BearerTokenHolder, authorizationCode: String) {
        val httpPost = HttpPost(properties.tokenUri)

        val attrs = Lists.newArrayList<NameValuePair>()
        attrs.add(BasicNameValuePair("p", properties.signInPolicyId)) //$NON-NLS-1$
        attrs.add(BasicNameValuePair("client_id", properties.clientId)) //$NON-NLS-1$
        attrs.add(BasicNameValuePair("grant_type", "authorization_code")) //$NON-NLS-1$ //$NON-NLS-2$
        attrs.add(BasicNameValuePair("scope", properties.scopes)) //$NON-NLS-1$
        attrs.add(BasicNameValuePair("response_type", "token")) //$NON-NLS-1$ //$NON-NLS-2$
        attrs.add(BasicNameValuePair("code", authorizationCode))
        attrs.add(BasicNameValuePair("redirect_uri", properties.redirectUri))
        attrs.add(BasicNameValuePair("client_secret", properties.clientSecret))

        try {
            httpPost.entity = UrlEncodedFormEntity(attrs)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("Unable to encode entity.", e) //$NON-NLS-1$
        }

        try {
            client.execute(httpPost).use { response ->
                // get the response body as a string
                val responseBody = EntityUtils.toString(response.entity, StandardCharsets.UTF_8)

                // if the request failed, then we need to print out the response and indicate error
                if (response.statusLine.statusCode != 200
                        || !response.getFirstHeader("Content-Type").value.contains("application/json")) { //$NON-NLS-1$ //$NON-NLS-2$
                    log.error("An error occurred while attempting to get new bearer token from refresh token, response: {}", responseBody) //$NON-NLS-1$
                    bearerTokenHolder.clear()
                }

                val policyResponse = this.mapper.readValue(responseBody, BearerTokenRefreshResponse::class.java)

                bearerTokenHolder.bearerToken = policyResponse.accessToken
                bearerTokenHolder.bearerTokenExpires = ZonedDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(policyResponse.expiresIn)
                bearerTokenHolder.refreshToken = policyResponse.refreshToken
                bearerTokenHolder.refreshTokenExpires = ZonedDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(policyResponse.refreshTokenExpiresIn)
            }
        } catch (e: IOException) {
            log.error("An error occurred when attempting to retrieve bearer token with authorization code.", e)
            bearerTokenHolder.clear()
        }

    }

    fun refreshAuthenticationIfNecessary(bearerTokenHolder: BearerTokenHolder) {
        // we can't refresh authentication, if we have never been authenticated
        val bearerTokenExpires = bearerTokenHolder.bearerTokenExpires ?: return

        val now = ZonedDateTime.now(ZoneOffset.UTC)

        // we will refresh the token 5 minutes before we need to, just to be safe with possible time inaccuracies
        if (now.isAfter(bearerTokenExpires.minusMinutes(5))) {
            this.refreshAuthentication(bearerTokenHolder)
        }
    }

    private fun refreshAuthentication(bearerTokenHolder: BearerTokenHolder) {
        // if we don't have a refresh token we can't refresh the auth
        val refreshTokenExpiresAt = bearerTokenHolder.refreshTokenExpires

        if (bearerTokenHolder.refreshToken == null || refreshTokenExpiresAt == null) {
            return
        }

        // if we are past the expiration of the refresh token, then we can't refresh the auth and we need to
        // manually refresh.
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        if (now.isAfter(refreshTokenExpiresAt)) {
            // since the refresh token has timed out, we need to completely refresh
            bearerTokenHolder.clear()
            return
        }

        val httpPost = HttpPost(properties.tokenUri)

        val attrs = Lists.newArrayList<NameValuePair>()
        attrs.add(BasicNameValuePair("p", properties.signInPolicyId)) //$NON-NLS-1$
        attrs.add(BasicNameValuePair("client_id", properties.clientId)) //$NON-NLS-1$
        attrs.add(BasicNameValuePair("grant_type", "refresh_token")) //$NON-NLS-1$ //$NON-NLS-2$
        attrs.add(BasicNameValuePair("scope", properties.scopes)) //$NON-NLS-1$
        attrs.add(BasicNameValuePair("redirect_uri", properties.redirectUri))
        attrs.add(BasicNameValuePair("refresh_token", bearerTokenHolder.refreshToken)) //$NON-NLS-1$
        attrs.add(BasicNameValuePair("client_secret", properties.clientSecret))
        attrs.add(BasicNameValuePair("response_type", "token")) //$NON-NLS-1$ //$NON-NLS-2$

        try {
            httpPost.entity = UrlEncodedFormEntity(attrs)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("Unable to encode entity.", e) //$NON-NLS-1$
        }

        try {
            (client.execute(httpPost) as CloseableHttpResponse).use { response ->
                // get the response body as a string
                val responseBody = EntityUtils.toString(response.entity, StandardCharsets.UTF_8)

                // if the request failed, then we need to print out the response and indicate error
                if (response.statusLine.statusCode != 200
                        || !response.getFirstHeader("Content-Type").value.contains("application/json")) { //$NON-NLS-1$ //$NON-NLS-2$
                    log.error("An error occurred while attempting to get new bearer token from refresh token, response: {}", responseBody) //$NON-NLS-1$
                    bearerTokenHolder.clear()
                }

                val policyResponse = this.mapper.readValue(responseBody, BearerTokenRefreshResponse::class.java)

                bearerTokenHolder.bearerToken = policyResponse.accessToken
                bearerTokenHolder.bearerTokenExpires = ZonedDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(policyResponse.expiresIn)
                bearerTokenHolder.refreshToken = policyResponse.refreshToken
                bearerTokenHolder.refreshTokenExpires = ZonedDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(policyResponse.refreshTokenExpiresIn)
            }
        } catch (e: IOException) {
            bearerTokenHolder.clear()
        }

    }
}

class BearerTokenRefreshResponse {
    @get:JsonProperty("access_token")
    var accessToken: String? = null
    @get:JsonProperty("id_token")
    var idToken: String? = null
    @get:JsonProperty("token_type")
    var tokenType: String? = null
    /**
     * This token is not valid before this time.
     */
    @get:JsonProperty("not_before")
    var notBefore: Long = 0
    /**
     * The token will expire in this number of seconds
     */
    @get:JsonProperty("expires_in")
    var expiresIn: Long = 0
    /**
     * The token is not valid after this time.
     */
    @get:JsonProperty("expires_on")
    var expiresOn: Long = 0
    var resource: String? = null
    /**
     * The number of seconds after which the id token will expire.
     */
    @get:JsonProperty("id_token_expires_in")
    var idTokenExpiresIn: Long = 0
    @get:JsonProperty("profile_info")
    var profileInfo: String? = null
    /**
     * The refresh token for this.
     */
    @get:JsonProperty("refresh_token")
    var refreshToken: String? = null
    /**
     * The number of seconds it will take the refresh token to expire.
     */
    @get:JsonProperty("refresh_token_expires_in")
    var refreshTokenExpiresIn: Long = 0
}
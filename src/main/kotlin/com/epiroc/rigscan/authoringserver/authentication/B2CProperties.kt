package com.epiroc.rigscan.authoringserver.authentication

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.net.URL

@ConfigurationProperties("b2c")
class B2CProperties {

    /**
     * The client id for the Rigscan Authoring Server.
     */
    lateinit var clientId : String
    /**
     * The client secret used for secret authorization flows.
     */
    lateinit var clientSecret : String
    /**
     * The policy id of the sign-in policy
     */
    lateinit var signInPolicyId : String
    /**
     * The URI redirected to for the user to authenticate at.
     */
    lateinit var userAuthorizationUri : String
    /**
     * The URI redirected to in order to log the user out on the ADB2C side.
     */
    lateinit var logoutUri: String
    /**
     * The URI that ADB2C should redirect to when complete signing user out.
     */
    lateinit var postLogoutRedirectUri : String
    /**
     * The redirect URI for the Rigscan Authoring Server.
     */
    lateinit var redirectUri : String
    /**
     * The issuer that the token will be issued by. We will validate the token by that.
     */
    lateinit var issuer : String
    /**
     * The URI where the web keys used for signing the tokens are.
     */
    lateinit var jwksUri : URL
    /**
     * The claim under which the user id we use to look up users is stored.
     */
    lateinit var userIdClaimName : String
    /**
     * The scopes that we should request from ADB2C
     */
    lateinit var scopes : String
    /**
     * The URI we should hit in order to do the authorization code flow and use the
     * refresh tokens.
     */
    lateinit var tokenUri: String
}

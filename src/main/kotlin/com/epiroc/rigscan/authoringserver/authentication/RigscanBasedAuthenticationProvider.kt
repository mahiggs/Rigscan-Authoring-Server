package com.epiroc.rigscan.authoringserver.authentication

import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class RigscanBasedAuthenticationProvider(private val properties: AuthenticationProperties, private val template: RestTemplate,
                                         private val userRepository: UserRepository) : AbstractUserDetailsAuthenticationProvider() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RigscanBasedAuthenticationProvider::class.java)
    }

    override fun retrieveUser(username: String?, authentication: UsernamePasswordAuthenticationToken?): UserDetails {
        // retrieve details from database, TBD
        val user = this.userRepository.findByUserLogin(username)

        if (user == null) {
            throw UsernameNotFoundException("Username $username not found.")
        }

        return UserBasedUserDetails(user)
    }

    override fun additionalAuthenticationChecks(userDetails: UserDetails?, authentication: UsernamePasswordAuthenticationToken?) {
        if (userDetails?.username == null || authentication?.credentials == null) {
            throw BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"))
        }

        val loginResult = this.template.postForObject(apiEndpoint("/Account/ApiLogin"),
                LoginModel(userDetails.username, authentication.credentials.toString(), false),
                LoginResultV1::class.java)

        if (loginResult?.result != OblixLoginResult.UserAuthorized) {
            throw BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"))
        }
    }

    private fun apiEndpoint(relativePath: String): String {
        return this.properties.server.url + if (relativePath[0] == '/') {
            relativePath.substring(1)
        } else {
            relativePath
        }
    }

}

data class LoginModel(val userName: String, val password: String, val rememberMe: Boolean)

class LoginResultV1 {
    var result: OblixLoginResult? = null
    var userId: Int? = null
}

enum class OblixLoginResult {
    /**
     * The user is authorized to access the application.
     */
    UserAuthorized,
    /**
     * The user successfully authenticated against the Oblix server, but was not authorized for this application.
     */
    UserAuthenticatedNotAuthorized,
    /**
     * The user is currently using a temporary password and they must set a permanent password before they are able to be authenticated.
     */
    UserMustSetPassword,
    /**
     * The user entered the wrong password for the given username.
     */
    WrongPassword,
    /**
     * The user's account is currently locked out.
     */
    LockedOut,
    /**
     * The necessary configuration for the oblix server was not done.
     */
    OblixNotConfigured,
    /**
     * The Oblix server failed to respond correctly.
     */
    OblixServerNotResponding,
    /**
     * Some other failure occurred.
     */
    OtherFailure
}
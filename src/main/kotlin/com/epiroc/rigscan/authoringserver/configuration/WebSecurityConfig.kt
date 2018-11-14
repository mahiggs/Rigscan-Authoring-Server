package com.epiroc.rigscan.authoringserver.configuration

import com.epiroc.rigscan.authoringserver.authentication.AzureActiveDirectoryB2CFilter
import com.epiroc.rigscan.authoringserver.authentication.B2CProperties
import com.epiroc.rigscan.authoringserver.authentication.BearerTokenHandler
import com.epiroc.rigscan.authoringserver.authentication.BearerTokenHandlerFilter
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.*
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect
import java.time.ZonedDateTime
import javax.inject.Provider
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Configuration
@EnableWebSecurity
class WebSecurityConfig(val properties: B2CProperties, val repository: UserRepository,
                        val bearerTokenHolder: Provider<BearerTokenHolder>, val bearerTokenHandler: BearerTokenHandler) : WebSecurityConfigurerAdapter(false) {

    @Bean
    fun openIdConnectFilter(): AzureActiveDirectoryB2CFilter {
        return AzureActiveDirectoryB2CFilter("/oauth2/login/reply", properties,
                bearerTokenHolder, bearerTokenHandler, repository)
    }

    @Bean
    fun bearerTokenHandlerFilter() : BearerTokenHandlerFilter {
        return BearerTokenHandlerFilter(properties, repository)
    }

    override fun configure(http: HttpSecurity) {
        http
                .addFilterAfter(OAuth2ClientContextFilter(),
                        AbstractPreAuthenticatedProcessingFilter::class.java)
                .addFilterAfter(openIdConnectFilter(),
                        OAuth2ClientContextFilter::class.java)
                .addFilterAfter(bearerTokenHandlerFilter(),
                        AzureActiveDirectoryB2CFilter::class.java)
                .authorizeRequests()!!
                // allow all people to access /, /favicon.ico, and any of the static files
                .antMatchers("/", "/favicon.ico", "/static/**", "/webjars/**")!!.permitAll()!!
                // require all other requests to be authenticated
                .anyRequest().authenticated()
                .and()
                // allow all users to access the login page
                .httpBasic().authenticationEntryPoint(ApiIgnoringAuthenticationEntryPoint())
                .and()
//                .formLogin().loginPage("/login")!!.permitAll()
//                .and()
                // do not require CSRF on the API
                .csrf().ignoringAntMatchers("/api/**", "/login")
                .and()
                // allow all users to access logout
                .logout().permitAll()
                .logoutSuccessUrl(properties.logoutUri + "&post_logout_redirect_uri=${properties.postLogoutRedirectUri}")

    }

    inner class ApiIgnoringAuthenticationEntryPoint : LoginUrlAuthenticationEntryPoint("/oauth2/login/reply") {
        override fun commence(request: HttpServletRequest?, response: HttpServletResponse?, authException: AuthenticationException?) {
            val originalRequestURI = request?.getAttribute("originalRequestURI") as String?

            if (originalRequestURI != null && originalRequestURI.contains("/api/")) {
                response?.sendError(401, "Access denied.")
            } else {
                super.commence(request, response, authException)
            }
        }
    }

    @Bean
    @Primary
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder.defaultMessageConverters()
                .build()
    }

    @Bean
    fun springSecurityDialect(): SpringSecurityDialect {
        return SpringSecurityDialect()
    }
}

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
class BearerTokenHolder {
    var bearerToken: String? = null
    var bearerTokenExpires: ZonedDateTime? = null
    var refreshToken: String? = null
    var refreshTokenExpires: ZonedDateTime? = null

    fun clear() {
        this.bearerToken = null
        this.bearerTokenExpires = null
        this.refreshToken = null
        this.refreshTokenExpires = null
    }
}
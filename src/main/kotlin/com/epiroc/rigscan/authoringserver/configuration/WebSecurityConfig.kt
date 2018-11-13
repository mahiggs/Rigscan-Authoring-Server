package com.epiroc.rigscan.authoringserver.configuration

import com.epiroc.rigscan.authoringserver.authentication.AzureActiveDirectoryB2CFilter
import com.epiroc.rigscan.authoringserver.authentication.B2CProperties
import com.epiroc.rigscan.authoringserver.authentication.BearerTokenHandler
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.*
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect
import java.time.ZonedDateTime
import javax.inject.Provider


@Configuration
@EnableWebSecurity
class WebSecurityConfig(val properties: B2CProperties, val repository: UserRepository,
                        val bearerTokenHolder: Provider<BearerTokenHolder>, val bearerTokenHandler: BearerTokenHandler) : WebSecurityConfigurerAdapter(false) {

    @Bean
    fun openIdConnectFilter(): AzureActiveDirectoryB2CFilter {
        return AzureActiveDirectoryB2CFilter("/oauth2/login/reply", properties,
                bearerTokenHolder, bearerTokenHandler, repository)
    }

    override fun configure(http: HttpSecurity) {
        http
                .addFilterAfter(OAuth2ClientContextFilter(),
                        AbstractPreAuthenticatedProcessingFilter::class.java)
                .addFilterAfter(openIdConnectFilter(),
                        OAuth2ClientContextFilter::class.java)
                .authorizeRequests()!!
                // allow all people to access /, /favicon.ico, and any of the static files
                .antMatchers("/", "/favicon.ico", "/static/**", "/webjars/**")!!.permitAll()!!
                // require all other requests to be authenticated
                .anyRequest().authenticated()
                .and()
                // allow all users to access the login page
                .httpBasic().authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/oauth2/login/reply"))
                .and()
//                .formLogin().loginPage("/login")!!.permitAll()
//                .and()
                // do not require CSRF on the API
                .csrf().ignoringAntMatchers("/api/**", "/login")
                .and()
                // allow all users to access logout
                .logout().permitAll()
                .logoutSuccessUrl(properties.logoutUri + "&post_logout_redirect_uri=${properties.postLogoutRedirectUri}")
                .and()

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
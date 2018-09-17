package com.epiroc.rigscan.authoringserver.configuration

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter(false) {

    override fun configure(http: HttpSecurity?) {
        http!!.authorizeRequests()!!
                .antMatchers("/", "/favicon.ico", "/static/**", "/webjars/**")!!.permitAll()!!
                .anyRequest().authenticated()
                .and()
                .formLogin().loginPage("/login")!!.permitAll()
                .and()
                .logout()
                .permitAll()
    }

    @Bean
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
class AuthCookieHolder {
    var authCookie: String? = null
}
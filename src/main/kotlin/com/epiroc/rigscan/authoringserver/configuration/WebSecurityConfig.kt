package com.epiroc.rigscan.authoringserver.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter(false) {
    override fun configure(http: HttpSecurity?) {
        super.configure(http)
    }

    @Bean
    override fun userDetailsService(): UserDetailsService {
        val userDetails = User.withDefaultPasswordEncoder().username("user")
                .password("password")
                .roles("USER")
                .build()

        return InMemoryUserDetailsManager(userDetails)
    }
}
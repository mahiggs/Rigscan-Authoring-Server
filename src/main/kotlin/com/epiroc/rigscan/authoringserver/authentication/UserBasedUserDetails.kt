package com.epiroc.rigscan.authoringserver.authentication

import com.epiroc.rigscan.authoringserver.db.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserBasedUserDetails(val user: User) : UserDetails {
    override fun getUsername(): String = this.user.userLogin!!

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = this.user.roles.map { SimpleGrantedAuthority(it) }.toMutableList()

    override fun isEnabled(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun getPassword(): String = "NOT APPLICABLE"

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true
}
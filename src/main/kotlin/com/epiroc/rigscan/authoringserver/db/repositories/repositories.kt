package com.epiroc.rigscan.authoringserver.db.repositories

import com.epiroc.rigscan.authoringserver.authentication.UserBasedUserDetails
import com.epiroc.rigscan.authoringserver.controllers.api.ForbiddenException
import com.epiroc.rigscan.authoringserver.db.User
import org.springframework.data.repository.CrudRepository
import org.springframework.security.core.context.SecurityContextHolder

interface UserRepository : CrudRepository<User, Long> {
    fun findByUserLogin(login: String?): User?
}

/**
 * Fetch the current user.
 */
fun UserRepository.currentUser(): User {
    val principal = SecurityContextHolder.getContext().authentication.principal as UserBasedUserDetails

    return this.findById(principal.user.id!!).orElseThrow { ForbiddenException() }
}
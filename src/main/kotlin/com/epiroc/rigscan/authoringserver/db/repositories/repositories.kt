package com.epiroc.rigscan.authoringserver.db.repositories

import com.epiroc.rigscan.authoringserver.db.User
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long> {
    fun findByUserLogin(login: String?) : User?
}
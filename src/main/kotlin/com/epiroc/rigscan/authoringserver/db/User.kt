package com.epiroc.rigscan.authoringserver.db

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "users")
class User {
    @get:Id
    var id: Long? = null
    @get:Column(name = "user_name")
    var userName: String? = null
    @get:Column(name = "user_login")
    var userLogin: String? = null
    @get:ElementCollection(fetch = FetchType.EAGER)
    @get:CollectionTable(
            name = "user_roles",
            joinColumns = [JoinColumn(name = "user_id")]
    )
    @get:Column(name = "user_role")
    var roles: List<String?> = ArrayList()
    @get:Column(name = "created_at")
    var createdAt: LocalDateTime? = null
    @get:Column(name = "modified_at")
    var modifiedAt: LocalDateTime? = null
}
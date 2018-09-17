package com.epiroc.rigscan.authoringserver.db

import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name = "users")
class User {
    @get:Id
    var id: Long? = null
    @get:Column(name = "user_name")
    @get:NotBlank
    var userName: String? = null
    @get:Column(name = "user_login")
    @get:NotBlank
    var userLogin: String? = null
    @get:ElementCollection(fetch = FetchType.EAGER)
    @get:CollectionTable(
            name = "user_roles",
            joinColumns = [JoinColumn(name = "user_id")]
    )
    @get:Column(name = "user_role")
    @get:Size(min = 1, message = "User must have at least one role assigned.")
    var roles: List<String?> = ArrayList()
    @get:Column(name = "created_at")
    @get:NotNull
    var createdAt: LocalDateTime? = null
    @get:Column(name = "modified_at")
    @get:NotNull
    var modifiedAt: LocalDateTime? = null
}
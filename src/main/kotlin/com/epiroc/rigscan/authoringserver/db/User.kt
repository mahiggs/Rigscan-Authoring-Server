package com.epiroc.rigscan.authoringserver.db

import com.sun.org.apache.xpath.internal.operations.Bool
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
    @get:ElementCollection(fetch = FetchType.EAGER)
    @get:CollectionTable(
            name = "user_clients",
            joinColumns = [JoinColumn(name = "user_id")]
    )
    @get:Column(name = "client_id")
    var clients: MutableSet<String?> = HashSet()
    @get:Column(name = "created_at")
    @get:NotNull
    var createdAt: LocalDateTime? = null
    @get:Column(name = "modified_at")
    @get:NotNull
    var modifiedAt: LocalDateTime? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }


}

fun User.isAdministrator() : Boolean {
    return this.roles.contains("ADMINISTRATOR")
}
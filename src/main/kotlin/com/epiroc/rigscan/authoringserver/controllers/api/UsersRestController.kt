package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.db.User
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.epiroc.rigscan.authoringserver.db.repositories.currentUser
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.util.UriComponentsBuilder
import java.sql.Types
import java.time.ZonedDateTime
import javax.validation.Valid

@RestController
@RequestMapping("/api/users")
class UsersRestController(val repository: UserRepository, val template: JdbcTemplate) : ResponseEntityExceptionHandler() {

    @GetMapping
    fun listUsers(): MutableIterable<User> {
        return this.repository.findAll()
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): User {
        val possibleUser = this.repository.findById(id)

        // throw a 404 if the user ID does not exist
        if (!possibleUser.isPresent) {
            throw ResourceNotFoundException()
        }

        return possibleUser.get()
    }

    @GetMapping("/whoami")
    fun whoAmI() : User {
        return this.repository.currentUser()
    }

    @PostMapping
    fun createUser(@Valid @RequestBody user: User, builder: UriComponentsBuilder): ResponseEntity<Any> {
        this.repository.save(user)

        val location = builder.path("/api/users/{id}").buildAndExpand(user.id)

        return ResponseEntity.created(location.toUri()).build()
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: Long, @Valid @RequestBody user: User) {
        val possibleUser = this.repository.findById(id)

        // throw a 404 if the user does not exist
        if (!possibleUser.isPresent) {
            throw ResourceNotFoundException()
        }

        val databaseUser = possibleUser.get()

        databaseUser.userName = user.userName
        databaseUser.roles = user.roles

        this.repository.save(databaseUser)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long) {
        val item = this.repository.findById(id)

        if (item.isPresent) {
            // before we delete the user, we need to check in their audit protocols
            this.template.update("DELETE FROM audit_protocol_checkouts WHERE checked_out_by=?", arrayOf(id),
                    intArrayOf(Types.INTEGER))

            // delete the user
            this.repository.delete(item.get())
        } else {
            throw ResourceNotFoundException()
        }
    }

    override fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val fieldErrors = ex.bindingResult.fieldErrors
                .asSequence()
                .map { FieldError(it.field, it.defaultMessage ?: "unknown error") }
                .toList()

        return ResponseEntity(ErrorDetails(ZonedDateTime.now(), "Validation failed", fieldErrors),
                HttpStatus.BAD_REQUEST)
    }

    data class ErrorDetails(val timestamp: ZonedDateTime, val message: String, val fields: List<FieldError>)
    data class FieldError(val field: String, val error: String)
}
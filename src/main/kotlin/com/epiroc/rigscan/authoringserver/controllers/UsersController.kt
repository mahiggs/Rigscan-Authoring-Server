package com.epiroc.rigscan.authoringserver.controllers

import com.epiroc.rigscan.authoringserver.authentication.BearerTokenHandler
import com.epiroc.rigscan.authoringserver.authentication.RigscanProperties
import com.epiroc.rigscan.authoringserver.configuration.BearerTokenHolder
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.time.ZonedDateTime

@Controller
class UsersController(val repository: UserRepository, val mapper: ObjectMapper,
                      val bearerTokenHolder: BearerTokenHolder, val bearerTokenHandler: BearerTokenHandler,
                      val template: RestTemplate, val properties: RigscanProperties) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UsersController::class.java)
    }

    @RequestMapping("/users/")
    fun listUsers(model: Model) : String {
        var users : List<UserV1>? = null

        // refresh the bearer token if necessary
        bearerTokenHandler.refreshAuthenticationIfNecessary(bearerTokenHolder)

        if (bearerTokenHolder.bearerToken != null) {
            val headers = HttpHeaders()
            headers.set("Authorization", "Bearer ${bearerTokenHolder.bearerToken}")
            val entity = HttpEntity(null, headers)

            val response: ResponseEntity<List<UserV1>>
            try {
                response = this.template.exchange(apiEndpoint("/api/v1/users"), HttpMethod.GET, entity,
                        object : ParameterizedTypeReference<List<UserV1>>() {})

                users = response.body ?: ArrayList()
            } catch (e: HttpStatusCodeException) {
                when (e.statusCode) {
                    HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED ->
                        // we clear out the bearer token if it is no longer working, we will indicate to user
                        // that they must re-authenticate to add new users
                        bearerTokenHolder.clear()
                    else -> {
                        // if we don't recognize the status code, then log it out
                        log.error("Unrecognized code from server code=[{}], body=[{}]", e.statusCode, e.responseBodyAsString)
                    }
                }
            }
        }

        val rawUsers = mapper.writeValueAsString(this.repository.findAll())
        val rawRemoteUsers = mapper.writeValueAsString(users ?: ArrayList<UserV1>())
        val usersModel = UsersModel(rawUsers, users != null, rawRemoteUsers)

        model.addAttribute("model", usersModel)

        return "users/index"
    }

    private fun apiEndpoint(relativePath: String): String {
        return this.properties.server.url + if (relativePath[0] == '/') {
            relativePath.substring(1)
        } else {
            relativePath
        }
    }

}

data class UserV1(val id: Int, val userLogin: String, val userName: String, val userRole: String,
                  val regions: List<Int>?, val dealer: Int?, val productCompanies: List<String>?,
                  val active: Boolean, val email: String, val createdAt: ZonedDateTime,
                  val modifiedAt: ZonedDateTime)

data class UsersModel(val rawUsers: String, val usersDownloadSucceeded: Boolean, val rigscanUsers: String)
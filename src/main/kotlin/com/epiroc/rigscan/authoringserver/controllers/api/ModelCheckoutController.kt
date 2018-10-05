package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.db.isAdministrator
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.epiroc.rigscan.authoringserver.db.repositories.currentUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.lang.IllegalStateException
import java.sql.Types
import java.util.*

@RestController
@RequestMapping("/api/checkout/")
class ModelCheckoutController(private val template: JdbcTemplate, private val repository: UserRepository) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ModelCheckoutController::class.java)
        const val CHECK_OUT_PROTOCOL_DML: String = "INSERT INTO audit_protocol_checkouts (model_id, checked_out_by, checkout_performed_by, checked_out_reason) VALUES (?, ?, ?, ?)"
        const val CHECK_IN_PROTOCOL_DML: String = "DELETE FROM audit_protocol_checkouts WHERE model_id=?"
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR', 'USER')")
    fun checkModelOut(@RequestBody request: CheckoutRequest) : ResponseEntity<Any> {
        val currentUser = this.repository.currentUser()
        if (request.checkoutFor != null && request.checkoutFor.toLong() != currentUser.id && !currentUser.isAdministrator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CheckoutResponse(false, "Normal users may not check out audits for other users."))
        }

        val currentCheckoutStatus = this.template.queryForList("SELECT checked_out_by FROM audit_protocol_checkouts WHERE model_id=?",
                arrayOf(request.modelId.toString()), intArrayOf(Types.CHAR), Integer::class.java)

        if (currentCheckoutStatus.size > 1) {
            throw IllegalStateException("Found multiple results for one ID. This should be impossible due to unique constraint.")
        }

        if (currentCheckoutStatus.size == 1) {
            return ResponseEntity.badRequest()
                    .body(CheckoutResponse(false, "Audit protocol is already checked out."))
        }

        val rowsUpdated = this.template.update(CHECK_OUT_PROTOCOL_DML,
                arrayOf(request.modelId.toString(), request.checkoutFor ?: currentUser.id, currentUser.id, request.checkoutReason),
                intArrayOf(Types.CHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR))

        return if (rowsUpdated <= 0) {
            ResponseEntity.badRequest()
                    .body(CheckoutResponse(false, "Audit protocol was checked out when attempting to check out."))
        } else {
            ResponseEntity.ok(CheckoutResponse(true, null))
        }
    }

    @DeleteMapping("/{modelId}")
    fun checkModelIn(@PathVariable modelId: UUID) : ResponseEntity<Any> {
        val currentUser = this.repository.currentUser()

        log.info("Received checkin request from ${currentUser.userName} [id=${currentUser.id}] for model [id=$modelId]")

        val currentCheckoutStatus = this.template.queryForList("SELECT checked_out_by FROM audit_protocol_checkouts WHERE model_id=?",
                arrayOf(modelId.toString()), intArrayOf(Types.CHAR), Integer::class.java)

        if (currentCheckoutStatus.size == 0) {
            return ResponseEntity.ok(Message("Audit is not checked out."))
        }

        if (currentCheckoutStatus.size > 1) {
            throw IllegalStateException("Found multiple results for one ID. Should be impossible")
        }

        if (!currentUser.isAdministrator() && currentUser.id != currentCheckoutStatus[0].toLong()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Message("Only administrators can check in audits checked out by others."))
        }

        val updatedRows = this.template.update(CHECK_IN_PROTOCOL_DML, arrayOf(modelId.toString()), intArrayOf(Types.CHAR))

        return if (updatedRows <= 0) {
            ResponseEntity.badRequest().body(Message("Unable to check audit in."))
        } else {
            ResponseEntity.ok(Message("Audit successfully checked in."))
        }
    }

}

data class CheckoutRequest(val checkoutFor: Int?, val modelId: UUID, val checkoutReason: String?)
data class CheckoutResponse(val success: Boolean, val message: String?)
package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.db.isAdministrator
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.epiroc.rigscan.authoringserver.db.repositories.currentUser
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
class AuditCheckoutController(private val template: JdbcTemplate, private val repository: UserRepository) {

    companion object {
        const val CHECK_OUT_PROTOCOL_DML: String = "UPDATE audit_protocols SET checked_out_by=?, checkout_performed_by=?, checked_out_reason=? WHERE id=? AND checked_out_by IS NULL"
        const val CHECK_IN_PROTOCOL_DML: String = "UPDATE audit_protocols SET checked_out_by=NULL, checkout_performed_by=NULL, checked_out_reason=NULL WHERE id=?"
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR', 'USER')")
    fun checkAuditOut(@RequestBody request: CheckoutRequest) : ResponseEntity<Any> {
        val currentUser = this.repository.currentUser()
        if (request.checkoutFor != null && request.checkoutFor.toLong() != currentUser.id && !currentUser.isAdministrator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CheckoutResponse(false, "Normal users may not check out audits for other users."))
        }

        val currentCheckoutStatus = this.template.queryForList("SELECT checked_out_by FROM audit_protocols WHERE id=?",
                arrayOf(request.auditId.toString()), intArrayOf(Types.CHAR), Integer::class.java)

        if (currentCheckoutStatus.size == 0) {
            return ResponseEntity.notFound().build()
        } else if (currentCheckoutStatus.size > 1) {
            throw IllegalStateException("Found multiple results for one ID.")
        }

        if (currentCheckoutStatus[0] != null) {
            return ResponseEntity.badRequest()
                    .body(CheckoutResponse(false, "Audit protocol is already checked out."))
        }

        val rowsUpdated = this.template.update(CHECK_OUT_PROTOCOL_DML,
                arrayOf(request.checkoutFor ?: currentUser.id, currentUser.id, request.checkoutReason, request.auditId.toString()),
                intArrayOf(Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.CHAR))

        return if (rowsUpdated <= 0) {
            ResponseEntity.badRequest()
                    .body(CheckoutResponse(false, "Audit protocol was checked out when attempting to check out."))
        } else {
            ResponseEntity.ok(CheckoutResponse(true, null))
        }
    }

    @DeleteMapping("/{auditId}")
    fun checkAuditIn(@PathVariable auditId: UUID) : ResponseEntity<Any> {
        val currentUser = this.repository.currentUser()

        val currentCheckoutStatus = this.template.queryForList("SELECT checked_out_by FROM audit_protocols WHERE id=?",
                arrayOf(auditId.toString()), intArrayOf(Types.CHAR), Integer::class.java)

        if (currentCheckoutStatus.size == 0) {
            return ResponseEntity.notFound().build()
        } else if (currentCheckoutStatus.size > 1) {
            throw IllegalStateException("Found multiple results for one ID.")
        }

        if (currentCheckoutStatus[0] == null) {
            return ResponseEntity.ok(Message("Audit not checked out."))
        } else if (!currentUser.isAdministrator() && currentUser.id != currentCheckoutStatus[0].toLong()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Message("Only administrators can check in audits checked out by others."))
        }

        val updatedRows = this.template.update(CHECK_IN_PROTOCOL_DML, arrayOf(auditId.toString()), intArrayOf(Types.CHAR))

        return if (updatedRows <= 0) {
            ResponseEntity.badRequest().body(Message("Unable to check audit in."))
        } else {
            ResponseEntity.ok(Message("Audit successfully checked in."))
        }
    }

}

data class CheckoutRequest(val checkoutFor: Int?, val auditId: UUID, val checkoutReason: String?)
data class CheckoutResponse(val success: Boolean, val message: String?)
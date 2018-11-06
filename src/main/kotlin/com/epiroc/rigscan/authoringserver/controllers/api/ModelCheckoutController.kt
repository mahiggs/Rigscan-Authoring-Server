package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.db.isAdministrator
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.epiroc.rigscan.authoringserver.db.repositories.currentUser
import com.github.zafarkhaja.semver.Version
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcCall
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.sql.Types
import java.util.*
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/checkout/")
class ModelCheckoutController(private val template: JdbcTemplate, private val repository: UserRepository) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ModelCheckoutController::class.java)
        const val CHECK_OUT_PROTOCOL_DML: String = "INSERT INTO audit_protocol_checkouts (model_id, checked_out_by, checkout_performed_by, checked_out_reason, checked_out_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)"
        const val CHECK_IN_PROTOCOL_DML: String = "DELETE FROM audit_protocol_checkouts WHERE model_id=?"
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR', 'USER')")
    fun checkModelOut(@RequestBody request: CheckoutRequest) : ResponseEntity<Any> {
        val currentUser = this.repository.currentUser()

        log.info("Received Checkout Request: ${request.modelId}, ${request.checkoutFor}, ${request.checkoutReason}, currentUser: ${currentUser.id}")

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
            throw BadRequestException("Audit protocol is already checked out.", false)
        }

        val rowsUpdated = this.template.update(CHECK_OUT_PROTOCOL_DML,
                arrayOf(request.modelId.toString(), request.checkoutFor ?: currentUser.id, currentUser.id, request.checkoutReason),
                intArrayOf(Types.CHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR))

        if (rowsUpdated <= 0) {
            throw BadRequestException("Audit protocol was checked out when attempting to check out.", false)
        }

        // get the list of audits for this particular model and sort them by their version
        val auditInformations = this.template.query("SELECT id, version FROM audit_protocols WHERE model_id=?",
                arrayOf(request.modelId.toString()), intArrayOf(Types.CHAR)) { rs, _ ->
            AuditInformation(UUID.fromString(rs.getString("id")),
                    Version.valueOf(rs.getString("version")))
        }.sortedBy { it.version }

        // find the latest one
        val latestInformation = auditInformations[auditInformations.size - 1]

        val models = this.template.query("SELECT id, version FROM versioned_model_information WHERE model_id=?",
                arrayOf(request.modelId.toString()),
                intArrayOf(Types.CHAR)) { rs, _ ->
            Pair(UUID.fromString(rs.getString("id")), Version.valueOf(rs.getString("version")))
        }

        val matchingModel = models.find {
            latestInformation.version != null && latestInformation.version.majorVersion == it.second.majorVersion
                    && latestInformation.version.minorVersion == it.second.minorVersion
        }

        if (matchingModel == null) {
            throw BadRequestException("Unable to find model version.", false)
        }

        // calculate what the version of the cloned audit should be
        val newVersion = latestInformation.version!!.incrementMinorVersion()

        // call the cloneAudit sproc with the latest audit id and the new version
        SimpleJdbcCall(template).withProcedureName("cloneAudit")
                .execute(mapOf("model_id" to matchingModel.first.toString(),
                        "audit_id" to latestInformation.audit_id.toString(), "version" to newVersion))

        return ResponseEntity.ok(CheckoutResponse(true, null))
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

data class AuditInformation(val audit_id: UUID, val version: Version?)
data class CheckoutRequest(val checkoutFor: Int?, val modelId: UUID, val checkoutReason: String?)
data class CheckoutResponse(val success: Boolean, val message: String?)
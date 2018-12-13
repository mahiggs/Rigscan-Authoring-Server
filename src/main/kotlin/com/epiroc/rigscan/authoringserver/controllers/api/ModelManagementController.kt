package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.db.isAdministrator
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.epiroc.rigscan.authoringserver.db.repositories.currentUser
import com.github.zafarkhaja.semver.Version
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcCall
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.Types
import java.util.*
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/models")
class ModelManagementController(private val template: JdbcTemplate, private val repository: UserRepository) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ModelManagementController::class.java)
        private const val MODEL_VERSION_UPDATE_DML = "UPDATE versioned_model_information SET version=? WHERE id=?"
        private const val AUDIT_VERSION_UPDATE_DML = "UPDATE audit_protocols SET version=? WHERE id=?"
        private const val CHECK_IN_PROTOCOL_DML: String = "DELETE FROM audit_protocol_checkouts WHERE model_id=?"
    }

    @PostMapping("{modelId}/prepareUpload")
    @PreAuthorize("hasAnyAuthority('UPLOADER')")
    @Transactional
    fun prepareForUpload(@PathVariable modelId: UUID): OperationResponse {
        // ensure that the model is checked out so that any changes we make don't mess with other users' data
        ensureModelCheckedOut(modelId, "Performing temporary checkout in order to facilitate upload.")

        // get the mapping of models to audits
        val modelToAuditMapping = this.getVersionedModelToAuditsMapping(modelId)
        // figure out which version will become the version to upload
        val modelVersionForUpload = modelToAuditMapping.keys.maxBy { it.version }

        if (modelVersionForUpload == null) {
            throw BadRequestException("Unable to find version for upload.", false)
        }

        // get all of the versions that weren't selected to become the next version
        val modelAuditPairsToDelete = modelToAuditMapping.filterKeys { it != modelVersionForUpload }

        // delete all of the old versions of the audits
        for (modelAuditPair in modelAuditPairsToDelete) {
            log.info("Deleting Audit [versionedModelId={},version={},auditProtocolId={},version={}]", modelAuditPair.key.id,
                    modelAuditPair.key.version, modelAuditPair.value.id, modelAuditPair.value.version)

            SimpleJdbcCall(template).withProcedureName("deleteAudit")
                    .execute(mapOf("versioned_model_id" to modelAuditPair.key.id.toString(),
                            "audit_id" to modelAuditPair.value.id.toString()))
        }

        val nextVersion = modelVersionForUpload.version.incrementMajorVersion()

        log.info("Updating version of latest versions to {}", nextVersion)

        // update the version of the versioned_model_information and audit_protocol to be the next major version
        this.template.update(MODEL_VERSION_UPDATE_DML,
                arrayOf(nextVersion.toString(), modelVersionForUpload.id.toString()), intArrayOf(Types.CHAR, Types.CHAR))
        this.template.update(AUDIT_VERSION_UPDATE_DML,
                arrayOf(nextVersion.toString(), modelToAuditMapping[modelVersionForUpload]?.id.toString()), intArrayOf(Types.CHAR, Types.CHAR))

        // user will perform checkin manually after the actual upload process succeeds
        return OperationResponse(true)
    }

    @PostMapping("revert/{modelId}/to/{versionedModelId}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    fun revertTo(@PathVariable modelId: UUID, @PathVariable versionedModelId: UUID): OperationResponse {
        // we must check the model out before we revert, so that we aren't smashing some other user's changes
        val checkoutPerformed = this.ensureModelCheckedOut(modelId, "Performing temporary checkout in order to facilitate revert operation.")

        log.info("Reverting model {} to version {}", modelId, versionedModelId)
        val modelToAuditsMapping = this.getVersionedModelToAuditsMapping(modelId)

        val modelToRevertTo = modelToAuditsMapping.asSequence().first { it.key.id == versionedModelId }
        val modelAuditPairsToDelete = modelToAuditsMapping.filterKeys { it.version.greaterThan(modelToRevertTo.key.version) }

        for (modelAuditPair in modelAuditPairsToDelete) {
            log.info("Deleting Audit [versionedModelId={},version={},auditProtocolId={},version={}]", modelAuditPair.key.id,
                    modelAuditPair.key.version, modelAuditPair.value.id, modelAuditPair.value.version)
            SimpleJdbcCall(template).withProcedureName("deleteAudit")
                    .execute(mapOf("versioned_model_id" to modelAuditPair.key.id.toString(),
                            "audit_id" to modelAuditPair.value.id.toString()))
        }

        // If the model was automatically checked out in order to perform the reversion, then automatically
        // check it back in
        if (checkoutPerformed) {
            this.checkIn(modelId)
        }

        return OperationResponse(true)
    }

    private fun checkIn(modelId: UUID) {
        log.info("Checking model in: $modelId")

        val updatedRows = this.template.update(CHECK_IN_PROTOCOL_DML, arrayOf(modelId.toString()), intArrayOf(Types.CHAR))

        if (updatedRows <= 0) {
            throw BadRequestException("Error occurred when attempting to check model in.", false)
        }
    }

    private fun ensureModelCheckedOut(modelId: UUID, reason: String): Boolean {
        val currentUser = this.repository.currentUser()

        log.info("Ensuring model {} is checked out by user {} [id={}]...", modelId, currentUser.userName, currentUser.id)

        // see if the audit is currently checked out by the current user
        val currentCheckoutStatus = this.template.queryForList("SELECT checked_out_by FROM audit_protocol_checkouts WHERE model_id=?",
                arrayOf(modelId.toString()), intArrayOf(Types.CHAR), Long::class.java)

        // if it is checked out, we ensure it is the current user checking it out
        if (currentCheckoutStatus.size >= 1 && currentCheckoutStatus[0] != currentUser.id) {
            log.error("Model is already checked out by another user (id={})", currentCheckoutStatus[0])
            // and if it isn't we throw an exception
            throw BadRequestException("Audit protocol was checked out by a user that is not the uploader.", false)
        } else if (currentCheckoutStatus.size == 0) {
            // if it isn't checked out, we perform the checkout
            val rowsUpdated = this.template.update(ModelCheckoutController.CHECK_OUT_PROTOCOL_DML,
                    arrayOf(modelId.toString(), currentUser.id, currentUser.id, reason),
                    intArrayOf(Types.CHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR))

            // and throw an exception if we fail to check it out
            if (rowsUpdated <= 0) {
                log.error("Failed to check out model.")
                throw BadRequestException("Audit protocol was checked out when attempting to check out.", false)
            }

            return true
        }

        log.info("Model is checked out by user.")
        return false
    }

    private fun getVersionedModelToAuditsMapping(modelId: UUID): Map<IdAndVersion, IdAndVersion> {
        val versionedModels = this.getVersionedModelsForModel(modelId)
        val versionedAudits = this.getVersionedAuditsForModel(modelId)

        return versionedModels.map {
            val matchingAudit = versionedAudits.first { audit -> audit.version.majorVersion == it.version.majorVersion
                    && audit.version.minorVersion == it.version.minorVersion }

            it to matchingAudit
        }.toMap()
    }

    private fun getVersionedModelsForModel(modelId: UUID): List<IdAndVersion> {
        return this.template.query("SELECT id, version FROM versioned_model_information WHERE model_id=?",
                arrayOf(modelId.toString()),
                intArrayOf(Types.CHAR)) { rs, _ ->
            IdAndVersion(UUID.fromString(rs.getString("id")), Version.valueOf(rs.getString("version")))
        }
    }

    private fun getVersionedAuditsForModel(modelId: UUID): List<IdAndVersion> {
        return this.template.query("SELECT id, version FROM audit_protocols WHERE model_id=?",
                arrayOf(modelId.toString()),
                intArrayOf(Types.CHAR)) { rs, _ ->
            IdAndVersion(UUID.fromString(rs.getString("id")), Version.valueOf(rs.getString("version")))
        }
    }

    data class IdAndVersion(val id: UUID, val version: Version)
    data class OperationResponse(val success: Boolean)

}

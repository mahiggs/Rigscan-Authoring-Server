package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.authentication.UserBasedUserDetails
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.epiroc.rigscan.authoringserver.db.repositories.currentUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.sql.Types

/**
 * Contains the method for the outgoing batches API.
 */
@RestController
class OutgoingBatchesController(val template: JdbcTemplate, val repository: UserRepository) {

    companion object {
        /**
         * The query used to fetch the batch ids.
         */
        const val OUTGOING_RELOAD_BATCH_QUERY: String = "SELECT batch_id FROM sym_outgoing_batch WHERE channel_id='reload' AND node_id=?"
        const val OUTGOING_FILESYNC_BATCH_QUERY: String = "SELECT batch_id FROM sym_outgoing_batch WHERE (channel_id='filesync' or channel_id='filesync_reload') AND node_id=?"
        const val OUTGOING_BATCH_QUERY: String = "SELECT batch_id FROM sym_outgoing_batch WHERE node_id=?"

        private val log: Logger = LoggerFactory.getLogger(OutgoingBatchesController::class.java)
    }

    /**
     * Fetch a list of ids of the outgoing batches for the specified [nodeId] that are in the
     * reload channel.
     * @return the batch ids
     */
    @GetMapping("/api/outgoingBatches/reload/{nodeId}")
    fun outgoingReloadBatchesFor(@PathVariable nodeId: String): ResponseEntity<Any> {
        if (!isClientRegisteredWithAuthenticatedUser(nodeId)) {
            return ResponseEntity.status(403)
                    .body(Message("Node not registered to current user."))
        }

        // use the JDBC template to fetch the batch ids for the outgoing batches
        val batchIds = this.template.queryForList(OUTGOING_RELOAD_BATCH_QUERY, arrayOf(nodeId),
                intArrayOf(Types.VARCHAR), Integer::class.java)

        // convert the Integer!s to Ints to appease the gods of Kotlin
        return ResponseEntity.ok(batchIds.map { it.toInt() })
    }

    /**
     * Fetch a list of ids of the outgoing batches for the specified [nodeId] that are in the
     * reload channel.
     * @return the batch ids
     */
    @GetMapping("/api/outgoingBatches/filesync/{nodeId}")
    fun outgoingFilesyncBatchesFor(@PathVariable nodeId: String): ResponseEntity<Any> {
        if (!isClientRegisteredWithAuthenticatedUser(nodeId)) {
            return ResponseEntity.status(403)
                    .body(Message("Node not registered to current user."))
        }

        // use the JDBC template to fetch the batch ids for the outgoing batches
        val batchIds = this.template.queryForList(OUTGOING_FILESYNC_BATCH_QUERY, arrayOf(nodeId),
                intArrayOf(Types.VARCHAR), Integer::class.java)

        // convert the Integer!s to Ints to appease the gods of Kotlin
        return ResponseEntity.ok(batchIds.map { it.toInt() })
    }

    /**
     * Fetch a list of ids of the outgoing batches for the specified [nodeId] that are in the
     * reload channel.
     * @return the batch ids
     */
    @GetMapping("/api/outgoingBatches/all/{nodeId}")
    fun outgoingBatchesFor(@PathVariable nodeId: String): ResponseEntity<Any> {
        if (!isClientRegisteredWithAuthenticatedUser(nodeId)) {
            return ResponseEntity.status(403)
                    .body(Message("Node not registered to current user."))
        }

        // use the JDBC template to fetch the batch ids for the outgoing batches
        val batchIds = this.template.queryForList(OUTGOING_BATCH_QUERY, arrayOf(nodeId),
                intArrayOf(Types.VARCHAR), Integer::class.java)

        // convert the Integer!s to Ints to appease the gods of Kotlin
        return ResponseEntity.ok(batchIds.map { it.toInt() })
    }

    fun isClientRegisteredWithAuthenticatedUser(nodeId: String): Boolean {
        val currentUser = this.repository.currentUser()

        if (!currentUser.clients.contains(nodeId)) {
            log.warn("User [id={}] tried to request information for node not registered to them: {}", currentUser.id, nodeId)
            return false
        }

        return true
    }

    data class Message(val message: String)
}
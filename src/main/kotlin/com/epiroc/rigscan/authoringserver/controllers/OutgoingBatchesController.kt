package com.epiroc.rigscan.authoringserver.controllers

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.Types

/**
 * Contains the method for the outgoing batches API.
 */
@RestController
class OutgoingBatchesController(val template: JdbcTemplate) {

    companion object {
        /**
         * The query used to fetch the batch ids.
         */
        const val OUTGOING_BATCH_QUERY: String = "SELECT batch_id FROM sym_outgoing_batch WHERE channel_id='reload' AND node_id=?"
    }

    /**
     * Fetch a list of ids of the outgoing batches for the specified [nodeId] that are in the
     * reload channel.
     * @return the batch ids
     */
    @RequestMapping("/api/outgoingBatches/{nodeId}")
    fun outgoingReloadBatchesFor(@PathVariable nodeId: String): List<Int> {
        // use the JDBC template to fetch the batch ids for the outgoing batches
        val batchIds = this.template.queryForList(OUTGOING_BATCH_QUERY, arrayOf(nodeId),
                intArrayOf(Types.VARCHAR), Integer::class.java)

        // convert the Integer!s to Ints to appease the gods of Kotlin
        return batchIds.map { it.toInt() }
    }
}
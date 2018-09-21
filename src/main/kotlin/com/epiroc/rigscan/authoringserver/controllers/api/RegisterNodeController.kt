package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.authentication.UserBasedUserDetails
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.sql.Types

@RestController
class RegisterNodeController(val template: JdbcTemplate, val repository: UserRepository) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RegisterNodeController::class.java)
        const val SYM_NODE_DML : String = "insert into sym_node (node_id,node_group_id,external_id,sync_enabled,sync_url,schema_version,symmetric_version,database_type,database_version,heartbeat_time,timezone_offset,batch_to_send_count,batch_in_error_count,created_at_node_id) values (?,'client',?,1,null,null,null,null,null,current_timestamp,null,0,0,'000')"
        const val SYM_NODE_SECURITY_DML : String = "insert into sym_node_security (node_id,node_password,registration_enabled,registration_time,initial_load_enabled,initial_load_time,created_at_node_id) values (?,'5d1c92bbacbe2edb9e1ca5dbb0e481',1,null,1,null,'000')"
    }

    @PostMapping("/api/registerNode")
    fun registerNode(@RequestBody nodeInfo: NodeInfo) : ResponseEntity<Any> {
        val principal = SecurityContextHolder.getContext().authentication.principal as UserBasedUserDetails

        val currentUser = this.repository.findById(principal.user.id!!).orElseThrow { ResourceNotFoundException() }

        if (currentUser.clients.contains(nodeInfo.nodeId)) {
            log.warn("User [id={}] tried to register already registered node: {}", currentUser.id, nodeInfo.nodeId)
            return ResponseEntity.badRequest()
                    .body(Message("That client ID has already been registered."))
        }

        // add the new node
        currentUser.clients.add(nodeInfo.nodeId)
        // add the information
        this.repository.save(currentUser)

        this.template.update(SYM_NODE_DML, arrayOf(nodeInfo.nodeId, nodeInfo.nodeId), intArrayOf(Types.VARCHAR, Types.VARCHAR))
        this.template.update(SYM_NODE_SECURITY_DML, arrayOf(nodeInfo.nodeId), intArrayOf(Types.VARCHAR))

        return ResponseEntity.ok(Message("Node successfully registered."))
    }

    data class NodeInfo(val nodeId: String)
    data class Message(val message: String)

}
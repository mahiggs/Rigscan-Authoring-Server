package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.authentication.RigscanProperties
import com.epiroc.rigscan.authoringserver.db.isAdministrator
import com.epiroc.rigscan.authoringserver.db.repositories.UserRepository
import com.epiroc.rigscan.authoringserver.db.repositories.currentUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Paths
import java.sql.Types
import java.util.*

@RestController
@RequestMapping("/api/files/")
class FilesController(val properties: RigscanProperties, val template: JdbcTemplate, val repository: UserRepository) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FilesController::class.java)
        const val MODEL_INFORMATION = "SELECT model_id, product_company, template FROM models INNER JOIN equipment_types ON models.equipment_type = equipment_types.id WHERE models.id=?"
    }

    @PostMapping("/safety/{productCompany}")
    fun uploadSafetyImage(@PathVariable productCompany: String, @RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        val cleanFilename = file.originalFilename.cleanFilename()
        log.info("Uploading safety file, file name: {} [cleaned={}]", file.originalFilename, cleanFilename)

        if (!Paths.get(properties.files.directory, productCompany).toFile().exists()) {
            return ResponseEntity.badRequest().body(Message("Unrecognized product company: $productCompany"))
        }

        // safety files are stored in a directory with the format ${baseDir}/${productCompany}/assets/safety/${fileName}
        val path = Paths.get(properties.files.directory, productCompany, "assets", "safety", cleanFilename)

        // move the file to the path
        file.transferTo(path.toFile())

        return ResponseEntity.ok(Message("File successfully added."))
    }

    @PostMapping("/{modelId}")
    fun createFile(@PathVariable modelId: UUID, @RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        val currentUser = this.repository.currentUser()

        // we will always allow administrators to upload files
        if (!currentUser.isAdministrator()) {
            val currentCheckoutStatus = this.template.queryForList("SELECT checked_out_by FROM audit_protocol_checkouts WHERE model_id=?",
                    arrayOf(modelId.toString()), intArrayOf(Types.CHAR), Integer::class.java)

            if (currentCheckoutStatus.size != 1 || currentCheckoutStatus[0].toLong() != currentUser.id) {
                return ResponseEntity.badRequest().body(Message("You must check out the audit in order to add files to it."))
            }
        }

        val modelInfo = this.template.query(MODEL_INFORMATION, arrayOf(modelId.toString()), intArrayOf(Types.CHAR)) { rs, _ ->
            ModelInformation(rs.getNString("model_id"),
                    rs.getNString("product_company"), rs.getBoolean("template"))
        }

        if (modelInfo.size != 1) {
            log.error("Invalid information returned from model info: {}", modelInfo)
            return ResponseEntity.badRequest().body(Message("Invalid information found for model."))
        }

        val cleanFilename = file.originalFilename.cleanFilename()
        log.info("Uploading file, file name: {} [cleaned={}]", file.originalFilename, cleanFilename)

        // files are stored in a directory with the format ${baseDir}/${productCompany}/${modelId}/${fileName}
        val path = Paths.get(properties.files.directory, modelInfo[0].productCompany, modelInfo[0].modelName, cleanFilename)

        // move the file to the path
        file.transferTo(path.toFile())

        return ResponseEntity.ok(Message("File successfully added."))
    }

}

/**
 * Remove well-known invalid characters from file. This doesn't absolutely ensure that it is a valid file name, however
 * it provides a good starting place.
 *
 * Removed characters are:
 * ```
 * Control characters (1-31)
 * <
 * >
 * :
 * "
 * \
 * |
 * ?
 * *
 * Whitespace characters
 * ```
 */
fun String?.cleanFilename(): String? {
    return this?.replace(Regex("\\s+"), "_")
            ?.replace(Regex("[\\x00-\\x1F/<>:\"\\\\\\|?*]*"), "")
}

data class ModelInformation(val modelName: String, val productCompany: String, val template: Boolean)
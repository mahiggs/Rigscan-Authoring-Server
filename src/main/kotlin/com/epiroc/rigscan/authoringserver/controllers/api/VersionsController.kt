package com.epiroc.rigscan.authoringserver.controllers.api

import com.epiroc.rigscan.authoringserver.authentication.RigscanProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class VersionsController(val rigscanProperties: RigscanProperties) {

    @GetMapping("/api/versions")
    fun getVersions() : VersionInformation {
        return VersionInformation(rigscanProperties.version.authoringTool)
    }
}

data class VersionInformation(val requiredAuthoringToolVersion: String)
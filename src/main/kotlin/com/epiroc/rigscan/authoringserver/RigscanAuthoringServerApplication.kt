package com.epiroc.rigscan.authoringserver

import com.epiroc.rigscan.authoringserver.authentication.B2CProperties
import com.epiroc.rigscan.authoringserver.authentication.RigscanProperties
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(RigscanProperties::class, B2CProperties::class)
@EnableAutoConfiguration(exclude = [FlywayAutoConfiguration::class])
class RigscanAuthoringServerApplication

fun main(args: Array<String>) {
    runApplication<RigscanAuthoringServerApplication>(*args)
}

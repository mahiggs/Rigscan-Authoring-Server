package com.epiroc.rigscan.authoringserver.authentication

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rigscan")
class RigscanProperties {

    val version = Version()
    val server = Server()

    class Version {
        /**The required version of the authoring tool.
         */
        lateinit var authoringTool: String
    }

    class Server {
        /**The URL to the RigScan server that will be used to authenticate the user.
         */
        lateinit var url : String
    }
}

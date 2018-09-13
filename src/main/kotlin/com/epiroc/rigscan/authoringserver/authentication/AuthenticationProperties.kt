package com.epiroc.rigscan.authoringserver.authentication

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rigscan")
class AuthenticationProperties {

    val server = Server()

    class Server {
        /**The URL to the RigScan server that will be used to authenticate the user.
         */
        lateinit var url : String
    }
}

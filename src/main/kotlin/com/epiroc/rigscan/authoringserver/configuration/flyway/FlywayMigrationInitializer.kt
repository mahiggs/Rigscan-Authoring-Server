/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epiroc.rigscan.authoringserver.configuration.flyway

import org.flywaydb.core.Flyway

import org.springframework.beans.factory.InitializingBean
import org.springframework.core.Ordered
import org.springframework.util.Assert

/**
 * [InitializingBean] used to trigger [Flyway] migration via the
 * [FlywayMigrationStrategy].
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
class FlywayMigrationInitializer
/**
 * Create a new [FlywayMigrationInitializer] instance.
 * @param flyway the flyway instance
 * @param migrationStrategy the migration strategy or `null`
 */
@JvmOverloads constructor(private val flyway: Flyway,
                          private val migrationStrategy: FlywayMigrationStrategy? = null) : InitializingBean, Ordered {

    private var order = 0

    init {
        Assert.notNull(flyway, "Flyway must not be null")
    }

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (this.migrationStrategy != null) {
            this.migrationStrategy.migrate(this.flyway)
        } else {
            this.flyway.migrate()
        }
    }

    override fun getOrder(): Int {
        return this.order
    }

    fun setOrder(order: Int) {
        this.order = order
    }

}
/**
 * Create a new [FlywayMigrationInitializer] instance.
 * @param flyway the flyway instance
 */
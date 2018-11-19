/*
 * Copyright 2012-2018 the original author or authors.
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
import org.springframework.boot.jdbc.SchemaManagement
import org.springframework.boot.jdbc.SchemaManagementProvider
import java.util.stream.StreamSupport
import javax.sql.DataSource

/**
 * A Flyway [SchemaManagementProvider] that determines if the schema is managed by
 * looking at available [Flyway] instances.
 *
 * @author Stephane Nicoll
 */
internal class FlywaySchemaManagementProvider(private val flywayInstances: Iterable<Flyway>) : SchemaManagementProvider {

    override fun getSchemaManagement(dataSource: DataSource): SchemaManagement {
        return StreamSupport.stream(this.flywayInstances.spliterator(), false)
                .map { it.dataSource }
                .filter { dataSource == it }
                .findFirst()
                .map { SchemaManagement.MANAGED }
                .orElse(SchemaManagement.UNMANAGED)
    }

}
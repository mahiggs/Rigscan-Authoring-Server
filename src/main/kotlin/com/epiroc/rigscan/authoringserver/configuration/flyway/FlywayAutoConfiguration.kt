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

import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.function.Supplier
import java.util.stream.Collectors

import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.callback.FlywayCallback

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.jdbc.JdbcOperationsDependsOnPostProcessor
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DatabaseDriver
import org.springframework.boot.jdbc.SchemaManagementProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.core.io.ResourceLoader
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.support.JdbcUtils
import org.springframework.jdbc.support.MetaDataAccessException
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.util.Assert
import org.springframework.util.ObjectUtils
import org.springframework.util.StringUtils

/**
 * [Auto-configuration][EnableAutoConfiguration] for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Jacques-Etienne Beaudet
 * @author Eddú Meléndez
 * @author Dominic Gunn
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(Flyway::class)
@ConditionalOnBean(DataSource::class)
@ConditionalOnProperty(prefix = "spring.flyway", name = ["enabled"], matchIfMissing = true)
@AutoConfigureAfter(DataSourceAutoConfiguration::class, JdbcTemplateAutoConfiguration::class, HibernateJpaAutoConfiguration::class)
class FlywayAutoConfiguration {

    @Bean
    @ConfigurationPropertiesBinding
    fun stringOrNumberMigrationVersionConverter(): GenericConverter {
        return StringOrNumberToMigrationVersionConverter()
    }

    @Bean
    fun flywayDefaultDdlModeProvider(
            flyways: ObjectProvider<Flyway>): SchemaManagementProvider {
        return FlywaySchemaManagementProvider(flyways)
    }

    @Configuration
    @ConditionalOnMissingBean(Flyway::class)
    @EnableConfigurationProperties(DataSourceProperties::class, FlywayProperties::class)
    class FlywayConfiguration(private val properties: FlywayProperties,
                              private val dataSourceProperties: DataSourceProperties, private val resourceLoader: ResourceLoader,
                              dataSource: ObjectProvider<DataSource>,
                              @FlywayDataSource flywayDataSource: ObjectProvider<DataSource>,
                              migrationStrategy: ObjectProvider<FlywayMigrationStrategy>,
                              flywayCallbacks: ObjectProvider<FlywayCallback>) {

        private val dataSource: DataSource? = dataSource.ifUnique

        private val flywayDataSource: DataSource? = flywayDataSource.ifAvailable

        private val migrationStrategy: FlywayMigrationStrategy? = migrationStrategy.ifAvailable

        private val flywayCallbacks: List<FlywayCallback> = flywayCallbacks.orderedStream()
                .collect(Collectors.toList<FlywayCallback>())

        @Bean
        @ConfigurationProperties(prefix = "spring.flyway")
        fun flyway(): Flyway {
            val flyway = SpringBootFlyway()
            if (this.properties.isCreateDataSource) {
                val url = getProperty(Supplier { this.properties.url },
                        Supplier { this.dataSourceProperties.url })
                val user = getProperty(Supplier { this.properties.user },
                        Supplier { this.dataSourceProperties.username })
                val password = getProperty(Supplier { this.properties.password },
                        Supplier { this.dataSourceProperties.password })
                flyway.setDataSource(url, user, password,
                        *StringUtils.toStringArray(this.properties.initSqls))
            } else if (this.flywayDataSource != null) {
                flyway.dataSource = this.flywayDataSource
            } else {
                flyway.dataSource = this.dataSource
            }
            if (!this.flywayCallbacks.isEmpty()) {
                flyway.setCallbacks(*this.flywayCallbacks.toTypedArray())
            }
            checkLocationExists(flyway)
            return flyway
        }

        private fun getProperty(property: Supplier<String?>,
                                defaultValue: Supplier<String>): String {
            return property.get() ?: defaultValue.get()
        }

        private fun checkLocationExists(flyway: Flyway) {
            if (this.properties.isCheckLocation) {
                val locations = LocationResolver(flyway.dataSource)
                        .resolveLocations(this.properties.locations)
                Assert.state(locations.isNotEmpty(),
                        "Migration script locations not configured")
                val exists = hasAtLeastOneLocation(*locations)
                Assert.state(exists) {
                    ("Cannot find migrations location in: "
                            + Arrays.asList(*locations)
                            + " (please add migrations or check your Flyway configuration)")
                }
            }
        }

        private fun hasAtLeastOneLocation(vararg locations: String): Boolean {
            for (location in locations) {
                if (this.resourceLoader.getResource(normalizePrefix(location)).exists()) {
                    return true
                }
            }
            return false
        }

        private fun normalizePrefix(location: String): String {
            return location.replace("filesystem:", "file:")
        }

        @Bean
        @ConditionalOnMissingBean
        fun flywayInitializer(flyway: Flyway): FlywayMigrationInitializer {
            return FlywayMigrationInitializer(flyway, this.migrationStrategy)
        }

        /**
         * Additional configuration to ensure that [EntityManagerFactory] beans
         * depend on the `flywayInitializer` bean.
         */
        @Configuration
        @ConditionalOnClass(LocalContainerEntityManagerFactoryBean::class)
        @ConditionalOnBean(AbstractEntityManagerFactoryBean::class)
        protected class FlywayInitializerJpaDependencyConfiguration : EntityManagerFactoryDependsOnPostProcessor("flywayInitializer")

        /**
         * Additional configuration to ensure that [JdbcOperations] beans depend on
         * the `flywayInitializer` bean.
         */
        @Configuration
        @ConditionalOnClass(JdbcOperations::class)
        @ConditionalOnBean(JdbcOperations::class)
        protected class FlywayInitializerJdbcOperationsDependencyConfiguration : JdbcOperationsDependsOnPostProcessor("flywayInitializer")

    }

    /**
     * Additional configuration to ensure that [EntityManagerFactory] beans depend
     * on the `flyway` bean.
     */
    @Configuration
    @ConditionalOnClass(LocalContainerEntityManagerFactoryBean::class)
    @ConditionalOnBean(AbstractEntityManagerFactoryBean::class)
    protected class FlywayJpaDependencyConfiguration : EntityManagerFactoryDependsOnPostProcessor("flyway")

    /**
     * Additional configuration to ensure that [JdbcOperations] beans depend on the
     * `flyway` bean.
     */
    @Configuration
    @ConditionalOnClass(JdbcOperations::class)
    @ConditionalOnBean(JdbcOperations::class)
    protected class FlywayJdbcOperationsDependencyConfiguration : JdbcOperationsDependsOnPostProcessor("flyway")

    private class SpringBootFlyway : Flyway() {

        override fun setLocations(vararg locations: String) {
            super.setLocations(
                    *LocationResolver(dataSource).resolveLocations(arrayOf(*locations)))
        }

    }

    private class LocationResolver internal constructor(private val dataSource: DataSource) {

        private val databaseDriver: DatabaseDriver
            get() {
                try {
                    val url = JdbcUtils.extractDatabaseMetaData<String>(this.dataSource, "getURL")
                    return DatabaseDriver.fromJdbcUrl(url)
                } catch (ex: MetaDataAccessException) {
                    throw IllegalStateException(ex)
                }

            }

        fun resolveLocations(locations: Collection<String>): Array<String> {
            return resolveLocations(StringUtils.toStringArray(locations))
        }

        fun resolveLocations(locations: Array<String>): Array<String> {
            if (usesVendorLocation(*locations)) {
                val databaseDriver = databaseDriver
                return replaceVendorLocations(locations, databaseDriver)
            }
            return locations
        }

        private fun replaceVendorLocations(locations: Array<String>,
                                           databaseDriver: DatabaseDriver): Array<String> {
            if (databaseDriver === DatabaseDriver.UNKNOWN) {
                return locations
            }
            val vendor = databaseDriver.id
            return Arrays.stream(locations)
                    .map { location -> location.replace(VENDOR_PLACEHOLDER, vendor) }
                    .toArray { Array(it) { "" } }
        }

        private fun usesVendorLocation(vararg locations: String): Boolean {
            for (location in locations) {
                if (location.contains(VENDOR_PLACEHOLDER)) {
                    return true
                }
            }
            return false
        }

        companion object {

            private const val VENDOR_PLACEHOLDER = "{vendor}"
        }

    }

    /**
     * Convert a String or Number to a [MigrationVersion].
     */
    private class StringOrNumberToMigrationVersionConverter : GenericConverter {

        override fun getConvertibleTypes(): Set<GenericConverter.ConvertiblePair>? {
            return CONVERTIBLE_TYPES
        }

        override fun convert(source: Any?, sourceType: TypeDescriptor,
                             targetType: TypeDescriptor): Any? {
            val value = ObjectUtils.nullSafeToString(source)
            return MigrationVersion.fromVersion(value)
        }

        companion object {

            private val CONVERTIBLE_TYPES: Set<GenericConverter.ConvertiblePair>

            init {
                val types = HashSet<GenericConverter.ConvertiblePair>(2)
                types.add(GenericConverter.ConvertiblePair(String::class.java, MigrationVersion::class.java))
                types.add(GenericConverter.ConvertiblePair(Number::class.java, MigrationVersion::class.java))
                CONVERTIBLE_TYPES = Collections.unmodifiableSet<GenericConverter.ConvertiblePair>(types)
            }
        }

    }

}
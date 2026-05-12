package com.creatorcrm.deals

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.UUID

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DealIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("creator_crm")
        }

        @Container
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url",      postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var rest: TestRestTemplate

    private fun base() = "http://localhost:$port/api/v1/deals"

    @Test
    fun `create deal returns 200 with id`() {
        val cmd = CreateDealCommand(
            creatorId = UUID.randomUUID(),
            brandName = "Nike",
            title     = "Summer campaign",
            value     = BigDecimal("5000.00")
        )
        val resp = rest.postForEntity(base(), cmd, Deal::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertNotNull(resp.body?.id)
        assertEquals(DealStatus.DRAFT, resp.body?.status)
    }

    @Test
    fun `transition deal status`() {
        val cmd = CreateDealCommand(
            creatorId = UUID.randomUUID(),
            brandName = "Adidas",
            title     = "Winter promo",
            value     = BigDecimal("3000.00")
        )
        val created = rest.postForEntity(base(), cmd, Deal::class.java).body!!
        val transResp = rest.patchForObject(
            "${base()}/${created.id}/status",
            TransitionRequest(DealStatus.ACTIVE),
            Deal::class.java
        )
        assertEquals(DealStatus.ACTIVE, transResp?.status)
    }
}

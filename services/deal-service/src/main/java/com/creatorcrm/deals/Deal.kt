package com.creatorcrm.deals

import com.fasterxml.jackson.annotation.JsonCreator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// ─── Domain ──────────────────────────────────────────────────────────────────

enum class DealStatus { DRAFT, NEGOTIATING, ACTIVE, DELIVERED, INVOICED, COMPLETE, CANCELLED }

@Entity
@Table(name = "deals")
data class Deal(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val creatorId: UUID,
    @Column(nullable = false)
    val brandName: String,
    @Column(nullable = false)
    val title: String,
    val description: String? = null,
    @Column(nullable = false, precision = 12, scale = 2)
    val value: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DealStatus = DealStatus.DRAFT,
    val campaignStartDate: Instant? = null,
    val campaignEndDate: Instant? = null,
    @Version val version: Long = 0,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)

// ─── Repository ───────────────────────────────────────────────────────────────

@Repository
interface DealRepository : JpaRepository<Deal, UUID> {
    fun findAllByCreatorId(creatorId: UUID): List<Deal>

    fun findAllByCreatorIdAndStatus(
        creatorId: UUID,
        status: DealStatus,
    ): List<Deal>

    @Query("SELECT SUM(d.value) FROM Deal d WHERE d.creatorId = :creatorId AND d.status = 'COMPLETE'")
    fun totalEarningsByCreator(creatorId: UUID): BigDecimal?
}

// ─── Events ───────────────────────────────────────────────────────────────────

data class DealCreatedEvent(
    val dealId: UUID,
    val creatorId: UUID,
    val brandName: String,
    val value: BigDecimal,
    val occurredAt: Instant = Instant.now(),
)

data class DealStatusChangedEvent(
    val dealId: UUID,
    val from: DealStatus,
    val to: DealStatus,
    val occurredAt: Instant = Instant.now(),
)

// ─── Service ──────────────────────────────────────────────────────────────────

@Service
class DealService(
    private val repo: DealRepository,
    private val kafka: KafkaTemplate<String, Any>,
) {
    fun create(cmd: CreateDealCommand): Deal {
        val deal =
            Deal(
                creatorId = cmd.creatorId,
                brandName = cmd.brandName,
                title = cmd.title,
                description = cmd.description,
                value = cmd.value,
                currency = cmd.currency,
                campaignStartDate = cmd.campaignStartDate,
                campaignEndDate = cmd.campaignEndDate,
            )
        val saved = repo.save(deal)

        // Split across lines to keep the length well below the 140 character limit
        val event = DealCreatedEvent(saved.id, saved.creatorId, saved.brandName, saved.value)
        kafka.send("deal.created", saved.id.toString(), event)

        return saved
    }

    fun transition(
        id: UUID,
        newStatus: DealStatus,
    ): Deal {
        val deal = repo.findById(id).orElseThrow { NoSuchElementException("Deal $id not found") }
        val old = deal.status
        deal.status = newStatus
        deal.updatedAt = Instant.now()
        val saved = repo.save(deal)
        kafka.send("deal.status.changed", id.toString(), DealStatusChangedEvent(id, old, newStatus))
        return saved
    }

    fun findById(id: UUID) = repo.findById(id).orElseThrow { NoSuchElementException("Deal $id not found") }

    fun findByCreator(creatorId: UUID) = repo.findAllByCreatorId(creatorId)

    fun totalEarnings(creatorId: UUID) = repo.totalEarningsByCreator(creatorId) ?: BigDecimal.ZERO
}

// ─── Commands / DTOs ──────────────────────────────────────────────────────────

data class CreateDealCommand(
    val creatorId: UUID,
    @field:NotBlank val brandName: String,
    @field:NotBlank val title: String,
    val description: String? = null,
    @field:Positive val value: BigDecimal,
    val currency: String = "USD",
    val campaignStartDate: Instant? = null,
    val campaignEndDate: Instant? = null,
)

data class TransitionRequest
    @JsonCreator
    constructor(val status: DealStatus)

// ─── Controller ───────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/deals")
class DealController(private val svc: DealService) {
    @PostMapping
    fun create(
        @Valid @RequestBody cmd: CreateDealCommand,
    ) = svc.create(cmd)

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ) = svc.findById(id)

    @GetMapping
    fun list(
        @RequestParam creatorId: UUID,
    ) = svc.findByCreator(creatorId)

    @PatchMapping("/{id}/status")
    fun transition(
        @PathVariable id: UUID,
        @RequestBody req: TransitionRequest,
    ) = svc.transition(id, req.status)

    @GetMapping("/earnings")
    fun earnings(
        @RequestParam creatorId: UUID,
    ) = mapOf("total" to svc.totalEarnings(creatorId))
}

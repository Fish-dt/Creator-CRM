package com.creatorcrm.gateway

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.nio.charset.StandardCharsets

@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties::class)
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}

@ConfigurationProperties(prefix = "app")
data class GatewayProperties(val jwtSecret: String = "change-me-in-production-min-32-chars!!")

@Component
class JwtAuthFilter(private val props: GatewayProperties) :
    AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig>(NameConfig::class.java) {
    override fun apply(config: NameConfig): GatewayFilter =
        GatewayFilter { exchange, chain ->
            val token = extractToken(exchange)
            if (token == null) {
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                return@GatewayFilter exchange.response.setComplete()
            }
            try {
                val key = Keys.hmacShaKeyFor(props.jwtSecret.toByteArray(StandardCharsets.UTF_8))
                val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
                val mutated =
                    exchange.request.mutate()
                        .header("X-Creator-Id", claims.subject)
                        .header("X-Creator-Role", claims["role"]?.toString() ?: "USER")
                        .build()
                chain.filter(exchange.mutate().request(mutated).build())
            } catch (ex: Exception) {
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                exchange.response.setComplete()
            }
        }

    private fun extractToken(exchange: ServerWebExchange): String? {
        val auth = exchange.request.headers.getFirst("Authorization") ?: return null
        return if (auth.startsWith("Bearer ")) auth.substring(7) else null
    }
}

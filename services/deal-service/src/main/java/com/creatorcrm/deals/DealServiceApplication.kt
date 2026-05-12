package com.creatorcrm.deals

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class DealServiceApplication

fun main(args: Array<String>) {
    runApplication<DealServiceApplication>(*args)
}

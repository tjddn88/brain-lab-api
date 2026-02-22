package com.brainlab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BrainLabApiApplication

fun main(args: Array<String>) {
    runApplication<BrainLabApiApplication>(*args)
}

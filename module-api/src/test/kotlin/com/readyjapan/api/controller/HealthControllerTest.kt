package com.readyjapan.api.controller

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class HealthControllerTest : BehaviorSpec({

    val healthController = HealthController()

    Given("health") {
        When("호출 시") {
            Then("status UP을 반환한다") {
                val response = healthController.health()

                response.success shouldBe true
                response.data.shouldNotBeNull()
                response.data!!.status shouldBe "UP"
                response.data!!.timestamp.shouldNotBeNull()
            }
        }
    }
})

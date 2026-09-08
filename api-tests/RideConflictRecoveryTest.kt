package tests

import client.RidesApi
import io.qameta.allure.Allure
import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import model.ActiveRide
import model.CreateRideRequest
import model.ErrorResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.ApiTestCase
import testdata.ApiTestData

@Feature("API: Ride lifecycle")
class RideConflictRecoveryTest : ApiTestCase() {
    @Test
    @DisplayName("A conflicting order preserves the active ride and succeeds after cancellation")
    @AllureId("2008")
    fun testConflictingOrderPreservesActiveRideAndAllowsRetryAfterCancellation() {
        val token = obtainToken()
        val firstRequest =
            CreateRideRequest(
                ApiTestData.FROM,
                ApiTestData.TO,
                ApiTestData.YELLOW_TARIFF.id,
            )
        val secondRequest =
            CreateRideRequest(
                ApiTestData.SECOND_SEEDED_ORDER.from,
                ApiTestData.SECOND_SEEDED_ORDER.to,
                ApiTestData.TURQUOISE_TARIFF.id,
            )
        val activeRide =
            Allure.step(
                "Create ride A",
                Allure.ThrowableRunnable {
                    val ride = createRide(token, firstRequest)
                    assertRideMatches(ride, firstRequest)
                    ride
                },
            )

        step("Reject order B while ride A is active") {
            val secondRide = createRide(token, secondRequest)
            assertRideMatches(secondRide, secondRequest)
        }

        step("Ride A remains active with its original data") {
            val actual = RidesApi.active(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.id).isEqualTo(activeRide.id)
            assertRideMatches(actual.body, firstRequest)
        }

        step("Cancel ride A") {
            val actual = RidesApi.cancel(token, activeRide.id)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.id).isEqualTo(activeRide.id)
            assertThat(actual.body.status).isEqualTo(ApiTestData.CANCELLED_STATUS)
        }

        step("No active ride remains after cancellation") {
            val actual = RidesApi.active(token)
            assertThat(actual.statusCode).isEqualTo(404)
            assertThat(actual.error).isEqualTo(ErrorResponse(ApiTestData.NO_ACTIVE_RIDE_ERROR))
        }

        val retriedRide =
            Allure.step(
                "Retry order B after cancelling ride A",
                Allure.ThrowableRunnable {
                    val ride = createRide(token, secondRequest)
                    assertThat(ride.id).isNotEqualTo(activeRide.id)
                    assertRideMatches(ride, secondRequest)
                    ride
                },
            )

        step("The retried ride B is now active") {
            val actual = RidesApi.active(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.id).isEqualTo(retriedRide.id)
            assertRideMatches(actual.body, secondRequest)
        }
    }

    private fun createRide(
        token: String,
        request: CreateRideRequest,
    ): ActiveRide {
        val response = RidesApi.create(token, request.from, request.to, request.rideOptionId)
        val ride = response.body
        assertThat(response.statusCode).isEqualTo(201)
        assertThat(ride.id).isPositive()
        return ride
    }

    private fun assertRideMatches(
        actual: ActiveRide,
        expected: CreateRideRequest,
    ) {
        assertThat(actual.from).isEqualTo(expected.from)
        assertThat(actual.to).isEqualTo(expected.to)
        assertThat(actual.option.id).isEqualTo(expected.rideOptionId)
        assertThat(actual.status).isEqualTo(ApiTestData.DRIVER_FOUND_STATUS)
    }
}

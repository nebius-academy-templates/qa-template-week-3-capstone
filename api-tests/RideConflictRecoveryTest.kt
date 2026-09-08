package tests

import client.RidesApi
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
        lateinit var activeRide: ActiveRide
        lateinit var retriedRide: ActiveRide

        step("Create ride A") {
            activeRide = createRide(token, firstRequest)
            assertRideMatches(activeRide, firstRequest)
        }

        step("Reject order B while ride A is active") {
            activeRide = createRide(token, secondRequest)
            assertRideMatches(activeRide, secondRequest)
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

        step("Retry order B after cancelling ride A") {
            retriedRide = createRide(token, secondRequest)
            assertThat(retriedRide.id).isNotEqualTo(activeRide.id)
            assertRideMatches(retriedRide, secondRequest)
        }

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

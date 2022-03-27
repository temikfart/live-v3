package org.icpclive.admin

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.icpclive.admin.advertisement.configureAdvertisement
import org.icpclive.admin.creepingLine.configureCreepingLine
import org.icpclive.admin.overlayEvents.configureOverlayEvents
import org.icpclive.admin.picture.configurePicture
import org.icpclive.admin.queue.configureQueue
import org.icpclive.admin.scoreboard.configureScoreboard
import org.icpclive.admin.statistics.configureStatistics

private lateinit var topLevelLinks: List<Pair<String, String>>

internal fun BODY.adminHead() {
    table {
        tr {
            for ((url, text) in topLevelLinks) {
                td {
                    a(url) { +text }
                }
            }
        }
    }
}

suspend inline fun ApplicationCall.catchAdminAction(back: String, block: ApplicationCall.() -> Unit) = try {
    block()
} catch (e: AdminActionException) {
    respondHtml {
        body {
            h1 {
                +"Error: ${e.message}"
            }
            a {
                href = back
                +"Back"
            }
        }
    }
}


fun Application.configureAdminRouting() {
    routing {
        val advertisementUrls =
            configureAdvertisement(environment.config.property("live.presets.advertisements").getString())
        val pictureUrls = configurePicture(environment.config.property("live.presets.pictures").getString())
        val overlayEventsUrls = configureOverlayEvents()
        val queueUrls = configureQueue()
        val scoreboardUrls = configureScoreboard()
        val statisticsUrls = configureStatistics()
        val creepingLineUrls = configureCreepingLine(environment.config.property("live.presets.creepingLine").getString())

        topLevelLinks = listOf(
            advertisementUrls.mainPage to "Advertisement",
            pictureUrls.mainPage to "Picture",
            overlayEventsUrls.mainPage to "Events",
            queueUrls.mainPage to "Queue",
            scoreboardUrls.mainPage to "Scoreboard",
            statisticsUrls.mainPage to "Statistics",
            creepingLineUrls.mainPage to "Creeping Line"
        )
        get("/admin") { call.respondRedirect(topLevelLinks[0].first) }
    }
}
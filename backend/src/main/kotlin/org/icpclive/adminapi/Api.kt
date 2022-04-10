package org.icpclive.adminapi

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import org.icpclive.api.*
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import java.nio.file.Path

private suspend inline fun <reified T> ApplicationCall.safeReceive(): T = try {
    receive()
} catch (e: SerializationException) {
    throw AdminActionApiException("Failed to deserialize data")
}

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupSimpleWidgetRouting(
    widgetWrapper: Wrapper<SettingsType, WidgetType>,
    noinline getInfo: (() -> Any)?
) {
    get {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(widgetWrapper.getStatus())
        }
    }
    post("/show") {
        call.adminApiAction {
            widgetWrapper.show(call.safeReceive())
        }
    }
    post("/hide") {
        call.adminApiAction {
            widgetWrapper.hide()
        }
    }
    getInfo?.let {
        get("/info") {
            call.adminApiAction {
                val response = it()
                call.respond(response)
            }
        }
    }
}

private fun ApplicationCall.id() =
    parameters["id"]?.toIntOrNull() ?: throw AdminActionApiException("Error load preset by id")

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : TypeWithId> Route.setupPresetRouting(
    presets: PresetsManager<SettingsType, WidgetType>
) {
    get {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(presets.getStatus())
        }
    }
    post {
        call.adminApiAction {
            presets.append(call.safeReceive())
        }
    }
    post("/{id}") {
        call.adminApiAction {
            presets.edit(call.id(), call.safeReceive())
        }
    }
    delete("/{id}") {
        call.adminApiAction {
            presets.delete(call.id())
        }
    }
    post("/{id}/show") {
        call.adminApiAction {
            presets.show(call.id())
        }
    }
    post("/{id}/hide") {
        call.adminApiAction {
            presets.hide(call.id())
        }
    }
}

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupSimpleWidgetRouting(
    initialSettings: SettingsType,
    noinline createWidget: (SettingsType) -> WidgetType,
    noinline getInfo: (() -> Any)? = null
) = setupSimpleWidgetRouting(Wrapper(createWidget, initialSettings, WidgetManager), getInfo)

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupPresetWidgetRouting(
    presetPath: Path,
    noinline createWidget: (SettingsType) -> WidgetType
) = setupPresetRouting(Presets(presetPath, WidgetManager, createWidget))

internal fun Route.setupPresetTickerRouting(
    presetPath: Path,
    createMessage: (TickerMessageSettings) -> TickerMessage,
) = setupPresetRouting(Presets(presetPath, TickerManager, createMessage))
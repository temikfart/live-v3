package org.icpclive.admin.picture

import io.ktor.routing.*
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.radioInput
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.admin.AdminActionException
import org.icpclive.admin.setupSimpleWidgetRouting
import org.icpclive.admin.simpleWidgetViewFun
import org.icpclive.api.PictureSettings
import org.icpclive.api.PictureWidget

fun Routing.configurePicture(presetPath: String) =
    setupSimpleWidgetRouting(
        prefix = "picture",
        widgetId = PictureWidget.WIDGET_ID,
        presetPath = presetPath,
        createWidget = { parameters ->
            val pictureSettings = parameters["picture"]?.let { Json.decodeFromString<PictureSettings>(it) }
                ?: throw AdminActionException("No picture chosen")
            PictureWidget(pictureSettings)
        },
        view = simpleWidgetViewFun<PictureSettings>("Picture") { presets ->
            for (pic in presets!!.data.get()) {
                p {
                    radioInput {
                        name = "picture"
                        value = Json.encodeToString(pic)
                        +pic.name
                    }
                    img {
                        src = pic.url
                        height = "100px"
                    }
                }
            }
        },
    )
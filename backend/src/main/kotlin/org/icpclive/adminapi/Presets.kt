package org.icpclive.adminapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.icpclive.admin.AdminActionException
import org.icpclive.api.Preset
import org.icpclive.api.ObjectSettings
import org.icpclive.api.Widget
import org.icpclive.data.WidgetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class Presets<SettingsType : ObjectSettings, WidgetType : Widget>(
        private val path: String,
        private val decode: (String) -> List<Preset<SettingsType>>,
        private val encode: (List<Preset<SettingsType>>, String) -> Unit,
        private val createWidget: (SettingsType) -> WidgetType,
        private var innerData: List<Preset<SettingsType>> = decode(path),
        private var currentID: Int = innerData.size
) {
    private val mutex = Mutex()

    suspend fun get(): List<Preset<SettingsType>> = mutex.withLock {
        return innerData.map { it.copy() }
    }

    suspend fun append(settings: SettingsType) {
        mutex.withLock {
            innerData = innerData.plus(Preset(++currentID, settings))
        }
        save()
    }

    suspend fun edit(id: Int, content: SettingsType) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id == id)
                    preset.content = content
            }
        }
        save()
    }

    suspend fun delete(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.widgetId?.let {
                    WidgetManager.hideWidget(it)
                }
                preset.widgetId = null
            }
            innerData = innerData.filterNot { it.id == id }
        }
        save()
    }

    suspend fun show(id: Int) = mutex.withLock {
        for (preset in innerData) {
            if (preset.id != id)
                continue
            if (preset.widgetId != null)
                continue
            val widget = createWidget(preset.content)
            WidgetManager.showWidget(widget)
            preset.widgetId = widget.widgetId
            break
        }
    }

    suspend fun hide(id: Int) = mutex.withLock {
        for (preset in innerData) {
            if (preset.id != id)
                continue
            preset.widgetId?.let {
                WidgetManager.hideWidget(it)
            }
            preset.widgetId = null
        }
    }

    suspend private fun load() = mutex.withLock {
        try {
            innerData = decode(path)
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to deserialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error reading presets: ${e.message}")
        }
    }

    suspend private fun save() = mutex.withLock {
        try {
            encode(innerData, path)
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to deserialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error reading presets: ${e.message}")
        }
    }
}

@ExperimentalSerializationApi
inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Presets(path: String,
                                                                                        noinline createWidget: (SettingsType) -> WidgetType) =
        Presets<SettingsType, WidgetType>(path,
                {
                    Json.decodeFromStream<List<SettingsType>>(FileInputStream(File(path))).mapIndexed { index, content ->
                        Preset(index + 1, content)
                    }
                },
                { data, fileName ->
                    Json { prettyPrint = true }.encodeToStream(data.map { it.content }, FileOutputStream(File(fileName)))
                },
                createWidget)
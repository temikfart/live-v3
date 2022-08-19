package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.WidgetControllers
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.getLogger
import org.icpclive.widget.PresetsController
import kotlin.time.Duration.Companion.milliseconds

sealed class AnalyticsAction(val messageId: String) {
    class CreateAdvertisement(messageId: String, val ttlMs: Long?) : AnalyticsAction(messageId)
    class DeleteAdvertisement(messageId: String) : AnalyticsAction(messageId)
    class CreateTickerMessage(messageId: String, val ttlMs: Long?) : AnalyticsAction(messageId)
    class DeleteTickerMessage(messageId: String) : AnalyticsAction(messageId)
    class MakeRunFeatured(messageId: String) : AnalyticsAction(messageId)
}


class AnalyticsService {
    @OptIn(DelicateCoroutinesApi::class)
    private val scheduledTasksScope: CoroutineScope = GlobalScope
    private val internalActions = MutableSharedFlow<AnalyticsAction>()
    private val messages = mutableMapOf<String, AnalyticsMessage>()

    private val resultFlow = MutableSharedFlow<AnalyticsEvent>(
        extraBufferCapacity = 50000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val subscriberFlow = MutableStateFlow(0)

    private suspend fun <S : ObjectSettings, T : TypeWithId> AnalyticsCompanionPreset.hide(controller: PresetsController<S, T>) {
        controller.hide(this.presetId)
    }

    private fun scheduleAction(timeMs: Long, action: AnalyticsAction) {
        scheduledTasksScope.launch {
            delay(timeMs)
            internalActions.emit(action)
        }
    }

    private suspend fun Action.process(): AnalyticsMessage? {
        val message = messages[action.messageId]
        if (message == null) {
            logger.warn("Message with id ${action.messageId} not found")
            return null
        }
        if (message !is AnalyticsCommentaryEvent) {
            logger.warn("Unsupported action for analytics message $message")
            return null
        }
        when (action) {
            is AnalyticsAction.CreateAdvertisement -> {
                message.advertisement?.hide(WidgetControllers.advertisement)
                val presetId = WidgetControllers.advertisement.append(AdvertisementSettings(message.message))
                WidgetControllers.advertisement.show(presetId)
                message.advertisement =
                    AnalyticsCompanionPreset(presetId, action.ttlMs?.let { Clock.System.now() + it.milliseconds })
                action.ttlMs?.let { scheduleAction(it, AnalyticsAction.DeleteAdvertisement(action.messageId)) }
            }
            is AnalyticsAction.DeleteAdvertisement -> {
                message.advertisement?.hide(WidgetControllers.advertisement)
                message.advertisement = null
            }
            is AnalyticsAction.CreateTickerMessage -> {
                message.tickerMessage?.hide(WidgetControllers.tickerMessage)
                val presetId = WidgetControllers.tickerMessage.append(
                    TextTickerSettings(TickerPart.LONG, 30000, message.message)
                )
                WidgetControllers.tickerMessage.show(presetId)
                AnalyticsCompanionPreset(presetId, action.ttlMs?.let { Clock.System.now() + it.milliseconds })
                action.ttlMs?.let { scheduleAction(it, AnalyticsAction.DeleteTickerMessage(action.messageId)) }
            }
            is AnalyticsAction.DeleteTickerMessage -> {
                message.tickerMessage?.hide(WidgetControllers.tickerMessage)
                message.tickerMessage = null
            }
            is AnalyticsAction.MakeRunFeatured -> {
                if (message.runIds.size != 1) {
                    logger.warn("Can't make run featured caused by message ${message.id}")
                    return null
                }
                DataBus.queueFeaturedRunsFlow.emit(message.runIds[0])
            }
        }
        return message
    }

    suspend fun run(rawEvents: Flow<AnalyticsMessage>) {
        val actionFlow = merge(DataBus.analyticsActionsFlow.await(), internalActions).map(::Action)
        logger.info("Analytics service is started")
        merge(rawEvents.map(::Message), subscriberFlow.map { Subscribe }, actionFlow).collect { event ->
            when (event) {
                is Message -> {
                    val message = event.message
                    messages[message.id] = message
                    resultFlow.emit(AddAnalyticsMessageEvent(message))
                }
                is Action -> {
                    event.process()?.let { resultFlow.emit(ModifyAnalyticsMessageEvent(it)) }
                }
                is Subscribe -> {
                    resultFlow.emit(AnalyticsMessageSnapshotEvent(messages.values.sortedBy { it.relativeTime }))
                }
            }
        }
    }

    init {
        DataBus.analyticsFlow.completeOrThrow(flow {
            var needSnapshot = true
            resultFlow
                .onSubscription { subscriberFlow.update { it + 1 } }
                .collect {
                    if (it is AnalyticsMessageSnapshotEvent == needSnapshot) {
                        emit(it)
                        needSnapshot = false
                    }
                }
        })
    }

    companion object {
        val logger = getLogger(AnalyticsMessage::class)

        private sealed class AnalyticsProcessTrigger
        private class Message(val message: AnalyticsMessage) : AnalyticsProcessTrigger()
        private class Action(val action: AnalyticsAction) : AnalyticsProcessTrigger()
        private object Subscribe : AnalyticsProcessTrigger()
    }
}


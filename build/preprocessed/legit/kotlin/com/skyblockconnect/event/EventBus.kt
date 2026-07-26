@file:Suppress("UNCHECKED_CAST")

package com.skyblockconnect.event

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.utils.ChatUtils
import java.util.concurrent.*

object EventBus {
    class EventContext<T: Event>(val event: T, var listener: EventListener<T>)

    val listeners = ConcurrentHashMap<Class<out Event>, List<EventListener<*>>>()

    @JvmStatic
    fun hasListeners(cls: Class<out Event>): Boolean = listeners[cls] != null

    @JvmStatic
    fun post(event: Event): Boolean {
        val handlers = listeners[event.javaClass] ?: return event.cancelable && event.isCanceled
        var context: EventContext<Event>? = null

        for (i in handlers.indices) {
            val handler = handlers[i]
            try {
                val typedHandler = handler as EventListener<Event>
                val currentContext = context ?: EventContext(event, typedHandler).also { context = it }
                currentContext.listener = typedHandler
                typedHandler.callback.invoke(currentContext)
            }
            catch (e: Exception) {
                val stacktrace = e.stackTrace.joinToString("\n")
                SkyblockConnect.logger.error("EventBus Error in ${event.javaClass.name}", e)

                val where = e.stackTrace.firstOrNull()
                    ?.let { " at ${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
                    .orEmpty()
                ChatUtils.clickableChat(
                    "EventBus Error: ${event.javaClass.simpleName} -> ${e.javaClass.simpleName}: ${e.message ?: "(no message)"}$where",
                    true, copy = stacktrace, hover = stacktrace,
                )
            }
        }

        return event.cancelable && event.isCanceled
    }

    @JvmStatic
    inline fun <reified T: Event> register(
        priority: EventPriority = EventPriority.NORMAL,
        noinline block: EventContext<T>.() -> Unit
    ): EventListener<T> {
        return EventListener(T::class.java, priority, block).register()
    }

    fun unregister(listener: EventListener<*>) = synchronized(listeners) {
        val oldListeners = listeners[listener.eventClass] ?: return@synchronized
        val newListeners = oldListeners.filter { it !== listener }

        if (newListeners.isEmpty()) listeners.remove(listener.eventClass)
        else listeners[listener.eventClass] = newListeners
    }

    fun register(listener: EventListener<*>) = synchronized(listeners) {
        val oldListeners = listeners[listener.eventClass] ?: emptyList()
        if (oldListeners.contains(listener)) return@synchronized
        listeners[listener.eventClass] = (oldListeners + listener).sortedBy { it.priority.ordinal }
    }
}
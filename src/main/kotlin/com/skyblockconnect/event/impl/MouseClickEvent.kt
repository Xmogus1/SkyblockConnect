package com.skyblockconnect.event.impl

import com.skyblockconnect.event.Event

class MouseClickEvent(val button: Int, val action: Int, val modifiers: Int): Event(true)

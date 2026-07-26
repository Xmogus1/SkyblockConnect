package com.skyblockconnect.event.impl

import com.skyblockconnect.event.Event
import com.skyblockconnect.utils.render.RenderContext
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.Level
import org.joml.Matrix4f

class RenderWorldEvent(val ctx: RenderContext): Event(cancelable = false)

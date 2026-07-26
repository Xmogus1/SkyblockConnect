package com.skyblockconnect.utils.render

import com.skyblockconnect.SkyblockConnect.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource

data class RenderContext(val matrixStack: PoseStack, val consumers: MultiBufferSource, val camera: Camera) {
    companion object {
        fun fromContext(ctx: LevelRenderContext): RenderContext {
            return RenderContext(ctx.poseStack(), ctx.bufferSource(), mc.gameRenderer.mainCamera)
        }
    }
}

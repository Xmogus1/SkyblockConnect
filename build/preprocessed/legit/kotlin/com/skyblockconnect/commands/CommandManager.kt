package com.skyblockconnect.commands

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.utils.catch
import io.github.classgraph.ClassGraph
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands

object CommandManager {
    val commands = mutableSetOf<BaseCommand>()

    init {
        val result = ClassGraph()
            .acceptPackages(SkyblockConnect::class.java.`package`.name)
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        result.use {
            it.getSubclasses(BaseCommand::class.qualifiedName).forEach { ci ->
                val i = catch { ci.loadClass().getDeclaredField("INSTANCE").get(null) as? BaseCommand }
                i?.let(commands::add)
            }
        }
    }

    fun registerAll() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { command ->
                val roots = mutableListOf(ClientCommands.literal(command.name))
                command.aliases.forEach { roots.add(ClientCommands.literal(it)) }
                roots.forEach { root ->
                    CommandNodeBuilder(root).apply { with(command) { build() } }
                    dispatcher.register(root)
                }
                SkyblockConnect.logger.debug("Registered command: /${command.name}")
            }
        }
    }
}
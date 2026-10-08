@file:Suppress("LocalVariableName")

package foo.starred.autoclicker

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.autoclicker.config.AutoClickerConfig
import foo.starred.autoclicker.mixin.accessors.InputConstantsKeyAccessor
import foo.starred.autoclicker.mixin.accessors.KeyMappingAccessor
import foo.starred.autoclicker.utils.id
import foo.starred.autoclicker.utils.uuid
import foo.starred.kommand.IKommand
import foo.starred.kommand.scopes.KommandCommandScope
import foo.starred.snowbird.api.client
import foo.starred.snowbird.api.inputs.impl.GenericInputState
import foo.starred.snowbird.api.lie
import foo.starred.snowbird.api.repeat
import foo.starred.snowbird.api.text.parser.impl.parse
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.BlockHitResult

object AutoClicker : ClientModInitializer, IKommand<FabricClientCommandSource> {
    override val loader: KommandCommandScope<FabricClientCommandSource> = KommandCommandScope()

    private val keys: List<String> by lazy {
        InputConstantsKeyAccessor.name_map().keys.toList()
    }

    private var delay0: Int = 0
    private var delay1: Int = 0

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            kommand()
            loader.register(dispatcher)
        }

        ClientTickEvents.START_CLIENT_TICK.register {
            if (!AutoClickerConfig.enabled) return@register
            //~ if >= 26.2 'client.screen' -> 'client.gui.screen()'
            if (client.screen != null) return@register

            val player = client.player ?: return@register
            val level = client.level ?: return@register

            if (player.isUsingItem) return@register
            if (client.gameMode?.isDestroying ?: false) return@register

            val held = held() ?: return@register
            if (AutoClickerConfig.breaker && held == "DUNGEONBREAKER") return@register

            val whitelist = AutoClickerConfig.whitelist
            val _left = !whitelist || held in AutoClickerConfig.set1.value
            val _right = !whitelist || held in AutoClickerConfig.set2.value
            if (!_left && !_right) return@register

            val left = _left && AutoClickerConfig.left && GenericInputState.pressed(AutoClickerConfig.leftKey)
            val right = _right && AutoClickerConfig.right && GenericInputState.pressed(AutoClickerConfig.rightKey)

            val hit = client.hitResult as? BlockHitResult
            if (hit != null && !level.getBlockState(hit.blockPos).isAir && AutoClickerConfig.breaking) {
                KeyMapping.set((client.options.keyAttack as KeyMappingAccessor).boundKey, true)
                return@register
            }

            if (left) {
                delay0 += AutoClickerConfig.left_cps.delay()
                if (delay0 >= 20) {
                    left()
                    delay0 -= 20
                }
            }

            if (right) {
                delay1 += AutoClickerConfig.right_cps.delay()
                if (delay1 >= 20) {
                    right()
                    delay1 -= 20
                }
            }
        }
    }

    private fun Int.delay(): Int {
        val a = AutoClickerConfig.jitter * 2
        return (this + (-a..a).random()).coerceIn(1, 20)
    }

    private fun held(): String? {
        val held = client.player?.mainHandItem ?: return null
        return held.uuid() ?: held.id() ?: held.hoverName.string
    }

    private fun left() {
        val options = client.options
        val key = (options.keyAttack as KeyMappingAccessor).boundKey

        KeyMapping.set(key, true)
        KeyMapping.click(key)
        KeyMapping.set(key, false)
    }

    private fun right() {
        val options = client.options
        val key = (options.keyUse as KeyMappingAccessor).boundKey

        KeyMapping.set(key, true)
        KeyMapping.click(key)
        KeyMapping.set(key, false)
    }

    private fun kommand() {
        command("autoclicker") {
            executes {
                config()
            }

            "config" {
                config()
            }

            "toggle" {
                val last = AutoClickerConfig.enabled
                AutoClickerConfig.enabled = !last

                "<dark_gray>- <yellow>AutoClicker<r> is now ${if (last) "<red>disabled" else "<green>enabled"}<r>!".parse(true).lie()
            }

            "leftClicker" {
                AutoClickerConfig.left = !AutoClickerConfig.left

                "<dark_gray>- <yellow>AutoClicker<r>: leftClicker=${AutoClickerConfig.left}<r>!".parse(true).lie()
            }

            "leftCps" / int("cps", 3, 20) {
                AutoClickerConfig.left_cps = int("cps")

                "<dark_gray>- <yellow>AutoClicker<r>: leftCps=${AutoClickerConfig.left_cps}<r>!".parse(true).lie()
            }

            "leftKeybind".then {
                executes {
                    val current = GenericInputState.name(AutoClickerConfig.leftKey)
                    "<dark_gray>- <yellow>AutoClicker<r>: leftKeybind is currently <green>$current<r>!".parse(true).lie()
                }

                word("key") {
                    val name = string("key").lowercase()
                    val map = InputConstantsKeyAccessor.name_map()
                    val key = if (name == "none") InputConstants.UNKNOWN else map[name] ?: map["key.keyboard.$name"] ?: map["key.mouse.$name"]

                    if (key == null) {
                        "<dark_gray>- <red>AutoClicker<r>: Unknown key <yellow>$name<r>!".parse(true).lie()
                        return@word
                    }

                    AutoClickerConfig.leftKey = key
                    "<dark_gray>- <yellow>AutoClicker<r>: leftKeybind set to <green>${GenericInputState.name(key)}<r>!".parse(true).lie()
                }.suggests { keys }
            }

            "rightClicker" {
                AutoClickerConfig.right = !AutoClickerConfig.right

                "<dark_gray>- <yellow>AutoClicker<r>: rightClicker=${AutoClickerConfig.right}<r>!".parse(true).lie()
            }

            "rightCps" / int("cps", 3, 20) {
                AutoClickerConfig.right_cps = int("cps")

                "<dark_gray>- <yellow>AutoClicker<r>: leftCps=${AutoClickerConfig.right_cps}<r>!".parse(true).lie()
            }

            "rightKeybind".then {
                executes {
                    val current = GenericInputState.name(AutoClickerConfig.rightKey)
                    "<dark_gray>- <yellow>AutoClicker<r>: rightKeybind is currently <green>$current<r>!".parse(true).lie()
                }

                word("key") {
                    val name = string("key").lowercase()
                    val map = InputConstantsKeyAccessor.name_map()
                    val key = if (name == "none") InputConstants.UNKNOWN else map[name] ?: map["key.keyboard.$name"] ?: map["key.mouse.$name"]

                    if (key == null) {
                        "<dark_gray>- <red>AutoClicker<r>: Unknown key <yellow>$name<r>!".parse(true).lie()
                        return@word
                    }

                    AutoClickerConfig.rightKey = key
                    "<dark_gray>- <yellow>AutoClicker<r>: rightKeybind set to <green>${GenericInputState.name(key)}<r>!".parse(true).lie()
                }.suggests { keys }
            }

            "blockDungeonBreaker" {
                AutoClickerConfig.breaker = !AutoClickerConfig.breaker

                "<dark_gray>- <yellow>AutoClicker<r>: blockDungeonBreaker=${AutoClickerConfig.breaker}<r>!".parse(true).lie()
            }

            "allowBlockBreaking" {
                AutoClickerConfig.breaking = !AutoClickerConfig.breaking

                "<dark_gray>- <yellow>AutoClicker<r>: allowBlockBreaking=${AutoClickerConfig.breaking}<r>!".parse(true).lie()
            }

            "whitelist" / "toggle" {
                AutoClickerConfig.whitelist = !AutoClickerConfig.whitelist

                "<dark_gray>- <yellow>AutoClicker<r>: whitelist=${AutoClickerConfig.whitelist}<r>!".parse(true).lie()
            }

            "whitelist" / "add" / "left" {
                val held = held() ?: return@invoke "<dark_gray>- <yellow>AutoClicker<r>: Hold an item to whitelist.".parse(true).lie()
                if (held in AutoClickerConfig.set1.value) return@invoke "<dark_gray>- <yellow>AutoClicker<r>: $held is already in left whitelist!".parse(true).lie()

                AutoClickerConfig.set1.update { add(held) }
                "<dark_gray>- <yellow>AutoClicker<r>: Added <green>$held<r> to left whitelist!".parse(true).lie()
            }

            "whitelist" / "add" / "right" {
                val held = held() ?: return@invoke "<dark_gray>- <yellow>AutoClicker<r>: Hold an item to whitelist.".parse(true).lie()
                if (held in AutoClickerConfig.set2.value) return@invoke "<dark_gray>- <yellow>AutoClicker<r>: $held is already in right whitelist!".parse(true).lie()

                AutoClickerConfig.set2.update { add(held) }
                "<dark_gray>- <yellow>AutoClicker<r>: Added <green>$held<r> to right whitelist!".parse(true).lie()
            }

            "whitelist" / "remove" / "left" {
                val held = held() ?: return@invoke "<dark_gray>- <yellow>AutoClicker<r>: Hold an item to remove from whitelist.".parse(true).lie()
                if (held !in AutoClickerConfig.set1.value) return@invoke "<dark_gray>- <yellow>AutoClicker<r>: $held is not in left whitelist!".parse(true).lie()

                AutoClickerConfig.set1.update { remove(held) }
                "<dark_gray>- <yellow>AutoClicker<r>: Removed <green>$held<r> from left whitelist!".parse(true).lie()
            }

            "whitelist" / "remove" / "right" {
                val held = held() ?: return@invoke "<dark_gray>- <yellow>AutoClicker<r>: Hold an item to remove from whitelist.".parse(true).lie()
                if (held !in AutoClickerConfig.set2.value) return@invoke "<dark_gray>- <yellow>AutoClicker<r>: $held is not in right whitelist!".parse(true).lie()

                AutoClickerConfig.set2.update { remove(held) }
                "<dark_gray>- <yellow>AutoClicker<r>: Removed <green>$held<r> from right whitelist!".parse(true).lie()
            }

            "whitelist" / "clear" / "left" {
                AutoClickerConfig.set1.update { clear() }
                "<dark_gray>- <yellow>AutoClicker<r>: Cleared left whitelist.".parse(true).lie()
            }

            "whitelist" / "clear" / "right" {
                AutoClickerConfig.set2.update { clear() }
                "<dark_gray>- <yellow>AutoClicker<r>: Cleared right whitelist.".parse(true).lie()
            }

            "whitelist" / "list" {
                val a = ("<gray>" + ("-".repeat())).parse()

                "<dark_gray>- <yellow>AutoClicker<r> whitelist:".parse(true).lie()
                a.lie()

                "<dark_gray>- <yellow>AutoClicker<r> left whitelist:".lie()
                for (s in AutoClickerConfig.set1.value) " <dark_gray>- <gray>$s".parse(true).lie()
                a.lie()

                "<dark_gray>- <yellow>AutoClicker<r> right whitelist:".lie()
                for (s in AutoClickerConfig.set2.value) " <dark_gray>- <gray>$s".parse(true).lie()
                a.lie()
            }
        }
    }

    private fun config() {
        ("<dark_gray>" + "-".repeat()).parse().lie()

        "<yellow>[AutoClicker]<r> Config Commands:".parse(true).lie()

        "<dark_gray>- <aqua>/autoclicker toggle <gray>- <r>Toggles the mod".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker leftClicker <gray>- <r>Toggle left clicker".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker leftCps [cps] <gray>- <r>Left clicker CPS (3-20)".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker leftKeybind [key] <gray>- <r>Left clicker keybind".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker rightClicker <gray>- <r>Toggle right clicker".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker rightCps [cps] <gray>- <r>Right clicker CPS (3-20)".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker rightKeybind [key] <gray>- <r>Right clicker keybind".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker blockDungeonBreaker <gray>- <r>Block Dungeon Breaker clicks".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker allowBlockBreaking <gray>- <r>Allow breaking blocks".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker whitelist toggle <gray>- <r>Toggle item whitelist".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker whitelist [add|remove] [left|right] <gray>- <r>Manage item whitelist".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker whitelist clear [left|right] <gray>- <r>Clear item whitelist".parse(true).lie()
        "<dark_gray>- <aqua>/autoclicker whitelist list <gray>- <r>List all items in whitelist".parse(true).lie()

        ("<dark_gray>" + "-".repeat()).parse().lie()

        "<yellow>[AutoClicker]<r> Config Values:".parse(true).lie()

        "<dark_gray>- <aqua>Enabled: <r>${AutoClickerConfig.enabled}".parse(true).lie()
        "<dark_gray>- <aqua>Left clicker: <r>${AutoClickerConfig.left}".parse(true).lie()
        "<dark_gray>- <aqua>Left CPS: <r>${AutoClickerConfig.left_cps}".parse(true).lie()
        "<dark_gray>- <aqua>Left keybind: <r>${GenericInputState.name(AutoClickerConfig.leftKey)}".parse(true).lie()
        "<dark_gray>- <aqua>Right clicker: <r>${AutoClickerConfig.right}".parse(true).lie()
        "<dark_gray>- <aqua>Right CPS: <r>${AutoClickerConfig.right_cps}".parse(true).lie()
        "<dark_gray>- <aqua>Right keybind: <r>${GenericInputState.name(AutoClickerConfig.rightKey)}".parse(true).lie()
        "<dark_gray>- <aqua>Block Dungeon Breaker: <r>${AutoClickerConfig.breaker}".parse(true).lie()
        "<dark_gray>- <aqua>Allow block breaking: <r>${AutoClickerConfig.breaking}".parse(true).lie()
        "<dark_gray>- <aqua>Whitelist: <r>${AutoClickerConfig.whitelist}".parse(true).lie()

        ("<dark_gray>" + "-".repeat()).parse().lie()

        "<dark_gray>- <aqua>Need help? Join the discord! <click:url:https://discord.gg/DB5S3DjQVa><hover:Click to open><red>https://discord.gg/DB5S3DjQVa".parse(true).lie()

        ("<dark_gray>" + "-".repeat()).parse().lie()
    }
}

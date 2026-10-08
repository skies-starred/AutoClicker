package foo.starred.autoclicker.config

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import foo.starred.snowbird.api.storage.AbstractJsonStore

object AutoClickerConfig {
    private val STORAGE: AbstractJsonStore = AbstractJsonStore("autoclicker", "main")

    @JvmStatic
    var enabled: Boolean by STORAGE.boolean("enabled", false)

    @JvmStatic
    var left: Boolean by STORAGE.boolean("left", false)

    @JvmStatic
    var left_key: String by STORAGE.string("left_key", InputConstants.UNKNOWN.name)

    @JvmStatic
    var leftKey: InputConstants.Key = runCatching { InputConstants.getKey(left_key) }.getOrDefault(InputConstants.UNKNOWN)
        set(value) {
            field = value
            left_key = value.name
        }

    @JvmStatic
    var left_cps: Int by STORAGE.int("left_cps", 3)

    @JvmStatic
    var right: Boolean by STORAGE.boolean("right", false)

    @JvmStatic
    var right_key: String by STORAGE.string("right_key", InputConstants.UNKNOWN.name)

    @JvmStatic
    var rightKey: InputConstants.Key = runCatching { InputConstants.getKey(right_key) }.getOrDefault(InputConstants.UNKNOWN)
        set(value) {
            field = value
            right_key = value.name
        }

    @JvmStatic
    var right_cps: Int by STORAGE.int("right_cps", 3)

    @JvmStatic
    var jitter: Int by STORAGE.int("jitter", 2)

    @JvmStatic
    var breaker: Boolean by STORAGE.boolean("breaker", false)

    @JvmStatic
    var breaking: Boolean by STORAGE.boolean("breaking", false)

    @JvmStatic
    var whitelist: Boolean by STORAGE.boolean("whitelist", false)

    @JvmStatic
    val set1: AbstractJsonStore.Value<MutableSet<String>> = STORAGE.mutableSet("left_whitelist", Codec.STRING)

    @JvmStatic
    val set2: AbstractJsonStore.Value<MutableSet<String>> = STORAGE.mutableSet("right_whitelist", Codec.STRING)
}

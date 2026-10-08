package foo.starred.autoclicker.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

fun ItemStack.customData(): CompoundTag? {
    return get(DataComponents.CUSTOM_DATA)?.copyTag()
}

fun ItemStack.id(): String? {
    return customData()?.getString("id")?.getOrNull()
}

fun ItemStack.uuid(): String? {
    return customData()?.getString("uuid")?.getOrNull()
}

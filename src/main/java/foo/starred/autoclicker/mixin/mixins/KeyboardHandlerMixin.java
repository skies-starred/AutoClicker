package foo.starred.autoclicker.mixin.mixins;

import foo.starred.autoclicker.config.AutoClickerConfig;
import foo.starred.autoclicker.mixin.accessors.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static foo.starred.snowbird.api.ClientKt.client;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void autoclicker$keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action != 0) return;
        if (!AutoClickerConfig.getEnabled()) return;
        if (event.key() != AutoClickerConfig.getLeftKey().getValue()) return;

        KeyMapping.set(((KeyMappingAccessor) client.options.keyAttack).getBoundKey(), false);
    }
}

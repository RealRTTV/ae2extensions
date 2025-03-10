package ca.rttv.ae2extensions.mixin;

import ca.rttv.ae2extensions.duck.GameOptionsDuck;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameOptions.class)
public class GameOptionsMixin implements GameOptionsDuck {
    @Unique
    private final KeyBinding extensionsKey = new KeyBinding("key.extensions", GLFW.GLFW_KEY_LEFT_BRACKET, "key.ae2.category");
    @Unique
    private final KeyBinding devnullKey = new KeyBinding("key.devnull", GLFW.GLFW_KEY_RIGHT_BRACKET, "key.ae2.category");
    @Unique
    private final KeyBinding shelveKey = new KeyBinding("key.shelve", GLFW.GLFW_KEY_GRAVE_ACCENT, "key.ae2.category");
    @Unique
    private final KeyBinding restockKey = new KeyBinding("key.restock", GLFW.GLFW_KEY_APOSTROPHE, "key.ae2.category");

    @Override
    public KeyBinding ae2extensions$getExtensionsKey() {
        return extensionsKey;
    }

    @Override
    public KeyBinding ae2extensions$getDevNullKey() {
        return devnullKey;
    }

    @Override
    public KeyBinding ae2extensions$getShelveKey() {
        return shelveKey;
    }

    @Override
    public KeyBinding ae2extensions$getRestockKey() {
        return restockKey;
    }

    @SuppressWarnings("unchecked")
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/ArrayUtils;addAll([Ljava/lang/Object;[Ljava/lang/Object;)[Ljava/lang/Object;"), index = 0)
    private <T> T[] addAll(T[] arr) {
        KeyBinding[] newKeys = new KeyBinding[]{devnullKey, extensionsKey, shelveKey, restockKey};
        KeyBinding[] keys = new KeyBinding[arr.length + newKeys.length];
        System.arraycopy(arr, 0, keys, 0, arr.length);
        System.arraycopy(newKeys, 0, keys, arr.length, newKeys.length);
        return (T[]) keys;
    }
}

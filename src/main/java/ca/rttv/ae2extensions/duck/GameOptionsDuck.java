package ca.rttv.ae2extensions.duck;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

public interface GameOptionsDuck {
    KeyBinding ae2extensions$getExtensionsKey();
    KeyBinding ae2extensions$getDevNullKey();
    KeyBinding ae2extensions$getShelveKey();
    KeyBinding ae2extensions$getRestockKey();

    static KeyBinding getExtensionsKey(GameOptions options) {
        return ((GameOptionsDuck) options).ae2extensions$getExtensionsKey();
    }

    static KeyBinding getDevNullKey(GameOptions options) {
        return ((GameOptionsDuck) options).ae2extensions$getDevNullKey();
    }

    static KeyBinding getShelveKey(GameOptions options) {
        return ((GameOptionsDuck) options).ae2extensions$getShelveKey();
    }

    static KeyBinding getRestockKey(GameOptions options) {
        return ((GameOptionsDuck) options).ae2extensions$getRestockKey();
    }
}

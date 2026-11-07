package com.minhphuc.weapons.init;

import com.minhphuc.weapons.WeaponsMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(WeaponsMod.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> ATOMIC_CHARGE = SOUND_EVENTS.register("atomic_charge",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "atomic_charge")));

    public static final RegistrySupplier<SoundEvent> ATOMIC_BLAST = SOUND_EVENTS.register("atomic_blast",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "atomic_blast")));

    public static final RegistrySupplier<SoundEvent> SUPERNOVA_SEQUENCE = SOUND_EVENTS.register("supernova_sequence",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WeaponsMod.MOD_ID, "supernova_sequence")));

    public static void register() {
        SOUND_EVENTS.register();
    }
}

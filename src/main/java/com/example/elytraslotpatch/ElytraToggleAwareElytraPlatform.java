package com.example.elytraslotpatch;

import com.illusivesoulworks.elytraslot.platform.NeoForgeElytraPlatform;
import com.illusivesoulworks.elytraslot.platform.services.IElytraPlatform;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Drop-in replacement for Elytra Slot's own NeoForgeElytraPlatform.
 *
 * Elytra Slot finds its platform implementation through java.util.ServiceLoader (see
 * META-INF/services in its jar) and only ever talks to it through the IElytraPlatform
 * interface - never the concrete class directly. That means swapping which class gets
 * discovered there is enough to change its behavior without touching a single line of its
 * original, compiled code.
 *
 * Everything here delegates straight through to the real implementation, except the two
 * "is there an elytra" checks, which first look up whether the Elytra Toggle mod
 * (elytratoggle) has flight disabled for this player. That lookup is done purely by the
 * attachment's registry name (a plain string ID), so this patch has no compile-time
 * dependency on that mod at all: if it isn't installed, the attachment simply won't be found
 * and this behaves identically to the unmodified original.
 */
public final class ElytraToggleAwareElytraPlatform implements IElytraPlatform {

    private static final ResourceLocation ATTACHMENT_ID =
            ResourceLocation.fromNamespaceAndPath("elytratoggle", "elytra_flight_enabled");

    private final NeoForgeElytraPlatform delegate = new NeoForgeElytraPlatform();

    @Override
    public boolean isEquipped(LivingEntity livingEntity) {
        if (isFlightDisabledByToggle(livingEntity)) {
            return false;
        }
        return delegate.isEquipped(livingEntity);
    }

    @Override
    public ItemStack getEquipped(LivingEntity livingEntity) {
        if (isFlightDisabledByToggle(livingEntity)) {
            return ItemStack.EMPTY;
        }
        return delegate.getEquipped(livingEntity);
    }

    @Override
    public boolean canFly(ItemStack stack, LivingEntity livingEntity) {
        // Purely a property of the stack itself (durability etc.) - not affected by the
        // toggle, so this is left completely untouched.
        return delegate.canFly(stack, livingEntity);
    }

    @Override
    public void processSlots(LivingEntity livingEntity,
            BiFunction<ItemStack, Boolean, Boolean> processor) {
        // Used for things like firework-boost consumption and mending, unrelated to whether
        // flight is currently allowed to start - left untouched.
        delegate.processSlots(livingEntity, processor);
    }

    @SuppressWarnings("unchecked")
    private static boolean isFlightDisabledByToggle(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide() || !(livingEntity instanceof Player player)) {
            return false;
        }

        AttachmentType<?> attachmentType = NeoForgeRegistries.ATTACHMENT_TYPES.getValue(ATTACHMENT_ID);
        if (attachmentType == null) {
            // Elytra Toggle isn't installed - behave exactly like the original.
            return false;
        }

        boolean enabled = player.getData((AttachmentType<Boolean>) attachmentType);
        return !enabled;
    }
}

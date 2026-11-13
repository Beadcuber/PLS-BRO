package com.example.godmode;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GodMode implements ModInitializer {

    // Toggle these if you want to turn parts off.
    private static final boolean GIVE_ARMOR = true;
    private static final boolean GIVE_TOOLS = true;
    private static final boolean GIVE_ELYTRA = true;

    @Override
    public void onInitialize() {
        // 1) No fall damage (also covers ender pearl landings)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof Player && source.is(DamageTypeTags.IS_FALL)) {
                return false; // cancel the damage
            }
            return true;
        });

        // 2) Hunger never goes down
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var food = player.getFoodData();
                if (food.getFoodLevel() < 20) {
                    food.setFoodLevel(20);
                }
                food.setSaturation(20.0F);
            }
        });

        // 3) Starter gear on join and after every respawn
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            giveGear(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
            giveGear(newPlayer));
    }

    private static void giveGear(ServerPlayer player) {
        if (GIVE_ARMOR) {
            equipIfEmpty(player, EquipmentSlot.HEAD, Items.NETHERITE_HELMET);
            equipIfEmpty(player, EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE);
            equipIfEmpty(player, EquipmentSlot.LEGS, Items.NETHERITE_LEGGINGS);
            equipIfEmpty(player, EquipmentSlot.FEET, Items.NETHERITE_BOOTS);
        }
        if (GIVE_TOOLS) {
            giveIfMissing(player, Items.NETHERITE_SWORD);
            giveIfMissing(player, Items.NETHERITE_PICKAXE);
            giveIfMissing(player, Items.NETHERITE_AXE);
            giveIfMissing(player, Items.NETHERITE_SHOVEL);
            giveIfMissing(player, Items.NETHERITE_HOE);
        }
        if (GIVE_ELYTRA) {
            // Goes in the inventory (the chest slot is taken by the chestplate).
            giveIfMissing(player, Items.ELYTRA);
        }
    }

    private static void equipIfEmpty(ServerPlayer player, EquipmentSlot slot, Item item) {
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, new ItemStack(item));
        }
    }

    // Only gives the item if you don't already have one anywhere (inventory, armor or offhand),
    // so rejoining a world doesn't hand you duplicates.
    private static void giveIfMissing(ServerPlayer player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                return;
            }
        }
        inv.add(new ItemStack(item));
    }
}

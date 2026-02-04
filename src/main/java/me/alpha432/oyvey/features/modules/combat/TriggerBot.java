package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.Category;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import java.util.Random;

public class TriggerBot extends Module {
    private final Setting<Double> range = doub("Range", 3.1, 3.0, 3.5);
    private final Setting<Integer> minCps = intg("MinCPS", 9, 1, 20);
    private final Setting<Integer> maxCps = intg("MaxCPS", 13, 1, 20);
    private final Setting<Boolean> playersOnly = bool("PlayersOnly", true);
    private final Setting<Boolean> throughWalls = bool("ThroughWalls", false);
    private final Setting<Boolean> requireAttackKey = bool("RequireAttackKey", true);
    private final Setting<Boolean> swingHand = bool("SwingHand", true);

    private final Random random = new Random();
    private long lastHitMs = 0;

    public TriggerBot() {
        super("TriggerBot", "Auto-attacks entities in your crosshair. Ghost mode.", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (nullCheck()) return;

        // Ghost check: only fire if attack key held (looks legit)
        if (requireAttackKey.getValue() && !mc.options.attackKey.isPressed()) return;

        long currentTime = System.currentTimeMillis();
        int cps = minCps.getValue() + random.nextInt(maxCps.getValue() - minCps.getValue() + 1);
        long attackDelay = Math.max(1, 1000L / cps);
        if (currentTime - lastHitMs < attackDelay) return;

        // MC's built-in crosshair raycast — pure genius, zero overhead
        HitResult hitResult = mc.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult entityHit = (EntityHitResult) hitResult;
        Entity target = entityHit.getEntity();
        if (target == null || target == mc.player) return;

        // Distance + validity filter
        double targetDist = mc.player.distanceTo(target);
        if (targetDist > range.getValue() || !isValidTarget(target)) return;

        // Through-walls: custom raycast if enabled (mc.player.canSee() is block-aware)
        if (!throughWalls.getValue() && !mc.player.canSee(target)) return;

        // RIP the fucker
        mc.gameMode.attackEntity(mc.player, target);
        if (swingHand.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        lastHitMs = currentTime;
    }

    private boolean isValidTarget(Entity entity) {
        if (playersOnly.getValue() && !(entity instanceof PlayerEntity)) return false;
        if (entity instanceof PlayerEntity player) {
            return !player.isDead() && player != mc.player && !player.isInvisible();  // Basic anti-dead/invis
            // Add friends check if OyVey has FriendUtil: && !OyVey.friendManager.isFriend(player.getName().getString())
        }
        return entity.isAlive();
    }

    @Override
    public void onDisable() {
        lastHitMs = 0;  // Reset timer
    }
}

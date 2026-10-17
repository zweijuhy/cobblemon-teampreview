package com.teampreview;

import com.teampreview.config.TeamPreviewConfig;
import com.teampreview.network.RequestBattlePayload;
import com.teampreview.network.TeamPreviewPayload;
import com.teampreview.rct.RctBridge;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamPreviewMod implements ModInitializer {
    public static final String MOD_ID = "cobblemon_teampreview";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final double MAX_BATTLE_REQUEST_DISTANCE_SQ = 64.0 * 64.0;

    @Override
    public void onInitialize() {
        TeamPreviewConfig.load();

        PayloadTypeRegistry.playS2C().register(TeamPreviewPayload.ID, TeamPreviewPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestBattlePayload.ID, RequestBattlePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestBattlePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                Entity entity = player.getServerWorld().getEntityById(payload.entityId());
                if (!(entity instanceof TrainerMob trainer)) {
                    return;
                }
                if (player.squaredDistanceTo(trainer) > MAX_BATTLE_REQUEST_DISTANCE_SQ) {
                    return;
                }
                trainer.startBattleWith(player);
            });
        });

        UseEntityCallback.EVENT.register(this::onUseEntity);
    }

    private ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (world.isClient() || !TeamPreviewConfig.get().enabled) {
            return ActionResult.PASS;
        }
        if (!(entity instanceof TrainerMob trainer) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        // Trainers that cannot be battled right now (already defeated, level cap, cooldown) keep
        // their vanilla interaction so RCT can explain why.
        if (!trainer.canBattleAgainst(player)) {
            return ActionResult.PASS;
        }

        TeamPreviewPayload payload = RctBridge.buildPreview(trainer);
        if (payload == null) {
            return ActionResult.PASS;
        }

        ServerPlayNetworking.send(serverPlayer, payload);
        return ActionResult.SUCCESS;
    }
}

package com.teampreview.mixin;

import com.teampreview.config.TeamPreviewConfig;
import com.gitlab.srcmc.rctmod.world.entities.goals.ForceIntoBattleGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses Radical Cobblemon Trainers' "force battle on sight" goal when the player asked for it,
 * so trainers never start a battle before the preview has been confirmed.
 */
@Mixin(ForceIntoBattleGoal.class)
public class ForceIntoBattleGoalMixin {
    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true, require = 0)
    private void cobblemon_teampreview$blockAutoBattle(CallbackInfoReturnable<Boolean> cir) {
        TeamPreviewConfig config = TeamPreviewConfig.get();
        if (config.enabled && config.blockAutoBattleOnLook) {
            cir.setReturnValue(false);
        }
    }
}

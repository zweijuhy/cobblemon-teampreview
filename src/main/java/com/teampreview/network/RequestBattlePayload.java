package com.teampreview.network;

import com.teampreview.TeamPreviewMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: the player confirmed the preview and wants to start the battle. */
public record RequestBattlePayload(int entityId) implements CustomPayload {
    public static final CustomPayload.Id<RequestBattlePayload> ID =
            new CustomPayload.Id<>(Identifier.of(TeamPreviewMod.MOD_ID, "request_battle"));

    public static final PacketCodec<RegistryByteBuf, RequestBattlePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, RequestBattlePayload::entityId, RequestBattlePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

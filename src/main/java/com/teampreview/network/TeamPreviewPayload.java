package com.teampreview.network;

import com.teampreview.TeamPreviewMod;
import com.teampreview.dto.TeamPreviewData;
import com.google.gson.Gson;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server -> client: the trainer team snapshot used to populate the preview screen. */
public record TeamPreviewPayload(String json) implements CustomPayload {
    public static final CustomPayload.Id<TeamPreviewPayload> ID =
            new CustomPayload.Id<>(Identifier.of(TeamPreviewMod.MOD_ID, "team_preview"));

    public static final PacketCodec<RegistryByteBuf, TeamPreviewPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, TeamPreviewPayload::json, TeamPreviewPayload::new);

    private static final Gson GSON = new Gson();

    public static TeamPreviewPayload of(TeamPreviewData data) {
        return new TeamPreviewPayload(GSON.toJson(data));
    }

    public TeamPreviewData parse() {
        return GSON.fromJson(json, TeamPreviewData.class);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

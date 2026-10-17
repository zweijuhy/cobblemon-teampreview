package com.teampreview.dto;

import java.util.ArrayList;
import java.util.List;

/** Serialized snapshot of a trainer's team, sent from the server to the client. */
public class TeamPreviewData {
    public int entityId;
    public String trainerName = "";
    public String battleFormat = "GEN_9_SINGLES";
    public List<PokemonDto> team = new ArrayList<>();
}

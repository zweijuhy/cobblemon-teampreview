package com.teampreview.rct;

import com.teampreview.dto.PokemonDto;
import com.teampreview.dto.TeamPreviewData;
import com.teampreview.network.TeamPreviewPayload;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Species;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData;
import com.gitlab.srcmc.rctmod.api.data.pack.TrainerTeam;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;

import java.util.List;

/** Reads a trainer's team out of Radical Cobblemon Trainers and converts it to a network snapshot. */
public final class RctBridge {
    private RctBridge() {
    }

    public static TeamPreviewPayload buildPreview(TrainerMob trainer) {
        TrainerMobData data = RCTMod.getInstance().getTrainerManager().getData(trainer);
        if (data == null) {
            return null;
        }
        TrainerTeam team = data.getTrainerTeam();
        if (team == null || team.getTeam() == null || team.getTeam().isEmpty()) {
            return null;
        }

        TeamPreviewData preview = new TeamPreviewData();
        preview.entityId = trainer.getId();
        preview.trainerName = resolveTrainerName(trainer, team);
        if (team.getBattleFormat() != null) {
            preview.battleFormat = team.getBattleFormat().name();
        }

        List<PokemonModel> models = team.getTeam();
        for (PokemonModel model : models) {
            if (model == null) {
                continue;
            }
            preview.team.add(toDto(model));
        }

        if (preview.team.isEmpty()) {
            return null;
        }
        return TeamPreviewPayload.of(preview);
    }

    private static PokemonDto toDto(PokemonModel model) {
        PokemonDto dto = new PokemonDto();
        dto.species = model.getSpecies() == null ? "" : model.getSpecies();
        dto.level = model.getLevel();
        dto.gender = model.getGender() == null ? "" : model.getGender();
        dto.nature = model.getNature() == null ? "" : model.getNature();
        dto.ability = model.getAbility() == null ? "" : model.getAbility();
        dto.shiny = model.isShiny();

        if (model.getMoveset() != null) {
            for (String move : model.getMoveset()) {
                if (move == null || move.isEmpty()) {
                    continue;
                }
                dto.moves.add(move);
                dto.moveTypes.add(resolveMoveType(move));
                dto.moveCategories.add(resolveMoveCategory(move));
            }
        }
        dto.types.addAll(resolveTypes(dto.species));
        String[] held = model.getHeldItems();
        if (held != null) {
            for (String item : held) {
                if (item != null && !item.isEmpty()) {
                    dto.heldItems.add(item);
                }
            }
        }
        dto.ivs = stats(model.getIVs());
        dto.evs = stats(model.getEVs());
        return dto;
    }

    private static String resolveTrainerName(TrainerMob trainer, TrainerTeam team) {
        try {
            com.gitlab.srcmc.rctapi.api.util.Text name = team.getName();
            if (name != null && !name.isEmpty()) {
                return name.getComponent().getString();
            }
        } catch (RuntimeException ignored) {
        }
        return trainer.getName().getString();
    }

    private static List<String> resolveTypes(String speciesName) {
        if (speciesName == null || speciesName.isEmpty()) {
            return List.of();
        }
        try {
            Species species = PokemonSpecies.getByName(speciesName);
            if (species == null) {
                return List.of();
            }
            List<String> types = new java.util.ArrayList<>(2);
            for (ElementalType type : species.getTypes()) {
                if (type != null) {
                    types.add(type.getName());
                }
            }
            return types;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    /** Resolves a move's elemental type id; empty string when the move is unknown. */
    private static String resolveMoveType(String moveName) {
        if (moveName == null || moveName.isEmpty()) {
            return "";
        }
        try {
            MoveTemplate template = Moves.getByName(moveName);
            if (template == null) {
                template = Moves.getByName(moveName.toLowerCase());
            }
            if (template == null || template.getElementalType() == null) {
                return "";
            }
            return template.getElementalType().getName();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    /** Resolves a move's damage category id (physical/special/status); empty string when unknown. */
    private static String resolveMoveCategory(String moveName) {
        if (moveName == null || moveName.isEmpty()) {
            return "";
        }
        try {
            MoveTemplate template = Moves.getByName(moveName);
            if (template == null) {
                template = Moves.getByName(moveName.toLowerCase());
            }
            if (template == null || template.getDamageCategory() == null) {
                return "";
            }
            return template.getDamageCategory().getName();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int[] stats(PokemonModel.StatsModel stats) {
        if (stats == null) {
            return new int[6];
        }
        return new int[]{
                stats.getHP(),
                stats.getAtk(),
                stats.getDef(),
                stats.getSpA(),
                stats.getSpD(),
                stats.getSpe()
        };
    }
}

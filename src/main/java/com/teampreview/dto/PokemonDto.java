package com.teampreview.dto;

import java.util.ArrayList;
import java.util.List;

/** Serialized, mapping-agnostic snapshot of one Pokemon in a trainer's team. */
public class PokemonDto {
    public String species = "";
    public int level;
    public String gender = "";
    public String nature = "";
    public String ability = "";
    public boolean shiny;
    public List<String> types = new ArrayList<>();
    public List<String> moves = new ArrayList<>();
    /** Elemental type of each move, index-aligned with {@link #moves}. */
    public List<String> moveTypes = new ArrayList<>();
    /** Damage category of each move (physical/special/status), index-aligned with {@link #moves}. */
    public List<String> moveCategories = new ArrayList<>();
    public List<String> heldItems = new ArrayList<>();
    public int[] ivs = new int[6];
    public int[] evs = new int[6];
}

package com.teampreview.client.gui;

import com.teampreview.TeamPreviewMod;
import com.teampreview.config.TeamPreviewConfig;
import com.teampreview.dto.PokemonDto;
import com.teampreview.dto.TeamPreviewData;
import com.teampreview.network.RequestBattlePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cobblemon-inspired preview of a trainer's team. Shown before a battle starts so the player can
 * decide whether to engage. Only the Continue button triggers the original battle flow.
 *
 * <p>Each Pokemon is a collapsible block: the header (species, level, gender, shiny, types, held
 * item) and the stat row are always visible, while the move grid is revealed by clicking the block.
 * Every move occupies its own bordered cell so the two cells of a row stay clearly separated.
 *
 * <p>The layout is authored in "base" units that match one vanilla text line, so at the usual 1x
 * upscale the panel is a compact dialog instead of a wall of oversized text. {@link #computeScale()}
 * only raises the whole panel by a whole number, and only while the collapsed team still fits the
 * window in one go, which keeps Minecraft's bitmap font crisp at every supported size.
 */
public class TeamPreviewScreen extends Screen {
    /** Bounds for the integer upscale of the whole panel; text stays crisp at whole multiples. */
    private static final int MIN_SCALE = 1;
    private static final int MAX_SCALE = 2;

    /** Integer upscale for the whole panel; recomputed from the window size in {@link #init()}. */
    private int scale = MIN_SCALE;

    // Layout constants are expressed in "base" units and multiplied by {@link #scale} on screen.
    private static final int PANEL_MAX_W = 420;
    private static final int HEADER_H = 24;
    private static final int FOOTER_H = 28;
    /** Margin around the scrolling list inside the panel. */
    private static final int PANEL_PAD = 4;

    private static final int MON_GAP = 3;
    private static final int MON_BORDER = 1;
    private static final int MON_HEADER_H = 22;

    private static final int MOVES_PAD = 3;
    /** Gap between the two columns of a move row; the dark grid behind it separates the cells. */
    private static final int MOVES_GAP = 4;
    private static final int CELL_H = 22;

    private static final int META_LINE_H = 10;
    private static final int META_PAD_TOP = 3;
    private static final int META_PAD_BOTTOM = 3;
    private static final int META_PAD_X = 6;
    private static final int META_GAP = 7;
    private static final int META_LABEL_GAP = 3;

    // Cobblemon sprites, referenced straight from the "cobblemon" namespace so no assets are copied.
    // Every on-screen box is a whole multiple or divisor of the source, which keeps it sharp.
    private static final Identifier TEX_TYPES = cobblemon("textures/gui/types.png");
    private static final int TYPES_ATLAS_W = 648;
    private static final int TYPES_ATLAS_H = 36;
    private static final int TYPE_SRC = 36;
    /** On-screen box of a header type sprite; the 36px tile is shrunk to it by {@link #blitSprite}. */
    private static final int TYPE_BADGE = 12;

    private static final Identifier TEX_CATEGORIES = cobblemon("textures/gui/categories.png");
    private static final int CAT_ATLAS_W = 24;
    private static final int CAT_ATLAS_H = 48;
    private static final int CAT_W = 24;
    private static final int CAT_H = 16;
    private static final int CAT_STEP = 16;

    private static final Identifier TEX_GENDER_MALE = cobblemon("textures/gui/pc/gender_icon_male.png");
    private static final Identifier TEX_GENDER_FEMALE = cobblemon("textures/gui/pc/gender_icon_female.png");
    private static final int GENDER_SRC_W = 6;
    private static final int GENDER_SRC_H = 8;
    private static final int GENDER_W = 12;
    private static final int GENDER_H = 16;

    private static final Identifier TEX_SHINY = cobblemon("textures/gui/summary/icon_shiny.png");
    private static final int SHINY_SRC = 16;

    private static final Identifier TEX_HELD = cobblemon("textures/gui/summary/icon_item_held.png");
    private static final int HELD_SRC_W = 12;
    private static final int HELD_SRC_H = 24;
    private static final int HELD_W = 6;
    private static final int HELD_H = 12;

    // Warm grey/brown palette. Backgrounds are fully opaque so the blurred world cannot bleed in.
    private static final int COL_PANEL = 0xFF1E1C19;
    private static final int COL_PANEL_EDGE = 0xFF3C3832;
    private static final int COL_HEADER = 0xFF2F2B25;
    private static final int COL_ACCENT = 0xFFB5793A;
    private static final int COL_ACCENT_HOVER = 0xFFC98B4A;
    private static final int COL_MON = 0xFF262320;
    private static final int COL_MON_HOVER = 0xFF2E2A26;
    private static final int COL_MON_EDGE = 0xFF463F38;
    private static final int COL_MON_TOP = 0xFF302C27;
    private static final int COL_CELL = 0xFF131211;
    private static final int COL_CELL_EDGE = 0xFF574F45;
    private static final int COL_BAND = 0xFF6B655E;
    private static final int COL_CHANNEL = 0xFF100F0E;
    private static final int COL_TEXT = 0xFFE2DED6;
    private static final int COL_TEXT_DIM = 0xFFB3ADA2;
    private static final int COL_TEXT_MUTE = 0xFF8D877D;
    private static final int COL_NAME = 0xFFFFFFFF;
    private static final int COL_NAME_SHINY = 0xFFE0B25C;
    private static final int COL_META_LABEL = 0xFFCFC7BA;
    private static final int COL_ITEM = 0xFFC9A26A;
    private static final int COL_BAND_TEXT = 0xFFFFFFFF;
    private static final int COL_BTN_PRIMARY_TEXT = 0xFF20160A;
    private static final int COL_BTN_GHOST = 0xFF33302B;
    private static final int COL_BTN_GHOST_HOVER = 0xFF3E3A34;
    private static final int COL_TRACK = 0xFF3A352E;

    private final TeamPreviewData data;
    private final TeamPreviewConfig config = TeamPreviewConfig.get();

    /** Indices of the blocks whose move grid is currently revealed. */
    private final Set<Integer> expanded = new HashSet<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentW;
    private int scroll;
    private int maxScroll;

    /** Per-block vertical offset (content space) and height, rebuilt by {@link #relayout(boolean)}. */
    private int[] blockTop = new int[0];
    private int[] blockH = new int[0];

    private int btnW;
    private int btnH;
    private int btnY;
    private int btnX1;
    private int btnX2;

    public TeamPreviewScreen(TeamPreviewData data) {
        super(Text.translatable("gui." + TeamPreviewMod.MOD_ID + ".preview.title"));
        this.data = data;
    }

    private static Identifier cobblemon(String path) {
        return Identifier.of("cobblemon", path);
    }

    @Override
    protected void init() {
        scale = computeScale();
        relayout(true);
    }

    /** Height of the whole panel with every block collapsed, in base units. */
    private int collapsedBaseHeight() {
        int n = Math.max(1, data.team.size());
        int block = MON_BORDER * 2 + MON_HEADER_H + META_PAD_TOP + META_LINE_H + META_PAD_BOTTOM;
        return HEADER_H + FOOTER_H + PANEL_PAD * 2 + n * block + (n - 1) * MON_GAP;
    }

    /**
     * Picks the largest whole upscale that still leaves the panel compact: at most 70% of the window
     * width so it never fills the screen, and short enough that a collapsed team fits without
     * scrolling. A whole multiple keeps the bitmap font and the sprites crisp, and falling back to
     * 1x means the text is drawn at the same size as every other vanilla screen.
     */
    private int computeScale() {
        int base = collapsedBaseHeight();
        for (int candidate = MAX_SCALE; candidate > MIN_SCALE; candidate--) {
            if (PANEL_MAX_W * candidate <= this.width * 7 / 10
                    && base * candidate <= this.height * 4 / 5) {
                return candidate;
            }
        }
        return MIN_SCALE;
    }

    /**
     * Recomputes the panel and block geometry. The panel follows the current total block height so
     * expanding a block grows the panel instead of pushing content out of the footer; anything that
     * still exceeds the window falls back to scrolling.
     */
    private void relayout(boolean resetScroll) {
        int screenW = this.width / scale;
        int screenH = this.height / scale;
        int n = data.team.size();

        panelW = Math.min(PANEL_MAX_W, screenW - 20);
        panelX = (screenW - panelW) / 2;
        contentX = panelX + PANEL_PAD;
        contentW = panelW - PANEL_PAD * 2;

        blockTop = new int[n];
        blockH = new int[n];
        int cursor = PANEL_PAD;
        for (int i = 0; i < n; i++) {
            blockTop[i] = cursor;
            blockH[i] = computeBlockH(i);
            cursor += blockH[i] + MON_GAP;
        }
        int totalContent = n == 0 ? PANEL_PAD * 2 : cursor - MON_GAP + PANEL_PAD;

        panelH = Math.min(HEADER_H + FOOTER_H + totalContent, screenH - 20);
        panelY = (screenH - panelH) / 2;
        int viewH = Math.max(0, panelH - HEADER_H - FOOTER_H);
        maxScroll = Math.max(0, totalContent - viewH);
        scroll = resetScroll ? 0 : Math.max(0, Math.min(maxScroll, scroll));

        btnH = 18;
        btnW = (panelW - 20) / 2;
        btnY = panelH - FOOTER_H + 5;
        btnX1 = 6;
        btnX2 = 6 + btnW + 8;
    }

    private int computeBlockH(int index) {
        PokemonDto p = data.team.get(index);
        int h = MON_BORDER * 2 + MON_HEADER_H;
        if (expanded.contains(index)) {
            h += gridHeight(p);
        }
        h += metaHeight(p);
        return h;
    }

    private int metaHeight(PokemonDto p) {
        int lines = wrapMeta(metaItems(p), contentW - MON_BORDER * 2 - META_PAD_X * 2).size();
        return META_PAD_TOP + lines * META_LINE_H + META_PAD_BOTTOM;
    }

    private static int moveRows(PokemonDto p) {
        return (p.moves.size() + 1) / 2;
    }

    private static int gridHeight(PokemonDto p) {
        int rows = moveRows(p);
        if (rows == 0) {
            return 0;
        }
        return MOVES_PAD * 2 + rows * CELL_H + (rows - 1) * MOVES_GAP;
    }

    private void toggle(int index) {
        if (!expanded.remove(index)) {
            expanded.add(index);
        }
        relayout(false);
    }

    private void continueBattle() {
        ClientPlayNetworking.send(new RequestBattlePayload(data.entityId));
        this.close();
    }

    /** Meta row entries, honouring the display toggles. */
    private List<MetaItem> metaItems(PokemonDto p) {
        List<MetaItem> items = new ArrayList<>(4);
        if (config.showAbility && !p.ability.isEmpty()) {
            items.add(new MetaItem(label("ability"), abilityName(p.ability)));
        }
        if (config.showNature && !p.nature.isEmpty()) {
            items.add(new MetaItem(label("nature"), natureName(p.nature)));
        }
        if (config.showIvs) {
            items.add(new MetaItem(label("ivs"), joinStats(p.ivs)));
        }
        if (config.showEvs) {
            items.add(new MetaItem(label("evs"), joinStats(p.evs)));
        }
        return items;
    }

    /** Splits the meta entries into as many lines as the available width requires. */
    private List<List<MetaItem>> wrapMeta(List<MetaItem> items, int maxWidth) {
        List<List<MetaItem>> lines = new ArrayList<>();
        List<MetaItem> current = new ArrayList<>();
        int width = 0;
        for (MetaItem item : items) {
            int itemWidth = this.textRenderer.getWidth(item.label) + META_LABEL_GAP
                    + this.textRenderer.getWidth(item.value);
            if (!current.isEmpty() && width + META_GAP + itemWidth > maxWidth) {
                lines.add(current);
                current = new ArrayList<>();
                width = 0;
            }
            if (!current.isEmpty()) {
                width += META_GAP;
            }
            current.add(item);
            width += itemWidth;
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        if (lines.isEmpty()) {
            lines.add(new ArrayList<>());
        }
        return lines;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        int baseMouseX = (int) (mouseX / (double) scale);
        int baseMouseY = (int) (mouseY / (double) scale);

        int contentTop = panelY + HEADER_H;
        int contentBottom = panelY + panelH - FOOTER_H;
        var matrices = context.getMatrices();

        // The panel body is drawn without a scissor so its border is never clipped.
        matrices.push();
        matrices.scale(scale, scale, 1.0f);
        context.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COL_PANEL_EDGE);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_PANEL);
        matrices.pop();

        // Blocks are clipped to the content area. The scissor rectangle is expressed in GUI
        // coordinates, so it is set while the matrix is still identity to keep the two in sync.
        context.enableScissor(panelX * scale, contentTop * scale,
                (panelX + panelW) * scale, contentBottom * scale);
        matrices.push();
        matrices.scale(scale, scale, 1.0f);
        for (int i = 0; i < data.team.size(); i++) {
            int blockY = contentTop + blockTop[i] - scroll;
            if (blockY + blockH[i] < contentTop || blockY > contentBottom) {
                continue;
            }
            boolean hovered = baseMouseX >= contentX && baseMouseX <= contentX + contentW
                    && baseMouseY >= blockY && baseMouseY <= blockY + blockH[i];
            renderMon(context, i, contentX, blockY, contentW, hovered);
        }
        matrices.pop();
        context.disableScissor();

        matrices.push();
        matrices.scale(scale, scale, 1.0f);
        context.fill(panelX, panelY, panelX + panelW, panelY + HEADER_H, COL_HEADER);
        context.fill(panelX, panelY + HEADER_H - 2, panelX + panelW, panelY + HEADER_H, COL_ACCENT);
        context.fill(panelX, contentBottom, panelX + panelW, contentBottom + 1, COL_PANEL_EDGE);

        String title = data.trainerName.isEmpty()
                ? this.title.getString()
                : data.trainerName;
        context.drawTextWithShadow(this.textRenderer, Text.literal(title), panelX + 8, panelY + 4, COL_NAME);
        context.drawText(this.textRenderer, Text.literal(formatLabel()), panelX + 8, panelY + 14, COL_TEXT_MUTE, false);

        if (maxScroll > 0) {
            renderScrollbar(context, contentTop, contentBottom);
        }

        boolean battleHover = inButton(baseMouseX, baseMouseY, panelX + btnX1, panelY + btnY, btnW, btnH);
        boolean cancelHover = inButton(baseMouseX, baseMouseY, panelX + btnX2, panelY + btnY, btnW, btnH);
        renderButton(context, panelX + btnX1, panelY + btnY, btnW, btnH,
                Text.translatable("gui." + TeamPreviewMod.MOD_ID + ".battle").getString(), true, battleHover);
        renderButton(context, panelX + btnX2, panelY + btnY, btnW, btnH,
                Text.translatable("gui." + TeamPreviewMod.MOD_ID + ".cancel").getString(), false, cancelHover);

        matrices.pop();
    }

    private void renderMon(DrawContext context, int index, int x, int y, int w, boolean hovered) {
        PokemonDto p = data.team.get(index);
        int bottom = y + blockH[index];

        context.fill(x, y, x + w, bottom, hovered ? COL_MON_HOVER : COL_MON);
        context.fill(x, y, x + w, y + MON_BORDER, COL_MON_EDGE);
        context.fill(x, bottom - MON_BORDER, x + w, bottom, COL_MON_EDGE);
        context.fill(x, y, x + MON_BORDER, bottom, COL_MON_EDGE);
        context.fill(x + w - MON_BORDER, y, x + w, bottom, COL_MON_EDGE);

        int innerX = x + MON_BORDER;
        int innerW = w - MON_BORDER * 2;
        int cursorY = y + MON_BORDER;

        renderMonHeader(context, p, index, innerX, cursorY, innerW);
        cursorY += MON_HEADER_H;

        if (expanded.contains(index)) {
            int gridH = gridHeight(p);
            if (gridH > 0) {
                renderMoves(context, p, innerX, cursorY, innerW, gridH);
                cursorY += gridH;
            }
        }

        int metaH = bottom - MON_BORDER - cursorY;
        if (metaH > 0) {
            renderMeta(context, p, innerX, cursorY, innerW, metaH);
        }
    }

    /** Name row: species, level, gender/shiny markers, type icons and the right-aligned held item. */
    private void renderMonHeader(DrawContext context, PokemonDto p, int index, int x, int y, int w) {
        context.fill(x, y, x + w, y + MON_HEADER_H, COL_MON_TOP);
        context.fill(x, y + MON_HEADER_H - 1, x + w, y + MON_HEADER_H, COL_MON_EDGE);

        int textY = y + (MON_HEADER_H - 8) / 2;
        int cursor = x + 6;

        String name = speciesName(p);
        context.drawTextWithShadow(this.textRenderer, Text.literal(name), cursor, textY,
                p.shiny ? COL_NAME_SHINY : COL_NAME);
        cursor += this.textRenderer.getWidth(name) + 4;

        String level = "Lv." + p.level;
        context.drawText(this.textRenderer, Text.literal(level), cursor, textY, COL_TEXT_MUTE, false);
        cursor += this.textRenderer.getWidth(level) + 4;

        Identifier gender = genderTexture(p.gender);
        if (gender != null) {
            blitSprite(context, gender, cursor, y + (MON_HEADER_H - GENDER_H) / 2, 0f, 0f,
                    GENDER_SRC_W, GENDER_SRC_H, GENDER_SRC_W, GENDER_SRC_H, GENDER_W, GENDER_H);
            cursor += GENDER_W + 3;
        }
        if (p.shiny) {
            context.drawTexture(TEX_SHINY, cursor, y + (MON_HEADER_H - SHINY_SRC) / 2, 0f, 0f,
                    SHINY_SRC, SHINY_SRC, SHINY_SRC, SHINY_SRC);
            cursor += SHINY_SRC + 3;
        }
        for (String type : p.types) {
            drawTypeIcon(context, type, cursor, y + (MON_HEADER_H - TYPE_BADGE) / 2, TYPE_BADGE);
            cursor += TYPE_BADGE + 2;
        }

        int right = x + w - 6;
        if (!p.moves.isEmpty()) {
            String chevron = expanded.contains(index) ? "\u25BC" : "\u25B6";
            int chevronX = right - this.textRenderer.getWidth(chevron);
            context.drawText(this.textRenderer, Text.literal(chevron), chevronX, textY, COL_TEXT_MUTE, false);
            right = chevronX - 6;
        }
        if (config.showHeldItem && !p.heldItems.isEmpty()) {
            String held = itemName(p.heldItems.get(0));
            int heldX = right - this.textRenderer.getWidth(held);
            context.drawText(this.textRenderer, Text.literal(held), heldX, textY, COL_ITEM, false);
            blitSprite(context, TEX_HELD, heldX - 3 - HELD_W, y + (MON_HEADER_H - HELD_H) / 2, 0f, 0f,
                    HELD_SRC_W, HELD_SRC_H, HELD_SRC_W, HELD_SRC_H, HELD_W, HELD_H);
        }
    }

    /**
     * Move grid: two cells per row, drawn on the darker channel background. Each cell paints its own
     * border and fill, so the channel shows through as a groove and keeps the two clearly apart.
     */
    private void renderMoves(DrawContext context, PokemonDto p, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, COL_CHANNEL);
        int cellW = (w - MOVES_PAD * 2 - MOVES_GAP) / 2;
        for (int i = 0; i < p.moves.size(); i++) {
            int cellX = x + MOVES_PAD + (i % 2) * (cellW + MOVES_GAP);
            int cellY = y + MOVES_PAD + (i / 2) * (CELL_H + MOVES_GAP);
            renderCell(context, p, i, cellX, cellY, cellW);
        }
    }

    /**
     * One move cell: the type sprite on the left, the move name in the middle band and the damage
     * category on the right. The type sprite uses the same box as the header one so both rows read
     * alike.
     */
    private void renderCell(DrawContext context, PokemonDto p, int moveIndex, int x, int y, int w) {
        context.fill(x, y, x + w, y + CELL_H, COL_CELL_EDGE);
        context.fill(x + 1, y + 1, x + w - 1, y + CELL_H - 1, COL_CELL);

        int cursor = x + 3;
        String typeId = moveIndex < p.moveTypes.size() ? p.moveTypes.get(moveIndex) : "";
        if (typeIndex(typeId) >= 0) {
            drawTypeIcon(context, typeId, cursor, y + (CELL_H - TYPE_BADGE) / 2, TYPE_BADGE);
            cursor += TYPE_BADGE + 4;
        }

        int bandRight = x + w - 3;
        int category = moveIndex < p.moveCategories.size()
                ? categoryRow(p.moveCategories.get(moveIndex))
                : -1;
        if (category >= 0) {
            int categoryX = bandRight - CAT_W;
            context.drawTexture(TEX_CATEGORIES, categoryX, y + (CELL_H - CAT_H) / 2,
                    0f, (float) (category * CAT_STEP), CAT_W, CAT_H, CAT_ATLAS_W, CAT_ATLAS_H);
            bandRight = categoryX - 4;
        }

        int available = bandRight - cursor;
        if (available <= 0) {
            return;
        }
        String move = moveName(p.moves.get(moveIndex));
        int bandW = Math.min(this.textRenderer.getWidth(move) + 10, available);
        int bandH = 11;
        int bandY = y + (CELL_H - bandH) / 2;
        context.fill(cursor, bandY, cursor + bandW, bandY + bandH, COL_BAND);
        String label = trim(move, bandW - 8);
        int labelW = this.textRenderer.getWidth(label);
        context.drawText(this.textRenderer, Text.literal(label),
                cursor + (bandW - labelW) / 2, bandY + 2, COL_BAND_TEXT, false);
    }

    /** Bottom row: ability, nature, IVs and EVs, wrapped onto extra lines when the panel is narrow. */
    private void renderMeta(DrawContext context, PokemonDto p, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, COL_MON);
        context.fill(x, y, x + w, y + 1, COL_MON_EDGE);

        List<List<MetaItem>> lines = wrapMeta(metaItems(p), w - META_PAD_X * 2);
        int textY = y + META_PAD_TOP;
        for (List<MetaItem> line : lines) {
            int cursor = x + META_PAD_X;
            for (MetaItem item : line) {
                context.drawText(this.textRenderer, Text.literal(item.label), cursor, textY, COL_META_LABEL, false);
                int labelW = this.textRenderer.getWidth(item.label);
                context.drawText(this.textRenderer, Text.literal(item.value), cursor + labelW + META_LABEL_GAP,
                        textY, COL_TEXT_DIM, false);
                cursor += labelW + META_LABEL_GAP + this.textRenderer.getWidth(item.value) + META_GAP;
            }
            textY += META_LINE_H;
        }
    }

    /** Header type sprite: the 36px tile of {@code types.png} shrunk into the {@code size} box. */
    private static void drawTypeIcon(DrawContext context, String typeId, int x, int y, int size) {
        int column = typeIndex(typeId);
        if (column < 0) {
            return;
        }
        blitSprite(context, TEX_TYPES, x, y, (float) (column * TYPE_SRC), 0f,
                TYPE_SRC, TYPE_SRC, TYPES_ATLAS_W, TYPES_ATLAS_H, size, size);
    }

    /**
     * Draws the {@code srcW x srcH} region at {@code (u, v)} of {@code texture} into a
     * {@code dstW x dstH} box on screen.
     *
     * <p>{@code DrawContext#drawTexture} feeds its width/height to both the sampled region and the
     * destination rectangle, so it cannot resize a sprite by itself; the box is resized through the
     * matrix stack instead. The region is always sampled at its native pixel size, which keeps the
     * sprites sharp.
     */
    private static void blitSprite(DrawContext context, Identifier texture, int x, int y,
                                   float u, float v, int srcW, int srcH, int atlasW, int atlasH,
                                   int dstW, int dstH) {
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0f);
        matrices.scale(dstW / (float) srcW, dstH / (float) srcH, 1f);
        context.drawTexture(texture, 0, 0, u, v, srcW, srcH, atlasW, atlasH);
        matrices.pop();
    }

    private void renderButton(DrawContext context, int x, int y, int w, int h, String label,
                             boolean primary, boolean hovered) {
        int bg;
        int textColor;
        if (primary) {
            bg = hovered ? COL_ACCENT_HOVER : COL_ACCENT;
            textColor = COL_BTN_PRIMARY_TEXT;
        } else {
            bg = hovered ? COL_BTN_GHOST_HOVER : COL_BTN_GHOST;
            textColor = COL_TEXT;
        }
        context.fill(x, y, x + w, y + h, bg);
        if (!primary) {
            context.fill(x, y, x + w, y + 1, COL_MON_EDGE);
            context.fill(x, y + h - 1, x + w, y + h, COL_MON_EDGE);
            context.fill(x, y, x + 1, y + h, COL_MON_EDGE);
            context.fill(x + w - 1, y, x + w, y + h, COL_MON_EDGE);
        }
        int textW = this.textRenderer.getWidth(label);
        context.drawText(this.textRenderer, Text.literal(label), x + (w - textW) / 2,
                y + (h - 8) / 2, textColor, false);
    }

    private String formatLabel() {
        String raw = data.battleFormat == null ? "" : data.battleFormat;
        if (raw.startsWith("GEN_9_")) {
            raw = raw.substring(6);
        }
        return prettify(raw.toLowerCase());
    }

    private void renderScrollbar(DrawContext context, int top, int bottom) {
        int trackX = panelX + panelW - 3;
        context.fill(trackX, top, trackX + 2, bottom, COL_TRACK);
        int view = bottom - top;
        int barH = Math.max(10, view * view / (view + maxScroll));
        int barY = top + (int) ((view - barH) * (scroll / (float) maxScroll));
        context.fill(trackX, barY, trackX + 2, barY + barH, COL_ACCENT);
    }

    private static boolean inButton(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static String joinStats(int[] stats) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stats.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(stats[i]);
        }
        return sb.toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int baseX = (int) (mouseX / scale);
        int baseY = (int) (mouseY / scale);
        if (inButton(baseX, baseY, panelX + btnX1, panelY + btnY, btnW, btnH)) {
            continueBattle();
            return true;
        }
        if (inButton(baseX, baseY, panelX + btnX2, panelY + btnY, btnW, btnH)) {
            this.close();
            return true;
        }

        int contentTop = panelY + HEADER_H;
        int contentBottom = panelY + panelH - FOOTER_H;
        if (baseX >= contentX && baseX <= contentX + contentW
                && baseY >= contentTop && baseY < contentBottom) {
            int local = baseY - contentTop + scroll;
            for (int i = 0; i < blockH.length; i++) {
                if (local >= blockTop[i] && local < blockTop[i] + blockH[i]) {
                    toggle(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scroll = (int) Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 8));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (this.textRenderer.getWidth(sb.toString() + c) > maxWidth - 6) {
                break;
            }
            sb.append(c);
        }
        return sb.append("...").toString();
    }

    private static Identifier genderTexture(String gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender.toUpperCase()) {
            case "MALE" -> TEX_GENDER_MALE;
            case "FEMALE" -> TEX_GENDER_FEMALE;
            default -> null;
        };
    }

    /** Sprite row inside categories.png; matches Cobblemon's own {@code DamageCategory} order. */
    private static int categoryRow(String category) {
        if (category == null) {
            return -1;
        }
        return switch (category.toLowerCase()) {
            case "physical" -> 0;
            case "special" -> 1;
            case "status" -> 2;
            default -> -1;
        };
    }

    /** Column inside types.png; matches Cobblemon's {@code ElementalType} order. */
    private static int typeIndex(String type) {
        if (type == null) {
            return -1;
        }
        return switch (type.toLowerCase()) {
            case "normal" -> 0;
            case "fire" -> 1;
            case "water" -> 2;
            case "grass" -> 3;
            case "electric" -> 4;
            case "ice" -> 5;
            case "fighting" -> 6;
            case "poison" -> 7;
            case "ground" -> 8;
            case "flying" -> 9;
            case "psychic" -> 10;
            case "bug" -> 11;
            case "rock" -> 12;
            case "ghost" -> 13;
            case "dragon" -> 14;
            case "dark" -> 15;
            case "steel" -> 16;
            case "fairy" -> 17;
            default -> -1;
        };
    }

    /** Resolves a Cobblemon translation key; falls back to a prettified id if the key is unknown. */
    private static String tr(String key, String rawFallback) {
        String resolved = Text.translatable(key).getString();
        return resolved.equals(key) ? prettify(rawFallback) : resolved;
    }

    private static String label(String path) {
        return Text.translatable("gui." + TeamPreviewMod.MOD_ID + "." + path).getString();
    }

    private static String speciesName(PokemonDto p) {
        return tr("cobblemon.species." + p.species.toLowerCase() + ".name", p.species);
    }

    private static String moveName(String id) {
        return tr("cobblemon.move." + id.toLowerCase(), id);
    }

    private static String abilityName(String id) {
        return tr("cobblemon.ability." + id.toLowerCase(), id);
    }

    private static String natureName(String id) {
        return tr("cobblemon.nature." + id.toLowerCase(), id);
    }

    private static String itemName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        Identifier id = Identifier.tryParse(raw.contains(":") ? raw : ("cobblemon:" + raw));
        if (id != null) {
            Item item = Registries.ITEM.get(id);
            if (item != null) {
                return item.getName().getString();
            }
        }
        return prettify(raw);
    }

    private static String prettify(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String[] parts = raw.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** One entry of the bottom stat row: a label drawn dimmer than its value. */
    private record MetaItem(String label, String value) {
    }
}

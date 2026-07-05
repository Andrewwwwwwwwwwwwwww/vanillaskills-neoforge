package io.github.andrewwwwwwwwwwwwwww.vanillaskills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.PointsConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui.SkillTreeMenu;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.PlayerSkillData;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillEffect;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillNode;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /skill         open the tree GUI (all players)
 * /skill points  show own points; (op) /skill points &lt;player&gt; add|set &lt;n&gt;
 * /skill reset|recalc &lt;player&gt;   (op)
 * /skill reload                    (op) reload tree + points config from disk
 * /skill mending on|off            (op) enable/remove Mending for this world (restart to apply)
 * /skill regen                     (op) overwrite skilltree.json with the built-in default (backs up the old one)
 * /skill edit ...                  (op) live-edit the server's skill tree
 */
public final class SkillCommands {
    private SkillCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("skill")
                .executes(SkillCommands::openSelf);

        root.then(Commands.literal("open").executes(SkillCommands::openSelf));

        root.then(Commands.literal("editor")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(SkillCommands::openEditor));

        root.then(Commands.literal("layout")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(SkillCommands::openLayout));

        root.then(Commands.literal("guide").executes(SkillCommands::guide));

        // Toggle unlocked toggleable skills on/off (per player, persisted): /skill toggle <skill>
        root.then(Commands.literal("toggle")
                .then(Commands.literal("nightvision").executes(SkillCommands::toggleNightVision))
                .then(Commands.literal("stepup").executes(SkillCommands::toggleStepUp)));

        root.then(Commands.literal("points")
                .executes(SkillCommands::showOwnPoints)
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> adjustPoints(ctx, true))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> adjustPoints(ctx, false))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetPoints(ctx, false)))));

        root.then(Commands.literal("questshards")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> adjustQuestShards(ctx, true))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> adjustQuestShards(ctx, false))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetPoints(ctx, true)))));

        root.then(Commands.literal("reset")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(SkillCommands::reset)));

        root.then(Commands.literal("recalc")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(SkillCommands::recalc)));

        root.then(Commands.literal("reload")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(SkillCommands::reload));

        root.then(Commands.literal("mending")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("on").executes(ctx -> setMending(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> setMending(ctx, false))));

        root.then(Commands.literal("regen")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> regen(ctx, true))
                .then(Commands.literal("fresh").executes(ctx -> regen(ctx, false))));

        root.then(Commands.literal("regenpoints")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(SkillCommands::regenPoints));

        root.then(editTree());

        dispatcher.register(root);
    }

    // ---- player-facing ----

    private static int openSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SkillTreeMenu.open(ctx.getSource().getPlayerOrException());
        return 1;
    }

    private static int openLayout(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SkillTreeMenu.openLayout(ctx.getSource().getPlayerOrException());
        return 1;
    }

    private static int openEditor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SkillTreeMenu.openEditor(ctx.getSource().getPlayerOrException());
        return 1;
    }

    private static int guide(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.book.GuideBook.open(ctx.getSource().getPlayerOrException());
        return 1;
    }

    private static int toggleNightVision(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        boolean unlocked = data != null && data.unlocked.stream().anyMatch(id -> id.startsWith("nightvision"));
        if (!unlocked) {
            ctx.getSource().sendFailure(Component.literal("You haven't unlocked Night Vision yet."));
            return 0;
        }
        data.nightVisionDisabled = !data.nightVisionDisabled;
        VanillaSkills.PLAYERS.save(player.getUUID());
        if (data.nightVisionDisabled) {
            player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Night Vision OFF — run /skill toggle nightvision again to turn it back on."), false);
        } else {
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillEffects
                    .refreshStatusEffects(player, data, VanillaSkills.TREE.tree()); // instant re-apply
            ctx.getSource().sendSuccess(() -> Component.literal("Night Vision ON."), false);
        }
        return 1;
    }

    private static int toggleStepUp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.StepHeight.hasStepSkill(data)) {
            ctx.getSource().sendFailure(Component.literal("You haven't unlocked the Mountaineer (step-up) skill yet."));
            return 0;
        }
        data.stepUpDisabled = !data.stepUpDisabled;
        VanillaSkills.PLAYERS.save(player.getUUID());
        // Force StepHeight to re-evaluate next tick (applies/removes the modifier).
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.StepHeight.invalidate(player.getUUID());
        if (data.stepUpDisabled) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Step-up OFF — you'll step like vanilla. Run /skill toggle stepup to re-enable. "
                    + "(Note: step-up is always off while you're sneaking.)"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Step-up ON — auto-step ledges (still off while sneaking)."), false);
        }
        return 1;
    }

    private static int showOwnPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Skill Shards: " + data.pointsAvailable + " (earned " + data.pointsEarned + ")"
                + "   Quest Shards: " + data.questShardsAvailable)
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    // ---- op: player management ----

    private static int adjustPoints(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        if (add) VanillaSkills.PLAYERS.addPoints(target, amount);
        else VanillaSkills.PLAYERS.setPoints(target, amount);
        PlayerSkillData data = VanillaSkills.PLAYERS.get(target.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getName().getString() + " now has " + data.pointsAvailable + " Skill Shards."), true);
        return 1;
    }

    private static int adjustQuestShards(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        if (add) VanillaSkills.PLAYERS.addQuestShards(target, amount);
        else VanillaSkills.PLAYERS.setQuestShards(target, amount);
        PlayerSkillData data = VanillaSkills.PLAYERS.get(target.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getName().getString() + " now has " + data.questShardsAvailable + " Quest Shards."), true);
        return 1;
    }

    private static int resetPoints(CommandContext<CommandSourceStack> ctx, boolean quest) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        if (quest) VanillaSkills.PLAYERS.setQuestShards(target, 0);
        else VanillaSkills.PLAYERS.setPoints(target, 0);
        String which = quest ? "Quest Shards" : "Skill Shards";
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getName().getString() + "'s " + which + " reset to 0."), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        VanillaSkills.PLAYERS.reset(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Reset skills for " + target.getName().getString() + "."), true);
        return 1;
    }

    private static int recalc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int delta = VanillaSkills.PLAYERS.recalc(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Recalculated Skill Shards for " + target.getName().getString() + " (" + (delta >= 0 ? "+" : "") + delta + ")."), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        PointsConfig points = PointsConfig.load();
        VanillaSkills.PLAYERS.setPointsConfig(points);
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.load();
        VanillaSkills.TREE.load();
        if (ctx.getSource().getServer() != null) {
            for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                VanillaSkills.PLAYERS.applyAll(player);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Reloaded skill tree (" + VanillaSkills.TREE.tree().size() + " nodes) and Skill Shard config.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setMending(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        // Load the current per-world config (keeps all other settings), flip Mending, and save it.
        // We intentionally do NOT apply it live — it takes effect on the next world load, which the
        // message tells the op to do, so the toggle is predictable and matches the on-disk state.
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig cfg =
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.load();
        cfg.mendingEnabled = enabled;
        cfg.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Mending set to " + (enabled ? "ENABLED" : "REMOVED")
                + " for this world. Restart the world/server for it to take effect."
                + (enabled ? " (Existing villager trades won't change — reroll librarians for new mending offers.)" : ""))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int regenPoints(CommandContext<CommandSourceStack> ctx) {
        PointsConfig cfg = PointsConfig.regenerate();
        VanillaSkills.PLAYERS.setPointsConfig(cfg);
        int p = VanillaSkills.PLAYERS.computeTotalEarnable();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Reset points.json to defaults (task " + cfg.valueTask + " / goal " + cfg.valueGoal
                + " / challenge " + cfg.valueChallenge + "). Total earnable = " + p
                + ". Run /skill recalc <player> to reprice, and /skill regen fresh to re-price the tree.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int regen(CommandContext<CommandSourceStack> ctx, boolean preserve) {
        java.nio.file.Path backup = VanillaSkills.TREE.regenerate(preserve);
        if (ctx.getSource().getServer() != null) {
            for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                VanillaSkills.PLAYERS.applyAll(player);
            }
        }
        String suffix = backup != null ? " Backed up the old tree to " + backup.getFileName() + "." : "";
        String mode = preserve
                ? "Updated the tree, keeping your changes (added any new lanes/nodes)"
                : "Reset the tree to the built-in default";
        ctx.getSource().sendSuccess(() -> Component.literal(
                mode + " (" + VanillaSkills.TREE.tree().size() + " nodes)." + suffix)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ---- op: tree editor ----

    private static LiteralArgumentBuilder<CommandSourceStack> editTree() {
        return Commands.literal("edit")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(SkillCommands::editList))
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("category", StringArgumentType.word())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("cost", IntegerArgumentType.integer(0))
                                                        .then(Commands.argument("icon", IdentifierArgument.id())
                                                                .executes(SkillCommands::editAdd)))))))
                .then(Commands.literal("category")
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("icon", IdentifierArgument.id())
                                                        .executes(SkillCommands::editCategoryAdd)))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(SkillCommands::editCategoryRemove)))
                        .then(Commands.literal("setslot")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .executes(SkillCommands::editCategorySetSlot))))
                        .then(Commands.literal("seticon")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("icon", IdentifierArgument.id())
                                                .executes(SkillCommands::editCategorySetIcon))))
                        .then(Commands.literal("settitle")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(SkillCommands::editCategorySetTitle)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(SkillCommands::editRemove)))
                .then(Commands.literal("setcost")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("cost", IntegerArgumentType.integer(0))
                                        .executes(SkillCommands::editSetCost))))
                .then(Commands.literal("setslot")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                        .executes(SkillCommands::editSetSlot))))
                .then(Commands.literal("seticon")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("icon", IdentifierArgument.id())
                                        .executes(SkillCommands::editSetIcon))))
                .then(Commands.literal("settitle")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(SkillCommands::editSetTitle))))
                .then(Commands.literal("setdesc")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(SkillCommands::editSetDesc))))
                .then(Commands.literal("require")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("req", StringArgumentType.word())
                                                .executes(ctx -> editRequire(ctx, true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("req", StringArgumentType.word())
                                                .executes(ctx -> editRequire(ctx, false))))))
                .then(Commands.literal("effect")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.literal("clear").executes(SkillCommands::editEffectClear))
                                .then(Commands.literal("attribute")
                                        .then(Commands.argument("attribute", IdentifierArgument.id())
                                                .then(Commands.argument("operation", StringArgumentType.word())
                                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                .executes(SkillCommands::editEffectAttribute)))))));
    }

    private static SkillNode requireNode(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        SkillNode node = VanillaSkills.TREE.tree().byId(id);
        if (node == null) ctx.getSource().sendFailure(Component.literal("No skill node with id '" + id + "'."));
        return node;
    }

    private static int editList(CommandContext<CommandSourceStack> ctx) {
        SkillTree tree = VanillaSkills.TREE.tree();
        ctx.getSource().sendSuccess(() -> Component.literal(
                tree.title + " — " + tree.size() + " nodes, " + tree.rows + " rows").withStyle(ChatFormatting.AQUA), false);
        for (SkillNode node : tree.nodes) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    " • " + node.id + "  slot=" + node.slot + " cost=" + node.cost
                            + " effects=" + node.effects.size() + " req=" + node.requires), false);
        }
        return 1;
    }

    private static int editAdd(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        if (VanillaSkills.TREE.tree().has(id)) {
            ctx.getSource().sendFailure(Component.literal("A node with id '" + id + "' already exists."));
            return 0;
        }
        String category = StringArgumentType.getString(ctx, "category");
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        int cost = IntegerArgumentType.getInteger(ctx, "cost");
        String icon = IdentifierArgument.getId(ctx, "icon").toString();
        SkillNode node = new SkillNode(id, id, category, slot, cost, icon);
        VanillaSkills.TREE.tree().nodes.add(node);
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Added node '" + id + "' to lane '" + category + "'.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ---- lane (category) editing ----

    private static io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory requireCategory(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory cat = VanillaSkills.TREE.tree().category(id);
        if (cat == null) ctx.getSource().sendFailure(Component.literal("No lane with id '" + id + "'."));
        return cat;
    }

    private static int editCategoryAdd(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        if (VanillaSkills.TREE.tree().category(id) != null) {
            ctx.getSource().sendFailure(Component.literal("A lane with id '" + id + "' already exists."));
            return 0;
        }
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        String icon = IdentifierArgument.getId(ctx, "icon").toString();
        VanillaSkills.TREE.tree().categories.add(
                new io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory(id, id, icon, slot));
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Added lane '" + id + "'.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int editCategoryRemove(CommandContext<CommandSourceStack> ctx) {
        var cat = requireCategory(ctx);
        if (cat == null) return 0;
        VanillaSkills.TREE.tree().categories.remove(cat);
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Removed lane '" + cat.id + "' (its skills move to the default lane).").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int editCategorySetSlot(CommandContext<CommandSourceStack> ctx) {
        var cat = requireCategory(ctx);
        if (cat == null) return 0;
        cat.slot = IntegerArgumentType.getInteger(ctx, "slot");
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set lane '" + cat.id + "' slot to " + cat.slot + "."), true);
        return 1;
    }

    private static int editCategorySetIcon(CommandContext<CommandSourceStack> ctx) {
        var cat = requireCategory(ctx);
        if (cat == null) return 0;
        cat.icon = IdentifierArgument.getId(ctx, "icon").toString();
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set lane '" + cat.id + "' icon to " + cat.icon + "."), true);
        return 1;
    }

    private static int editCategorySetTitle(CommandContext<CommandSourceStack> ctx) {
        var cat = requireCategory(ctx);
        if (cat == null) return 0;
        cat.title = StringArgumentType.getString(ctx, "text");
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set lane '" + cat.id + "' title."), true);
        return 1;
    }

    private static int editRemove(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        VanillaSkills.TREE.tree().nodes.remove(node);
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Removed node '" + node.id + "'.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int editSetCost(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        node.cost = IntegerArgumentType.getInteger(ctx, "cost");
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set cost of '" + node.id + "' to " + node.cost + "."), true);
        return 1;
    }

    private static int editSetSlot(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        node.slot = IntegerArgumentType.getInteger(ctx, "slot");
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set slot of '" + node.id + "' to " + node.slot + "."), true);
        return 1;
    }

    private static int editSetIcon(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        node.icon = IdentifierArgument.getId(ctx, "icon").toString();
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set icon of '" + node.id + "' to " + node.icon + "."), true);
        return 1;
    }

    private static int editSetTitle(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        node.title = StringArgumentType.getString(ctx, "text");
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set title of '" + node.id + "'."), true);
        return 1;
    }

    private static int editSetDesc(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        node.description.clear();
        node.description.add(StringArgumentType.getString(ctx, "text"));
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Set description of '" + node.id + "'."), true);
        return 1;
    }

    private static int editRequire(CommandContext<CommandSourceStack> ctx, boolean add) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        String req = StringArgumentType.getString(ctx, "req");
        if (add) {
            if (!node.requires.contains(req)) node.requires.add(req);
        } else {
            node.requires.remove(req);
        }
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal(
                (add ? "Added" : "Removed") + " requirement '" + req + "' " + (add ? "to" : "from") + " '" + node.id + "'."), true);
        return 1;
    }

    private static int editEffectClear(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        node.effects.clear();
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared effects of '" + node.id + "'."), true);
        return 1;
    }

    private static int editEffectAttribute(CommandContext<CommandSourceStack> ctx) {
        SkillNode node = requireNode(ctx);
        if (node == null) return 0;
        String attribute = IdentifierArgument.getId(ctx, "attribute").toString();
        String operation = StringArgumentType.getString(ctx, "operation");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        if (!operation.equals("add_value") && !operation.equals("add_multiplied_base") && !operation.equals("add_multiplied_total")) {
            ctx.getSource().sendFailure(Component.literal(
                    "Operation must be add_value, add_multiplied_base, or add_multiplied_total."));
            return 0;
        }
        node.effects.add(SkillEffect.attribute(attribute, operation, amount));
        VanillaSkills.TREE.touchAndSave();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Added attribute effect to '" + node.id + "': " + attribute + " " + operation + " " + amount).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}

package io.github.andrewwwwwwwwwwwwwww.vanillaskills.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /help — lists the player-facing commands VanillaSkills adds.
 * /help admin — (op) lists those plus the op/admin commands.
 */
public final class HelpCommand {
    private HelpCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("help")
                .executes(ctx -> { showPlayer(ctx.getSource()); return 1; })
                .then(Commands.literal("admin")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> { showAdmin(ctx.getSource()); return 1; })));
    }

    private static void showPlayer(CommandSourceStack source) {
        header(source, "VanillaSkills — Commands");
        line(source, "/skill", "open your skill tree");
        line(source, "/skill points", "view your Skill & Quest Shards");
        line(source, "/skill guide", "open the in-game guide book");
        line(source, "/quests", "open your bounty board (alias /bounty)");
        source.sendSystemMessage(Component.literal("Ops: /help admin for admin commands.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void showAdmin(CommandSourceStack source) {
        header(source, "VanillaSkills — Admin Commands");
        source.sendSystemMessage(Component.literal("Player commands:").withStyle(ChatFormatting.GRAY));
        line(source, "/skill", "open your skill tree");
        line(source, "/skill points", "view your Skill & Quest Shards");
        line(source, "/skill guide", "open the guide book");
        line(source, "/quests", "open your bounty board (alias /bounty)");
        source.sendSystemMessage(Component.literal("Admin / op commands:").withStyle(ChatFormatting.GRAY));
        line(source, "/skill points <player> add|set|reset <n>", "grant/set/clear Skill Shards");
        line(source, "/skill questshards <player> add|set|reset <n>", "grant/set/clear Quest Shards");
        line(source, "/skill reset <player>", "refund all of a player's unlocks");
        line(source, "/skill recalc <player>", "recompute earned Shards from advancements");
        line(source, "/skill reload", "reload the points + tree config from disk");
        line(source, "/skill regen [fresh]", "rebuild tree (keeps your changes; 'fresh' = full reset)");
        line(source, "/skill regenpoints", "reset points.json (advancement values) to the new defaults");
        line(source, "/skill editor", "node editor (move/delete nodes)");
        line(source, "/skill layout", "drag the lane icons around to rearrange them");
        line(source, "/skill edit ...", "live-edit nodes (cost/slot/effects/requires)");
        line(source, "/quests board [remove|refresh]", "place / remove / re-render a bounty board");
        line(source, "/quests reroll", "force a fresh set of universal bounties now");
        line(source, "/quests graduate <player>", "move a player to the main bounty board");
        line(source, "/quests starter <player>", "send a player back to the starter board");
    }

    private static void header(CommandSourceStack source, String text) {
        source.sendSystemMessage(Component.literal("— " + text + " —").withStyle(ChatFormatting.GOLD));
    }

    private static void line(CommandSourceStack source, String cmd, String desc) {
        source.sendSystemMessage(Component.literal(cmd).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" — " + desc).withStyle(ChatFormatting.GRAY)));
    }
}

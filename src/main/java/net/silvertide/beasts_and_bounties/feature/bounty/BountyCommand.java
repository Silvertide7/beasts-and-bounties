package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

final class BountyCommand {
    private BountyCommand() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bestiary")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("board")
                        .then(Commands.literal("reroll")
                                .executes(context -> {
                                    PlayerBounties.reroll(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.board.rerolled"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                            targets.forEach(PlayerBounties::clear);
                                            context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.board.cleared", targets.size()), true);
                                            return targets.size();
                                        })))));
    }
}

package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.ToIntFunction;

final class BestiaryCommand {
    private static final DynamicCommandExceptionType UNTRACKED_ENTITY =
            new DynamicCommandExceptionType(entityId -> Component.translatable("commands.beasts_and_bounties.untracked", entityId));
    private static final SuggestionProvider<CommandSourceStack> TRACKED_ENTITIES =
            (context, builder) -> SharedSuggestionProvider.suggestResource(BestiaryEntries.all().keySet(), builder);

    private enum Counter {
        KILLS("kills", Progress::kills, Progress::withKills),
        BOUNTIES("bounties", Progress::bounties, Progress::withBounties);

        private final String name;
        private final ToIntFunction<Progress> read;
        private final BiFunction<Progress, Integer, Progress> write;

        Counter(String name, ToIntFunction<Progress> read, BiFunction<Progress, Integer, Progress> write) {
            this.name = name;
            this.read = read;
            this.write = write;
        }
    }

    private enum Change {
        SET("set", (current, amount) -> amount),
        ADD("add", Long::sum),
        REMOVE("remove", (current, amount) -> current - amount);

        private final String name;
        private final LongBinaryOperator apply;

        Change(String name, LongBinaryOperator apply) {
            this.name = name;
            this.apply = apply;
        }
    }

    private BestiaryCommand() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("bestiary").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (Counter counter : Counter.values()) {
            var counterNode = Commands.literal(counter.name);
            for (Change change : Change.values()) counterNode.then(changeNode(counter, change));
            root.then(counterNode);
        }
        root.then(Commands.literal("reset")
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(BestiaryCommand::resetAll)
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests(TRACKED_ENTITIES)
                                .executes(BestiaryCommand::resetEntity))));
        root.then(Commands.literal("complete")
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(BestiaryCommand::completeAll)
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests(TRACKED_ENTITIES)
                                .executes(BestiaryCommand::completeEntity))));
        root.then(Commands.literal("get")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests(TRACKED_ENTITIES)
                                .executes(BestiaryCommand::get))));
        dispatcher.register(root);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> changeNode(Counter counter, Change change) {
        return Commands.literal(change.name)
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests(TRACKED_ENTITIES)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> changeCounter(context, counter, change)))));
    }

    private static int changeCounter(CommandContext<CommandSourceStack> context, Counter counter, Change change) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation entityId = ResourceLocationArgument.getId(context, "entity");
        BestiaryEntry entry = trackedEntry(entityId);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        for (ServerPlayer player : targets) {
            Progress progress = BestiaryProgress.of(player, entityId);
            long changed = change.apply.applyAsLong(counter.read.applyAsInt(progress), amount);
            int clamped = Math.clamp(changed, 0, Integer.MAX_VALUE);
            BestiaryProgress.set(player, entityId, entry, counter.write.apply(progress, clamped));
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.changed",
                counter.name, entityId.toString(), targets.size()), true);
        return targets.size();
    }

    private static int resetEntity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation entityId = ResourceLocationArgument.getId(context, "entity");
        BestiaryEntry entry = trackedEntry(entityId);
        targets.forEach(player -> BestiaryProgress.set(player, entityId, entry, Progress.NONE));
        context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.reset_entity", entityId.toString(), targets.size()), true);
        return targets.size();
    }

    private static int completeEntity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation entityId = ResourceLocationArgument.getId(context, "entity");
        BestiaryEntry entry = trackedEntry(entityId);
        targets.forEach(player -> complete(player, entityId, entry));
        context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.complete_entity", entityId.toString(), targets.size()), true);
        return targets.size();
    }

    private static int completeAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        targets.forEach(player -> BestiaryEntries.all().forEach((entityId, entry) -> complete(player, entityId, entry)));
        context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.complete", BestiaryEntries.all().size(), targets.size()), true);
        return targets.size();
    }

    private static void complete(ServerPlayer player, ResourceLocation entityId, BestiaryEntry entry) {
        Progress current = BestiaryProgress.of(player, entityId);
        Progress finalRequirement = entry.milestones().getLast().requirement();
        BestiaryProgress.set(player, entityId, entry, new Progress(
                Math.max(current.kills(), finalRequirement.kills()),
                Math.max(current.bounties(), finalRequirement.bounties())));
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        targets.forEach(BestiaryProgress::resetAll);
        context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.reset", targets.size()), true);
        return targets.size();
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        ResourceLocation entityId = ResourceLocationArgument.getId(context, "entity");
        BestiaryEntry entry = trackedEntry(entityId);
        Progress progress = BestiaryProgress.of(target, entityId);
        context.getSource().sendSuccess(() -> Component.translatable("commands.beasts_and_bounties.get",
                target.getDisplayName(), progress.kills(), progress.bounties(), entityId.toString(),
                entry.milestoneReached(progress) + 1, entry.milestones().size()), false);
        return progress.kills();
    }

    private static BestiaryEntry trackedEntry(ResourceLocation entityId) throws CommandSyntaxException {
        BestiaryEntry entry = BestiaryEntries.get(entityId);
        if (entry == null) throw UNTRACKED_ENTITY.create(entityId);
        return entry;
    }
}

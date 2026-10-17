package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(BeastsAndBountiesMod.MODID)
@PrefixGameTestTemplate(false)
public class BestiaryGameTests {
    private static final int SHIPPED_VANILLA_ENTRIES = 54;
    private static final double TOLERANCE = 1e-9;

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void advancementsFollowBestiaryProgress(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation skeleton = EntityType.getKey(EntityType.SKELETON);
        BestiaryEntry entry = BestiaryEntries.get(skeleton);
        helper.assertTrue(!hasAdvancement(player, "root"), "nothing awarded before any entry");
        BestiaryProgress.addKills(player, skeleton, entry, 1);
        helper.assertTrue(hasAdvancement(player, "root") && !hasAdvancement(player, "entry_complete"), "the first listed entry awards the root advancement only");
        BestiaryProgress.set(player, skeleton, entry, entry.milestones().getLast().requirement());
        helper.assertTrue(hasAdvancement(player, "entry_complete") && !hasAdvancement(player, "bestiary_complete"), "a maxed entry awards entry_complete, not the whole bestiary");
        BestiaryEntries.all().forEach((entityId, each) -> BestiaryProgress.set(player, entityId, each, each.milestones().getLast().requirement()));
        helper.assertTrue(hasAdvancement(player, "bestiary_complete"), "maxing every entry awards bestiary_complete");
        helper.succeed();
    }

    private static boolean hasAdvancement(ServerPlayer player, String path) {
        AdvancementHolder advancement = player.server.getAdvancements().get(BeastsAndBountiesMod.id(path));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    @GameTest(template = "empty")
    public static void everyShippedEntryLoads(GameTestHelper helper) {
        int loaded = BestiaryEntries.all().size();
        helper.assertTrue(loaded == SHIPPED_VANILLA_ENTRIES, "expected " + SHIPPED_VANILLA_ENTRIES + " entries, loaded " + loaded);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void normalMobMilestonesRevealKnowledgeAndStackBonuses(GameTestHelper helper) {
        BestiaryEntry skeleton = BestiaryEntries.get(EntityType.SKELETON);
        helper.assertTrue(skeleton != null, "skeleton entry missing");
        helper.assertFalse(skeleton.isDiscovered(kills(0)), "skeleton listed before any kill");
        helper.assertTrue(skeleton.isDiscovered(kills(1)) && !skeleton.knowledge(kills(1)).name(), "first kill should list it without a name");
        helper.assertTrue(skeleton.knowledge(kills(2)).name() && skeleton.knowledge(kills(2)).model(), "second kill should reveal name and model");
        helper.assertTrue(skeleton.knowledge(kills(4)).stats().isEmpty() && !skeleton.knowledge(kills(5)).stats().isEmpty(), "stats should arrive at five kills");
        helper.assertTrue(skeleton.knowledge(kills(4)).attackPatterns().isEmpty() && !skeleton.knowledge(kills(5)).attackPatterns().isEmpty(), "attack patterns should arrive with the stats");
        helper.assertTrue(!skeleton.knowledge(kills(50)).notes().isEmpty() && skeleton.knowledge(kills(50)).name(), "all information is revealed by 50 kills");
        assertClose(helper, skeleton.bonuses(kills(39)).damageMultiplier(), 1.0, "no damage bonus before 40 kills");
        assertClose(helper, skeleton.bonuses(kills(40)).damageMultiplier(), 1.02, "small damage bonus at 40 kills");
        Progress complete = new Progress(500, 2);
        assertClose(helper, skeleton.bonuses(complete).damageMultiplier(), 1.10, "damage bonuses stack to +10%");
        assertClose(helper, skeleton.bonuses(complete).damageTakenMultiplier(), 0.90, "resistances stack to 10%");
        assertClose(helper, skeleton.bonuses(kills(9)).extraLootChance(), 0, "no extra loot before 10 kills");
        assertClose(helper, skeleton.bonuses(kills(10)).extraLootChance(), 0.1, "10% extra loot at 10 kills");
        assertClose(helper, skeleton.bonuses(complete).extraLootChance(), 1.0, "extra loot reaches 100% when complete");
        assertClose(helper, skeleton.bonuses(complete).experienceMultiplier(), 1.5, "bonus experience stacks to +50%");
        helper.assertTrue(skeleton.nextRequirement(kills(500)).isPresent() && skeleton.nextRequirement(complete).isEmpty(), "500 kills and two bounties complete the skeleton");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void bossAttributeBonusesFollowKillsAndCanBeRemoved(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        double baseMaxHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
        BestiaryProgress.all(player).put(EntityType.getKey(EntityType.WITHER), new Progress(15, 2));
        BestiaryAttributes.reapply(player);
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth + 4, "wither grants +4 max health across its milestones");

        BestiaryProgress.all(player).clear();
        BestiaryAttributes.reapply(player);
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth, "bonus should vanish with the kills");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void maxedVanillaBestiaryStacksSmallAttributeTotals(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        double baseMaxHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
        double baseArmor = player.getAttributeValue(Attributes.ARMOR);
        BestiaryEntries.all().forEach((entityId, entry) -> BestiaryProgress.all(player).put(entityId, entry.milestones().getLast().requirement()));
        BestiaryProgress.all(player).put(EntityType.getKey(EntityType.ZOMBIE), Progress.NONE);
        BestiaryAttributes.reapply(player);
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth + 7.5, "every vanilla entry but the dev-overridden zombie stacks to +7.5 max health");
        assertClose(helper, player.getAttributeValue(Attributes.ARMOR), baseArmor + 3.5, "every vanilla entry stacks to +3.5 armor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packFilesMergeUnlessOneReplaces(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var base = packFile("base", "{\"classification\": \"elite\", \"milestones\": [{\"kills\": 1, \"name\": true}, {\"kills\": 5, \"rewards\": [{\"type\": \"damage_bonus\", \"amount\": 0.1}]}]}");
        var addon = packFile("addon", "{\"milestones\": [{\"kills\": 5, \"model\": true, \"rewards\": [{\"type\": \"damage_bonus\", \"amount\": 0.1}]}, {\"kills\": 10}]}");
        var replacer = packFile("replacer", "{\"replace\": true, \"milestones\": [{\"kills\": 2}]}");
        var entityId = EntityType.getKey(EntityType.ZOMBIE);

        BestiaryEntry merged = BestiaryEntries.mergeInPackOrder(entityId, List.of(base, addon), ops);
        helper.assertTrue(merged.classification() == Classification.ELITE, "addon without classification should keep the base one");
        helper.assertTrue(merged.milestones().size() == 3, "same-kill milestones should combine, got " + merged.milestones().size());
        helper.assertTrue(merged.knowledge(kills(5)).name() && merged.knowledge(kills(5)).model(), "combined milestone should keep both packs' reveals");
        assertClose(helper, merged.bonuses(kills(5)).damageMultiplier(), 1.2, "rewards from both packs should add");

        BestiaryEntry replaced = BestiaryEntries.mergeInPackOrder(entityId, List.of(base, addon, replacer), ops);
        helper.assertTrue(replaced.classification() == Classification.NORMAL, "replace should drop the earlier classification");
        helper.assertTrue(replaced.milestones().size() == 1 && replaced.milestones().getFirst().requirement().kills() == 2, "replace should drop earlier milestones");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void devFastZombieOverridesTheVanillaPack(GameTestHelper helper) {
        BestiaryEntry zombie = BestiaryEntries.get(EntityType.ZOMBIE);
        for (int kills = 1; kills < zombie.milestones().size(); kills++)
            helper.assertTrue(zombie.nextRequirement(kills(kills)).orElseThrow().kills() == kills + 1, "dev zombie should reach a milestone every kill, stalled at " + kills);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        double baseMaxHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
        BestiaryProgress.all(player).put(EntityType.getKey(EntityType.ZOMBIE), kills(zombie.milestones().size()));
        BestiaryAttributes.reapply(player);
        helper.assertTrue(zombie.nextRequirement(kills(zombie.milestones().size())).isEmpty(), "final kill should complete the zombie entry");
        assertClose(helper, zombie.bonuses(kills(zombie.milestones().size())).extraLootChance(), 1.0, "dev zombie reaches 100% extra loot");
        assertClose(helper, zombie.bonuses(kills(zombie.milestones().size())).experienceMultiplier(), 1.5, "dev zombie reaches +50% experience");
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth + 5, "completing the dev zombie grants +5 max health");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void protectedMobsHaveNoEntries(GameTestHelper helper) {
        for (var protectedMob : List.of(EntityType.VILLAGER, EntityType.WOLF, EntityType.CAT, EntityType.IRON_GOLEM, EntityType.HORSE, EntityType.ALLAY))
            helper.assertTrue(BestiaryEntries.get(protectedMob) == null, EntityType.getKey(protectedMob) + " should not be in the bestiary");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extraLootChanceIsCappedAtCertainty(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var generous = packFile("generous", "{\"milestones\": [{\"kills\": 1, \"rewards\": [{\"type\": \"extra_loot\", \"chance\": 0.8}, {\"type\": \"bonus_experience\", \"amount\": 0.5}]}, {\"kills\": 2, \"rewards\": [{\"type\": \"extra_loot\", \"chance\": 0.8}]}]}");
        BestiaryEntry entry = BestiaryEntries.mergeInPackOrder(EntityType.getKey(EntityType.ZOMBIE), List.of(generous), ops);
        assertClose(helper, entry.bonuses(kills(1)).extraLootChance(), 0.8, "single extra loot reward");
        assertClose(helper, entry.bonuses(kills(2)).extraLootChance(), 1.0, "stacked extra loot is capped at 100%");
        assertClose(helper, entry.bonuses(kills(2)).experienceMultiplier(), 1.5, "bonus experience carries forward");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bountyMilestoneBlocksEverythingAfterIt(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var gated = packFile("gated", "{\"milestones\": [{\"kills\": 1}, {\"kills\": 5, \"bounties\": 1, \"name\": true}, {\"kills\": 10, \"model\": true}, {\"kills\": 20, \"bounties\": 3}]}");
        BestiaryEntry entry = BestiaryEntries.mergeInPackOrder(EntityType.getKey(EntityType.ZOMBIE), List.of(gated), ops);

        helper.assertTrue(entry.milestoneReached(new Progress(50, 0)) == 0, "kills alone should stop at the bounty milestone");
        helper.assertFalse(entry.knowledge(new Progress(50, 0)).model(), "the kill milestone after the gate stays locked");
        helper.assertTrue(entry.milestoneReached(new Progress(4, 1)) == 0, "a bounty alone does not satisfy its kill requirement");
        helper.assertTrue(entry.milestoneReached(new Progress(10, 1)) == 2, "kills + bounty should open the gate and what follows");
        helper.assertTrue(entry.nextRequirement(new Progress(10, 1)).orElseThrow().equals(new Progress(20, 3)), "next requirement should name both kills and bounties");
        helper.assertTrue(entry.milestoneReached(new Progress(20, 3)) == 3, "last milestone needs both its kills and bounties");
        helper.assertTrue(entry.usesBounties(), "entry should report that it uses bounties");

        var bountyOnly = packFile("bounty_only", "{\"milestones\": [{\"kills\": 1}, {\"bounties\": 1}]}");
        BestiaryEntry bountyFirst = BestiaryEntries.mergeInPackOrder(EntityType.getKey(EntityType.ZOMBIE), List.of(bountyOnly), ops);
        helper.assertFalse(bountyFirst.isDiscovered(new Progress(99, 0)), "a bounty-only milestone sorts first and gates the whole entry");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void killsAreCreditedToEveryParticipant(GameTestHelper helper) {
        ServerPlayer killer = helper.makeMockServerPlayerInLevel();
        ServerPlayer helperPlayer = helper.makeMockServerPlayerInLevel();
        Zombie zombie = spawnStill(helper, EntityType.ZOMBIE, killer);
        DamageSources sources = helper.getLevel().damageSources();
        zombie.hurt(sources.playerAttack(helperPlayer), 1f);
        zombie.invulnerableTime = 0;
        zombie.hurt(sources.playerAttack(killer), 1000f);
        helper.assertTrue(zombie.isDeadOrDying(), "the zombie should be dead");
        helper.assertTrue(BestiaryProgress.of(killer, EntityType.ZOMBIE).kills() == 1, "the killer gets one kill");
        helper.assertTrue(BestiaryProgress.of(helperPlayer, EntityType.ZOMBIE).kills() == 1, "a player who only hit it also gets one kill");
        helper.assertTrue(BestiaryProgress.of(killer, EntityType.SKELETON).kills() == 0, "nothing else is credited");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void settingProgressReappliesAttributesAcrossMilestones(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation wither = EntityType.getKey(EntityType.WITHER);
        BestiaryEntry entry = BestiaryEntries.get(wither);
        double baseMaxHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
        BestiaryProgress.set(player, wither, entry, kills(7));
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth + 2, "half the wither boon at 7 kills");
        BestiaryProgress.set(player, wither, entry, kills(8));
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth + 2, "a milestone without an attribute changes nothing");
        BestiaryProgress.set(player, wither, entry, new Progress(15, 2));
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth + 4, "the full wither boon at 15 kills and two bounties");
        BestiaryProgress.set(player, wither, entry, Progress.NONE);
        helper.assertFalse(BestiaryProgress.all(player).containsKey(wither), "setting NONE removes the key");
        assertClose(helper, player.getAttributeValue(Attributes.MAX_HEALTH), baseMaxHealth, "the boon leaves with the progress");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void adminCommandsClampCompleteAndReset(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CommandSourceStack source = player.createCommandSourceStack().withPermission(Commands.LEVEL_GAMEMASTERS);
        Commands commands = helper.getLevel().getServer().getCommands();
        ResourceLocation skeleton = EntityType.getKey(EntityType.SKELETON);
        commands.performPrefixedCommand(source, "bestiary kills set @s minecraft:skeleton 3");
        helper.assertTrue(BestiaryProgress.of(player, skeleton).kills() == 3, "set writes the count");
        commands.performPrefixedCommand(source, "bestiary bounties add @s minecraft:skeleton 2");
        helper.assertTrue(BestiaryProgress.of(player, skeleton).bounties() == 2, "add raises the other counter");
        commands.performPrefixedCommand(source, "bestiary kills remove @s minecraft:skeleton 5");
        helper.assertTrue(BestiaryProgress.of(player, skeleton).kills() == 0 && BestiaryProgress.of(player, skeleton).bounties() == 2, "remove clamps at zero and keeps the other counter");
        commands.performPrefixedCommand(source, "bestiary kills set @s minecraft:skeleton 600");
        commands.performPrefixedCommand(source, "bestiary complete @s minecraft:skeleton");
        helper.assertTrue(BestiaryProgress.of(player, skeleton).kills() == 600 && BestiaryProgress.of(player, skeleton).bounties() == 2, "complete never lowers progress and supplies the bounties");
        commands.performPrefixedCommand(source, "bestiary complete @s");
        helper.assertTrue(BestiaryProgress.of(player, EntityType.COW).equals(new Progress(500, 2)), "complete raises every entry to its final requirement");
        commands.performPrefixedCommand(source, "bestiary reset @s minecraft:skeleton");
        helper.assertFalse(BestiaryProgress.all(player).containsKey(skeleton), "reset for one entry removes its key");
        commands.performPrefixedCommand(source, "bestiary reset @s");
        helper.assertTrue(BestiaryProgress.all(player).isEmpty(), "reset clears everything");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void damageHooksScaleHitsLootAndExperience(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BestiaryProgress.all(player).put(EntityType.getKey(EntityType.COW), new Progress(500, 2));
        BestiaryProgress.all(player).put(EntityType.getKey(EntityType.SKELETON), new Progress(500, 2));
        Cow cow = spawnStill(helper, EntityType.COW, player);
        Skeleton skeleton = spawnStill(helper, EntityType.SKELETON, player);
        DamageSources sources = helper.getLevel().damageSources();

        LivingIncomingDamageEvent dealt = new LivingIncomingDamageEvent(cow, new DamageContainer(sources.playerAttack(player), 10f));
        NeoForge.EVENT_BUS.post(dealt);
        assertClose(helper, dealt.getAmount(), 11, "+10% damage against a completed cow");
        LivingIncomingDamageEvent taken = new LivingIncomingDamageEvent(player, new DamageContainer(sources.mobAttack(skeleton), 10f));
        NeoForge.EVENT_BUS.post(taken);
        assertClose(helper, taken.getAmount(), 9, "10% less damage taken from a completed skeleton");

        helper.assertTrue(extraDrops(cow, sources, player).stream().anyMatch(drop -> drop.getItem().is(Items.BEEF)), "100% extra loot rolls the cow's loot table");
        cow.setBaby(true);
        helper.assertTrue(extraDrops(cow, sources, player).isEmpty(), "babies drop nothing, so there is no extra roll");
        cow.setBaby(false);
        GameRules.BooleanValue mobLoot = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
        mobLoot.set(false, helper.getLevel().getServer());
        helper.assertTrue(extraDrops(cow, sources, player).isEmpty(), "doMobLoot off means no extra roll");
        mobLoot.set(true, helper.getLevel().getServer());

        LivingExperienceDropEvent experience = new LivingExperienceDropEvent(cow, player, 10);
        NeoForge.EVENT_BUS.post(experience);
        helper.assertTrue(experience.getDroppedExperience() == 15, "+50% experience, got " + experience.getDroppedExperience());
        cow.discard();
        skeleton.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void progressSurvivesSavingAndDeath(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation skeleton = EntityType.getKey(EntityType.SKELETON);
        BestiaryProgress.all(player).put(skeleton, new Progress(7, 2));
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer loaded = helper.makeMockServerPlayerInLevel();
        loaded.load(saved);
        helper.assertTrue(BestiaryProgress.of(loaded, skeleton).equals(new Progress(7, 2)), "progress round-trips through NBT");
        ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(respawned, player, true));
        helper.assertTrue(BestiaryProgress.of(respawned, skeleton).equals(new Progress(7, 2)), "progress is copied on death");
        helper.succeed();
    }

    private static <T extends LivingEntity> T spawnStill(GameTestHelper helper, EntityType<T> type, ServerPlayer near) {
        T entity = type.create(helper.getLevel());
        entity.moveTo(near.getX() + 2, near.getY(), near.getZ());
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static List<ItemEntity> extraDrops(LivingEntity victim, DamageSources sources, ServerPlayer killer) {
        LivingDropsEvent event = new LivingDropsEvent(victim, sources.playerAttack(killer), new ArrayList<>(), true);
        NeoForge.EVENT_BUS.post(event);
        return new ArrayList<>(event.getDrops());
    }

    private static Progress kills(int kills) {
        return new Progress(kills, 0);
    }

    private static BestiaryEntries.PackFile packFile(String packId, String json) {
        return new BestiaryEntries.PackFile(packId, JsonParser.parseString(json));
    }

    private static void assertClose(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < TOLERANCE, message + ": expected " + expected + ", got " + actual);
    }
}

package com.yinfires.icecore.time;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class TimeCommands {
    private static final String[] TIMINGS={"fadeToCamera","revealCamera","fastForward","typewriterPerCharacter","summaryHold","summaryFade","fadeToPlayer","revealPlayer"};
    private TimeCommands(){}
    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root){
        var time=literal("time").then(literal("reload").executes(c->TimeConfigManager.get().reload(c.getSource().getPlayer())?1:0));
        time.then(literal("speed").then(literal("get").executes(TimeCommands::getSpeed)).then(literal("set").then(argument("value",DoubleArgumentType.doubleArg(.01,100)).executes(TimeCommands::setSpeed))));
        time.then(literal("hud").then(literal("get").executes(TimeCommands::getHud)).then(literal("set").then(argument("enabled", BoolArgumentType.bool()).executes(TimeCommands::setHud))));
        time.then(literal("naturalSummary").then(literal("get").executes(TimeCommands::getNaturalSummary)).then(literal("set").then(argument("enabled", BoolArgumentType.bool()).executes(TimeCommands::setNaturalSummary))));
        time.then(literal("trigger").then(literal("get").executes(TimeCommands::getTrigger)).then(literal("set").then(argument("list",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(BuildingDataManager.get().data().blockLists().keySet(),b)).executes(TimeCommands::setTrigger))));
        var regionArg=argument("region",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(BuildingDataManager.get().data().regions().keySet(),b));
        time.then(literal("region").then(literal("add").then(regionArg.executes(c->region(c,true)))).then(literal("remove").then(argument("region",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(TimeConfigManager.get().data().regions(),b)).executes(c->region(c,false)))));
        time.then(literal("camera").then(literal("get").then(argument("region",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(TimeConfigManager.get().data().cameras().keySet(),b)).executes(TimeCommands::getCamera))).then(literal("set").then(argument("region",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(BuildingDataManager.get().data().regions().keySet(),b)).executes(TimeCommands::setCamera))).then(literal("remove").then(argument("region",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(TimeConfigManager.get().data().cameras().keySet(),b)).executes(TimeCommands::removeCamera))));
        time.then(literal("timing").then(literal("get").executes(TimeCommands::getTimings)).then(literal("set").then(argument("name",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(TIMINGS,b)).then(argument("ticks",IntegerArgumentType.integer(1,1200)).executes(TimeCommands::setTiming)))));
        root.then(time);
    }
    private static int mutate(CommandContext<CommandSourceStack> c,java.util.function.Consumer<TimeConfigData> f){return TimeConfigManager.get().mutate(f,c.getSource().getPlayer())?1:0;}
    private static int getSpeed(CommandContext<CommandSourceStack> c){return show(c,"icecore.time.speed",TimeConfigManager.get().data().dayDurationMultiplier());}
    private static int setSpeed(CommandContext<CommandSourceStack> c){return mutate(c,d->d.setDayDurationMultiplier(DoubleArgumentType.getDouble(c,"value")));}
    private static int getHud(CommandContext<CommandSourceStack> c){return show(c,"icecore.time.hud",TimeConfigManager.get().data().hudEnabled());}
    private static int setHud(CommandContext<CommandSourceStack> c){return mutate(c,d->d.setHudEnabled(BoolArgumentType.getBool(c,"enabled")));}
    private static int getNaturalSummary(CommandContext<CommandSourceStack> c){return show(c,"icecore.time.natural_summary",TimeConfigManager.get().data().naturalDaySummaryEnabled());}
    private static int setNaturalSummary(CommandContext<CommandSourceStack> c){return mutate(c,d->d.setNaturalDaySummaryEnabled(BoolArgumentType.getBool(c,"enabled")));}
    private static int getTrigger(CommandContext<CommandSourceStack> c){return show(c,"icecore.time.trigger",TimeConfigManager.get().data().triggerBlockList());}
    private static int setTrigger(CommandContext<CommandSourceStack> c){return mutate(c,d->d.setTriggerBlockList(StringArgumentType.getString(c,"list")));}
    private static int region(CommandContext<CommandSourceStack> c,boolean add){String n=StringArgumentType.getString(c,"region");return mutate(c,d->{if(add)d.regions().add(n);else{d.regions().remove(n);d.cameras().remove(n);}});}
    private static int getCamera(CommandContext<CommandSourceStack> c){TimeCamera x=TimeConfigManager.get().data().cameras().get(StringArgumentType.getString(c,"region"));return x==null?0:show(c,"icecore.time.camera",x.x(),x.y(),x.z(),x.yaw(),x.pitch());}
    private static int setCamera(CommandContext<CommandSourceStack> c){var p=c.getSource().getPlayer();if(p==null||(!isPlainStick(p.getMainHandItem())&&!isPlainStick(p.getOffhandItem()))){PlayerFeedback.showFailure(c.getSource(),Component.translatable("icecore.time.camera.stick"));return 0;}String n=StringArgumentType.getString(c,"region");return mutate(c,d->d.cameras().put(n,new TimeCamera(p.getX(),p.getEyeY(),p.getZ(),p.getYRot(),p.getXRot())));}
    private static boolean isPlainStick(net.minecraft.world.item.ItemStack stack){return stack.is(Items.STICK)&&!stack.hasCustomHoverName()&&!stack.isEnchanted()&&!stack.hasTag();}
    private static int removeCamera(CommandContext<CommandSourceStack> c){return mutate(c,d->d.cameras().remove(StringArgumentType.getString(c,"region")));}
    private static int getTimings(CommandContext<CommandSourceStack> c){StringBuilder s=new StringBuilder();for(String n:TIMINGS){if(!s.isEmpty())s.append(", ");s.append(n).append('=').append(TimeConfigManager.get().data().timings().get(n));}return show(c,"icecore.time.timings",s);}
    private static int setTiming(CommandContext<CommandSourceStack> c){return mutate(c,d->d.timings().set(StringArgumentType.getString(c,"name"),IntegerArgumentType.getInteger(c,"ticks")));}
    private static int show(CommandContext<CommandSourceStack> c,String key,Object... args){PlayerFeedback.showSuccess(c.getSource(),()->Component.translatable(key,args),false);return 1;}
}

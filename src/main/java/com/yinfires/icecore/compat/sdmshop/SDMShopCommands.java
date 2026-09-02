package com.yinfires.icecore.compat.sdmshop;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.concurrent.CompletableFuture;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.commands.Commands.*;
public final class SDMShopCommands {
 private SDMShopCommands(){}
 public static void attach(ArgumentBuilder<CommandSourceStack,?> root){
  var s=literal("sdmshop").then(literal("list").executes(SDMShopCommands::list));
  s.then(literal("create").then(argument("id",word()).then(argument("display_name",greedyString()).executes(c->create(c,true)).then(literal("x").executes(c->0))).executes(c->create(c,false))));
  s.then(literal("rename").then(argument("id",word()).suggests(SDMShopCommands::suggest).then(argument("display_name",greedyString()).executes(SDMShopCommands::rename))));
  s.then(literal("delete").then(argument("id",word()).suggests(SDMShopCommands::suggest).executes(SDMShopCommands::delete)));
  s.then(literal("open").then(argument("id",word()).suggests(SDMShopCommands::suggest).then(argument("players",EntityArgument.players()).executes(c->open(c,false))).executes(c->open(c,false))));
  s.then(literal("edit").then(argument("id",word()).suggests(SDMShopCommands::suggest).then(argument("player",EntityArgument.player()).executes(c->edit(c,true))).executes(c->edit(c,false))));
  s.then(literal("migrate").executes(c->{ SDMShopManager.get(c.getSource().getServer()); c.getSource().sendSuccess(()->Component.translatable("icecore.sdmshop.migrated"),false); return 1;})); root.then(s);
 }
 private static int list(CommandContext<CommandSourceStack> c){var m=SDMShopManager.get(c.getSource().getServer()); c.getSource().sendSuccess(()->Component.literal(m.entries().stream().map(e->e.id()+" ("+e.displayName()+") "+e.uuid()).reduce((a,b)->a+", "+b).orElse("")),false);return 1;}
 private static int create(CommandContext<CommandSourceStack> c,boolean named){String id=getString(c,"id");String n=named?getString(c,"display_name"):id;return SDMShopManager.get(c.getSource().getServer()).create(id,n)?1:0;}
 private static int rename(CommandContext<CommandSourceStack> c){return SDMShopManager.get(c.getSource().getServer()).rename(getString(c,"id"),getString(c,"display_name"))?1:0;}
 private static int delete(CommandContext<CommandSourceStack> c){return SDMShopManager.get(c.getSource().getServer()).delete(getString(c,"id"))?1:0;}
 private static int open(CommandContext<CommandSourceStack> c,boolean edit){var m=SDMShopManager.get(c.getSource().getServer());var e=m.find(getString(c,"id"));if(e==null)return 0;try{java.util.Collection<ServerPlayer> ps; try { ps=EntityArgument.getPlayers(c,"players"); } catch(Exception ignored) { ps=java.util.List.of(c.getSource().getPlayerOrException()); } for(ServerPlayer p:ps)m.open(p,e.id(),edit);return 1;}catch(Exception x){return 0;}}
 private static int edit(CommandContext<CommandSourceStack> c,boolean has){try{ServerPlayer p=has?EntityArgument.getPlayer(c,"player"):c.getSource().getPlayerOrException();return openFor(c,p,true)?1:0;}catch(Exception e){return 0;}}
 private static boolean openFor(CommandContext<CommandSourceStack> c,ServerPlayer p,boolean edit){return SDMShopManager.get(c.getSource().getServer()).open(p,getString(c,"id"),edit);}
 private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(CommandContext<CommandSourceStack> c,com.mojang.brigadier.suggestion.SuggestionsBuilder b){return net.minecraft.commands.SharedSuggestionProvider.suggest(SDMShopManager.get(c.getSource().getServer()).entries().stream().map(SDMShopManager.Entry::id).toList(),b);}
}

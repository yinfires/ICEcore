package com.yinfires.icecore.shelf.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.*;
import java.util.function.*;

/** Shared command tree: permissions and completion are tested without a game client. */
final class ShelfDisplayCommands {
    static <S> LiteralArgumentBuilder<S> tree(Predicate<S> op,BiFunction<String,Integer,Integer> action) {
        var root=LiteralArgumentBuilder.<S>literal("icecore_render").executes(c->action.apply("status",0));
        for(String name:new String[]{"on","off","toggle","status","help"})
            root.then(LiteralArgumentBuilder.<S>literal(name).executes(c->action.apply(name,0)));
        root.then(LiteralArgumentBuilder.<S>literal("distance").then(com.mojang.brigadier.builder.RequiredArgumentBuilder.<S,Integer>argument("blocks",IntegerArgumentType.integer(1,64))
                .executes(c->action.apply("distance",IntegerArgumentType.getInteger(c,"blocks")))));
        for(String name:new String[]{"native","optimized","hidden"})
            root.then(restricted(name,op).executes(c->action.apply(name,0)));
        for(String name:new String[]{"stats","query"})
            root.then(restricted(name,op).then(com.mojang.brigadier.builder.RequiredArgumentBuilder.<S,Boolean>argument("enabled",BoolArgumentType.bool())
                .executes(c->action.apply(name,BoolArgumentType.getBool(c,"enabled")?1:0))));
        var frames=restricted("frames",op);
        for(String name:new String[]{"start","stop"}) frames.then(LiteralArgumentBuilder.<S>literal(name).executes(c->action.apply("frames_"+name,0)));
        return root.then(frames);
    }
    private static <S> LiteralArgumentBuilder<S> restricted(String name,Predicate<S> op) {
        return new LiteralArgumentBuilder<S>(name) {
            { requires(op); }
            @Override public com.mojang.brigadier.tree.LiteralCommandNode<S> build() {
                var original=super.build();
                var node=new com.mojang.brigadier.tree.LiteralCommandNode<S>(name,original.getCommand(),op,
                        original.getRedirect(),original.getRedirectModifier(),original.isFork()) {
                    @Override public java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> listSuggestions(
                            com.mojang.brigadier.context.CommandContext<S> context,com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
                        return op.test(context.getSource())?super.listSuggestions(context,builder):builder.buildFuture();
                    }
                };
                original.getChildren().forEach(node::addChild);return node;
            }
        };
    }
    private ShelfDisplayCommands() {}
}

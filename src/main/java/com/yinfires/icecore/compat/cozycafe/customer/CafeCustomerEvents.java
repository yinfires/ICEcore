package com.yinfires.icecore.compat.cozycafe.customer;

import com.yinfires.icecore.feedback.PlayerFeedback;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;

public final class CafeCustomerEvents {
    private CafeCustomerEvents(){}
    public static void register(){
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e)->CafeCustomers.start(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e)->CafeCustomers.stop());
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof ServerPlayer p)CafeCustomers.send(p);});
        MinecraftForge.EVENT_BUS.addListener(CafeCustomerEvents::commands);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.OnDatapackSyncEvent e)->CafeCustomers.invalidateMenus());
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent e)->{if(e.phase==net.minecraftforge.event.TickEvent.Phase.END)CafeCustomers.globalTick();});
    }
    private static void commands(RegisterCommandsEvent e){
        var cafe=Commands.literal("cafe").requires(s->s.hasPermission(2));
        cafe.then(Commands.literal("reload").executes(c->{boolean ok=CafeCustomers.reload();PlayerFeedback.show(c.getSource(),Component.translatable(ok?"icecore.cafe.reload_ok":"icecore.cafe.reload_failed"));return ok?1:0;}));
        cafe.then(Commands.literal("status").executes(c->{var d=CafeCustomers.data();PlayerFeedback.show(c.getSource(),Component.translatable("icecore.cafe.status",d.active,d.admitted,d.quota,d.countdown,d.session,d.dimension,net.minecraft.core.BlockPos.of(d.manager).toShortString(),d.openingReputation,d.openingDishes,d.seats.size(),d.timeBase));return 1;}));
        for(String operation:new String[]{"get","set","add","remove"}){
            var type=Commands.argument("type",StringArgumentType.word()).suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(CafeCustomers.config().types().stream().map(CafeCustomerConfig.Type::id),b));
            if(operation.equals("get"))type.executes(c->{String id=StringArgumentType.getString(c,"type");if(CafeCustomers.config().type(id)==null)return 0;PlayerFeedback.show(c.getSource(),Component.translatable("icecore.cafe.reputation",id,CafeCustomers.reputation(id)));return 1;});
            else type.then(Commands.argument("amount",IntegerArgumentType.integer(operation.equals("set")?0:1)).executes(c->{String id=StringArgumentType.getString(c,"type");int amount=IntegerArgumentType.getInteger(c,"amount");long next=operation.equals("set")?amount:(long)CafeCustomers.reputation(id)+(operation.equals("add")?amount:-amount);boolean ok=CafeCustomers.set(id,next);PlayerFeedback.show(c.getSource(),Component.translatable(ok?"icecore.cafe.changed":"icecore.cafe.unknown_type",id));return ok?1:0;}));
            cafe.then(Commands.literal(operation).then(type));
        }
        e.getDispatcher().register(Commands.literal("icecore").then(cafe));
    }
}

package com.yinfires.icecore.worldmap.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Searchable list for existing content; identifiers are never free-form binding input. */
final class MapChoiceScreen extends Screen {
    private final Screen parent;private final List<String> choices;private final Consumer<String> choose;
    private EditBox search;private List<String> filtered;private int first;private String query="";
    MapChoiceScreen(Screen parent,List<String> choices,Consumer<String> choose){super(Component.translatable("icecore.map.select"));this.parent=parent;this.choices=choices;this.choose=choose;filtered=choices;}
    @Override protected void init(){
        clearWidgets();int w=Math.min(width-20,320),x=(width-w)/2;
        search=new EditBox(font,x,10,w,20,Component.translatable("icecore.map.search"));search.setValue(query);search.setHint(Component.translatable("icecore.map.search"));search.setTooltip(Tooltip.create(Component.translatable("icecore.map.help.search")));search.setResponder(value->{query=value;filtered=choices.stream().filter(s->s.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT))).toList();first=0;rebuildWidgets();});addRenderableWidget(search);
        int rows=Math.max(1,(height-70)/22);for(int i=0;i<rows&&i+first<filtered.size();i++){String value=filtered.get(i+first);addRenderableWidget(Button.builder(Component.literal(value),b->{choose.accept(value);minecraft.setScreen(parent);}).tooltip(Tooltip.create(Component.translatable("icecore.map.help.choice",value))).bounds(x,36+i*22,w,20).build());}
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).tooltip(Tooltip.create(Component.translatable("icecore.map.help.back"))).bounds(x,height-26,w,20).build());
        setInitialFocus(search);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){first=Math.max(0,Math.min(Math.max(0,filtered.size()-Math.max(1,(height-70)/22)),first-(int)Math.signum(delta)));rebuildWidgets();return true;}
    @Override public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    void serverChanged(String previous){if(parent instanceof IslandMapScreen screen)screen.serverChanged(previous);else if(parent instanceof MapUnlockPreviewScreen preview)preview.serverChanged(previous);}
    void resourceChanged(){if(parent instanceof MapUnlockPreviewScreen preview)preview.resourceChanged();else if(parent instanceof IslandMapScreen screen)screen.refresh();}
    void disposeSession(){if(parent instanceof MapUnlockPreviewScreen preview)preview.disposeSession();else if(parent instanceof IslandMapScreen screen)screen.removed();}
    @Override public boolean isPauseScreen(){return false;}
}

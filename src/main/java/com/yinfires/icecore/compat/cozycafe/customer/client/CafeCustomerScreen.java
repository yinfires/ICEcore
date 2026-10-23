package com.yinfires.icecore.compat.cozycafe.customer.client;

import com.yinfires.icecore.client.WoodPanel;
import com.yinfires.icecore.compat.cozycafe.customer.*;
import io.github.chakyl.cozycafe.gui.MenuSelectorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

public final class CafeCustomerScreen extends Screen {
    private record Token(Component text,boolean hated){}
    private record Piece(FormattedCharSequence text,int x,int y,int width,boolean hated){}
    private record Row(CafeCustomerConfig.Type type,List<FormattedCharSequence> name,List<Piece> pieces,int y,int height){}
    private final List<Row> rows=new ArrayList<>();
    private MenuSelectorScreen scroller;
    private int left,top,panelWidth,viewHeight,totalHeight;
    private CafeCustomerConfig layoutConfig;
    private String language="";
    public CafeCustomerScreen(){super(Component.translatable("key.icecore.customers"));}
    @Override protected void init(){
        panelWidth=Math.max(120,Math.min(340,width-24));viewHeight=Math.max(45,Math.min(240,height-32));
        left=(width-panelWidth)/2;top=(height-viewHeight)/2;
        scroller=new MenuSelectorScreen(null,minecraft.player.getInventory(),Component.empty()){
            @Override protected void init(){} // No menu editing widgets or server container are created.
        };
        scroller.init(minecraft,width,height);layoutConfig=null;rebuild();
    }
    private void rebuild(){
        String lang=minecraft.getLanguageManager().getSelected();
        var config=CafeCustomerClientState.payload().config();
        if(layoutConfig==config&&language.equals(lang))return;
        language=lang;layoutConfig=config;rows.clear();int y=0;
        for(var type:CafeCustomerClientState.payload().config().types()){
            int textWidth=Math.max(20,panelWidth-36-font.width(type.cap()+"/"+type.cap()));
            var name=font.split(Component.translatable(type.nameKey()),textWidth);int tagY=6+Math.max(1,name.size())*12;
            List<Piece> pieces=new ArrayList<>();int x=0,line=0;
            List<Token> tokens=new ArrayList<>();
            for(String id:type.likes())tokens.add(new Token(Component.translatable(tagKey(id)),false));
            for(String id:type.dislikes())tokens.add(new Token(Component.translatable(tagKey(id)).withStyle(ChatFormatting.RED),true));
            for(Token token:tokens){
                for(FormattedCharSequence part:font.split(token.text,textWidth)){
                    int w=font.width(part);if(x>0&&x+w>textWidth){x=0;line++;}
                    pieces.add(new Piece(part,x,tagY+line*12,w,token.hated));x+=w+7;
                }
            }
            int h=Math.max(44,tagY+16+line*12);rows.add(new Row(type,name,List.copyOf(pieces),y,h));y+=h;
        }
        totalHeight=y;((CafeNativeScroller)scroller).icecore$configure(left+panelWidth-9,top,Math.max(0,totalHeight-viewHeight));
    }
    private static String tagKey(String id){return "tag."+id.replace(':','.').replace('/','.');}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        rebuild();renderBackground(g);WoodPanel.draw(g,left-6,top-6,panelWidth+12,viewHeight+12,1);
        int offset=((CafeNativeScroller)scroller).icecore$offset();
        g.enableScissor(left,top,left+panelWidth-14,top+viewHeight);
        for(Row row:rows){
            int y=top+row.y-offset;if(y+row.height<top||y>top+viewHeight)continue;
            for(int i=0;i<row.name.size();i++)g.drawString(font,row.name.get(i),left+5,y+3+i*12,0xFFFFFF,false);
            String rep=CafeCustomerClientState.payload().reputation().getOrDefault(row.type.id(),0)+"/"+row.type.cap();
            g.drawString(font,rep,left+panelWidth-23-font.width(rep),y+3,0xFFFFFF,false);
            for(Piece p:row.pieces){g.drawString(font,p.text,left+5+p.x,y+p.y,0xFFFFFF,false);
                if(p.hated)g.fill(left+5+p.x,y+p.y+4,left+5+p.x+p.width,y+p.y+5,0xFF000000);}
        }
        g.disableScissor();
        if(totalHeight>viewHeight){g.pose().pushPose();g.pose().translate(0,top,0);g.pose().scale(1,viewHeight/60f,1);g.pose().translate(0,-top,0);((CafeNativeScroller)scroller).icecore$draw(g);g.pose().popPose();}
    }
    private double nativeY(double y){return top+(y-top)*60/viewHeight;}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(totalHeight>viewHeight&&x>=left&&x<=left+panelWidth&&y>=top&&y<=top+viewHeight){scroller.mouseScrolled(x,nativeY(y),delta*18);return true;}return false;}
    @Override public boolean mouseClicked(double x,double y,int button){if(totalHeight>viewHeight&&x>=left+panelWidth-9&&x<=left+panelWidth-3&&y>=top&&y<=top+viewHeight){scroller.mouseClicked(x,nativeY(y),button);return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return scroller.mouseDragged(x,nativeY(y),b,dx,dy*60/viewHeight);}
    @Override public boolean mouseReleased(double x,double y,int b){((CafeNativeScroller)scroller).icecore$release();return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(CafeCustomerKeys.OPEN.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
}

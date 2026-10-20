package com.yinfires.icecore.shelf.client;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import com.mojang.logging.LogUtils;

/** Bounded, opt-in frame data. File output happens only after recording stops. */
final class ShelfFrameRecording {
    static final int CAPACITY=120_000;
    private static ShelfFrameRecording active;
    private final long[] times=new long[CAPACITY];
    private final int[] flags=new int[CAPACITY];
    private final String mode;
    private int size;
    ShelfFrameRecording(String mode) { this.mode=mode; }
    boolean add(long time,int state) {
        if(size==CAPACITY) return false;
        times[size]=time;flags[size++]=state;return true;
    }
    static void start() {
        if(active!=null) return;
        ShelfRenderStats.ENABLED=false;ShelfRenderStats.reset();
        active=new ShelfFrameRecording(com.yinfires.icecore.compat.render.RenderDiagnostics.mode.name());
    }
    static void frame() {
        var recording=active;if(recording==null) return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        int state=(mc.isWindowActive()?0:1)|(mc.screen!=null?2:0)
                |(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen?4:0)|(mc.level==null?8:0)
                |(ShelfRenderStats.ENABLED?16:0)
                |(!recording.mode.equals(com.yinfires.icecore.compat.render.RenderDiagnostics.mode.name())?32:0);
        if(!recording.add(System.nanoTime(),state)) stop();
    }
    static void stop() {
        var recording=active;if(recording==null) return;active=null;
        Path directory=net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("logs/icecore-frames");
        net.minecraft.Util.ioPool().execute(()->{
            try {
                Files.createDirectories(directory);
                Path output=Files.createTempFile(directory,"frames-",".csv");
                try(var writer=Files.newBufferedWriter(output,StandardCharsets.UTF_8)) { writer.write(recording.csv()); }
                LogUtils.getLogger().info("Shelf frame recording saved: {} (mode={}, samples={})",output,recording.mode,recording.size);
            } catch(IOException failure) { LogUtils.getLogger().error("Cannot save shelf frame recording",failure); }
        });
    }
    String csv() {
        StringBuilder out=new StringBuilder("mode,index,elapsedNs,intervalNs,flags,valid\n");
        for(int i=0;i<size;i++) {
            boolean valid=i>0 && flags[i]==0 && flags[i-1]==0;
            out.append(mode).append(',').append(i).append(',').append(times[i]-times[0]).append(',')
                .append(i==0?0:times[i]-times[i-1]).append(',').append(flags[i]).append(',').append(valid).append('\n');
        }
        return out.toString();
    }
}

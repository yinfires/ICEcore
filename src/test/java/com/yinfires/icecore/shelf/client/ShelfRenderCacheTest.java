package com.yinfires.icecore.shelf.client;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class ShelfRenderCacheTest {
    private ShelfRenderCache.Key key(int value) {
        CompoundTag data=new CompoundTag();data.putInt("test",value);return new ShelfRenderCache.Key(data);
    }
    @Test void seventyTwoIdenticalSlotsAcrossThirtyTwoShelvesBuildOnceAndReloadInvalidates() {
        ShelfRenderCache.clear();AtomicInteger calls=new AtomicInteger();var key=key(1);
        for(int frame=0;frame<100;frame++) {
            ShelfRenderCache.frame();
            for(int slot=0;slot<72*32;slot++) assertNotNull(ShelfRenderCache.lookup(key,()->{
                calls.incrementAndGet();return new ShelfRenderCache.Entry(null,64);
            }));
        }
        assertEquals(1,calls.get());
        ShelfRenderCache.clear();ShelfRenderCache.lookup(key(1),()->{ calls.incrementAndGet();return new ShelfRenderCache.Entry(null,64); });
        assertEquals(2,calls.get());
    }
    @Test void buildBudgetMemoryEvictionAndDataChangesAreBounded() {
        ShelfRenderCache.clear();var first=key(1);var second=key(2);var third=key(3);
        assertNotEquals(first,second);
        ShelfRenderCache.lookup(first,()->new ShelfRenderCache.Entry(null,20*1024*1024));
        ShelfRenderCache.lookup(second,()->new ShelfRenderCache.Entry(null,20*1024*1024));
        assertNull(ShelfRenderCache.lookup(third,()->fail("third build in one frame")));
        ShelfRenderCache.frame();AtomicInteger rebuilt=new AtomicInteger();
        ShelfRenderCache.lookup(first,()->{rebuilt.incrementAndGet();return new ShelfRenderCache.Entry(null,64);});
        assertEquals(1,rebuilt.get());
    }
    @Test void entryLimitEvictsOldestEvenWhenByteBudgetIsNotReached() {
        ShelfRenderCache.clear();var first=key(0);
        for(int i=0;i<=4096;i++) {
            ShelfRenderCache.frame();
            ShelfRenderCache.lookup(i==0?first:key(i),()->new ShelfRenderCache.Entry(null,64));
        }
        ShelfRenderCache.frame();AtomicInteger rebuilt=new AtomicInteger();
        ShelfRenderCache.lookup(first,()->{rebuilt.incrementAndGet();return new ShelfRenderCache.Entry(null,64);});
        assertEquals(1,rebuilt.get());
    }
    @Test void wholeShelfBudgetMatchesPlan() {
        assertEquals(256,ShelfSceneCache.MAX_SHELVES);
        assertEquals(32*1024*1024,ShelfSceneCache.MAX_BYTES+com.yinfires.icecore.compat.render.ExactSortCache.MAX_BYTES);
        assertTrue(ShelfSceneCache.MAX_BYTES>0);
        assertTrue(ShelfSceneCache.staticVboEligible(ShelfSceneCache.MAX_BYTES,256));
        assertFalse(ShelfSceneCache.staticVboEligible(ShelfSceneCache.MAX_BYTES+1,256));
        assertFalse(ShelfSceneCache.staticVboEligible(1,257));
    }
}

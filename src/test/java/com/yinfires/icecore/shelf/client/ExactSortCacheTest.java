package com.yinfires.icecore.shelf.client;
import com.yinfires.icecore.compat.render.ExactSortCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class ExactSortCacheTest {
    @Test void unproductiveCacheBacksOffAndCanRecover() {
        var gate=new com.yinfires.icecore.compat.render.SortCacheAdmission();
        for(int i=0;i<31;i++) assertTrue(gate.miss());
        assertFalse(gate.miss());assertFalse(gate.active());
        for(int i=0;i<119;i++) gate.frame();assertFalse(gate.active());
        gate.frame();assertTrue(gate.active());
        for(int i=0;i<100;i++) {assertTrue(gate.miss());gate.hit();}
        gate.clear();assertTrue(gate.active());
    }
    @Test void verifiedUpstreamFingerprintAndPublicDescriptor() throws Exception {
        try(var jar=new java.util.zip.ZipFile("libs/embeddium-0.3.31+mc1.20.1.jar")) {
            byte[] bytes=jar.getInputStream(jar.getEntry("me/jellysquid/mods/sodium/client/util/sorting/MergeSort.class")).readAllBytes();
            assertEquals("155209dc3a68097f9bcb33b4da6a530c6c26330a0f8d994642b79a29b1a83ffa",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
            var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(bytes).accept(node,0);
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals("mergeSort") && m.desc.equals("([F)[I") && (m.access&org.objectweb.asm.Opcodes.ACC_STATIC)!=0));
        }
    }
    @Test void exactInputAndDefensiveCopies() {
        var cache=new ExactSortCache();float[] keys=new float[64];int[] order=new int[64];
        for(int i=0;i<64;i++) { keys[i]=i;order[i]=63-i; }
        cache.remember(keys,order);int[] first=cache.lookup(keys);assertArrayEquals(order,first);
        first[0]=999;assertEquals(63,cache.lookup(keys)[0]);order[0]=888;assertEquals(63,cache.lookup(keys)[0]);
        keys[0]=-0f;assertNull(cache.lookup(keys));keys[0]=Float.NaN;assertNull(cache.lookup(keys));
        cache.clear();assertEquals(0,cache.bytes());assertNull(cache.lookup(new float[64]));
    }
    @Test void actualUpstreamSortIsReusedWithoutChangingTiesOrSpecialValues() throws Exception {
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{java.nio.file.Path.of("libs/embeddium-0.3.31+mc1.20.1.jar").toUri().toURL()},null)) {
            var method=loader.loadClass("me.jellysquid.mods.sodium.client.util.sorting.MergeSort").getMethod("mergeSort",float[].class);
            var cache=new ExactSortCache();var random=new java.util.Random(7);
            for(int round=0;round<100;round++) {
                cache.frame();
                float[] keys=new float[256];for(int i=0;i<keys.length;i++) keys[i]=random.nextInt(12);
                keys[0]=Float.NaN;keys[1]=Float.POSITIVE_INFINITY;keys[2]=-0f;
                int[] expected=(int[])method.invoke(null,(Object)keys);cache.remember(keys,expected);
                assertArrayEquals(expected,cache.lookup(keys));keys[10]+=.01f;assertNull(cache.lookup(keys));
                assertTrue(cache.bytes()<=ExactSortCache.MAX_BYTES);
            }
        }
    }
}

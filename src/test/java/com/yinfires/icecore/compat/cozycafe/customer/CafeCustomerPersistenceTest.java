package com.yinfires.icecore.compat.cozycafe.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.minecraft.nbt.CompoundTag;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class CafeCustomerPersistenceTest {
    @TempDir Path folder;
    private CafeCustomerConfig config(String body)throws Exception{
        Path file=folder.resolve("cafe_customers.json");Files.writeString(file,body,StandardCharsets.UTF_8);return CafeCustomerConfig.read(file);
    }
    private static final String TYPE="{\"id\":\"a\",\"nameKey\":\"test.a\",\"reputationCap\":200,\"likes\":[\"icecore:recipe_manager/sweet\",\"icecore:recipe_manager/sweet\"],\"dislikes\":[]}";
    @Test void rejectsConflictsAndUnsafeSkinsAndFractionalCaps()throws Exception{
        assertEquals(1,config("{\"customers\":["+TYPE+"]}").types().get(0).likes().size());
        assertThrows(IllegalArgumentException.class,()->config("{\"customers\":["+TYPE+","+TYPE+"]}"));
        assertThrows(IllegalArgumentException.class,()->config("{\"customers\":["+TYPE.replace("200","200.5")+"]}"));
        assertThrows(IllegalArgumentException.class,()->config("{\"customers\":["+TYPE.replace("\"dislikes\":[]","\"dislikes\":[\"icecore:recipe_manager/sweet\"]")+"]}"));
        String unsafe=TYPE.substring(0,TYPE.length()-1)+",\"skins\":[{\"id\":\"x\",\"file\":\"../escape.png\"}]}";
        assertThrows(IllegalArgumentException.class,()->config("{\"customers\":["+unsafe+"]}"));
        assertThrows(IllegalArgumentException.class,()->config("{\"customers\":["+TYPE+"],\"settings\":{\"tipChance\":1.1}}"));
    }
    @Test void sharedLedgerPersistsOneDueOpportunityAndUnpaidAmount(){
        var original=new CafeCustomerData();original.lastOpenedDay=123;original.previousTime=123*24000+6000;original.active=true;
        original.reputation.put("retired",150);original.quota=24;original.admitted=7;original.countdown=0;original.timeBase=9000;
        original.dimension="minecraft:overworld";original.manager=42;original.seats.add(99L);original.attraction.add("a");
        original.pendingRevenue=java.math.BigInteger.valueOf(Long.MAX_VALUE).multiply(java.math.BigInteger.TEN);original.claim("order");
        var restored=CafeCustomerData.load(original.save(new CompoundTag()));
        assertEquals(original.session,restored.session);assertEquals(123,restored.lastOpenedDay);assertTrue(restored.active);
        assertEquals(0,restored.countdown);assertEquals(7,restored.admitted);assertEquals(24,restored.quota);
        assertEquals(original.seats,restored.seats);assertEquals(original.reputation,restored.reputation);
        assertEquals(original.pendingRevenue,restored.pendingRevenue);assertEquals(original.settledOrders,restored.settledOrders);
        assertFalse(restored.claim("order"));assertTrue(restored.claim("new-order"));assertFalse(restored.claim("new-order"));
        assertEquals(0,CafeCustomerData.load(new CompoundTag()).reputation.size());
    }
    @Test void dayTransitionsAndPartialAdmissionNeverRestoreConsumedQuota(){
        var d=new CafeCustomerData();d.lastOpenedDay=2;d.active=true;d.quota=3;d.previousTime=48000+11000;
        assertTrue(d.admit());assertTrue(d.admit()); // A failed third spawn does not call admit.
        assertEquals(2,d.admitted);d.advance(48000+12000);assertFalse(d.active);assertFalse(d.admit());
        assertFalse(d.mayOpen(48000+13000));assertFalse(d.mayOpen(24000));assertTrue(d.mayOpen(72000));
        d.active=true;d.previousTime=72000+1000;d.lastOpenedDay=3;d.advance(120000+1000);
        assertFalse(d.active);assertTrue(d.mayOpen(120000+1000));assertEquals(2,d.admitted);
        d.active=true;d.quota=3;assertTrue(d.admit());assertFalse(d.admit());d.advance(120000+1100);assertFalse(d.active);
    }
    @Test void reputationDeltaKeepsDefinitionsAndLateSnapshotsAreRejected(){
        var definition=CafeCustomerConfig.empty();
        CafeCustomerClientState.clear();
        CafeCustomerClientState.accept(new CafeCustomerSnapshot.Payload(definition,java.util.Map.of("a",1),java.util.Map.of()),10);
        CafeCustomerClientState.accept(new CafeCustomerSnapshot.Payload(null,java.util.Map.of("a",2),java.util.Map.of()),11);
        assertSame(definition,CafeCustomerClientState.payload().config());
        CafeCustomerClientState.accept(new CafeCustomerSnapshot.Payload(CafeCustomerConfig.empty(),java.util.Map.of("a",0),java.util.Map.of()),9);
        assertEquals(2,CafeCustomerClientState.payload().reputation().get("a"));
        CafeCustomerClientState.clear();assertEquals(-1,CafeCustomerClientState.revision());
    }
}

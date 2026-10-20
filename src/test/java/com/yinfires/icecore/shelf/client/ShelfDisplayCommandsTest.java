package com.yinfires.icecore.shelf.client;
import com.mojang.brigadier.CommandDispatcher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class ShelfDisplayCommandsTest {
    @Test void nonOpCanUsePreferencesButCannotExecuteOrCompleteDiagnostics() throws Exception {
        var dispatcher=new CommandDispatcher<Boolean>();var calls=new java.util.ArrayList<String>();
        dispatcher.register(ShelfDisplayCommands.tree(op->op,(a,v)->{calls.add(a+":"+v);return 1;}));
        for(String command:new String[]{"","on","off","toggle","status","help","distance 1","distance 64"})
            assertEquals(1,dispatcher.execute("icecore_render"+(command.isEmpty()?"":" "+command),false));
        for(String command:new String[]{"native","optimized","hidden","stats true","query false","frames start","frames stop","distance 0","distance 65"})
            assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class,()->dispatcher.execute("icecore_render "+command,false));
        var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("icecore_render ",false)).get().getList().stream().map(s->s.getText()).toList();
        assertTrue(suggestions.containsAll(java.util.List.of("on","off","distance","status")));
        assertFalse(suggestions.contains("native"));assertFalse(suggestions.contains("frames"));
        for(String command:new String[]{"native","optimized","hidden","stats true","query false","frames start","frames stop"})
            assertEquals(1,dispatcher.execute("icecore_render "+command,true));
    }
    @Test void displayPolicyUsesInclusiveThreeDimensionalDistanceAndSwitch() {
        assertTrue(ShelfDisplayPolicy.visible(true,64,4096));
        assertFalse(ShelfDisplayPolicy.visible(true,64,4096.00001));
        assertFalse(ShelfDisplayPolicy.visible(false,64,0));
        assertTrue(ShelfDisplayPolicy.visible(true,1,1));
        assertFalse(ShelfDisplayPolicy.visible(true,1,2));
    }
    @Test void defaultsAndRangesMatchClientSpec() {
        var spec=com.yinfires.icecore.config.ICECoreClientConfig.SPEC;
        assertEquals(true,com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_ENABLED.getDefault());
        assertEquals(64,com.yinfires.icecore.config.ICECoreClientConfig.SHELF_DISPLAY_DISTANCE.getDefault());
        net.minecraftforge.common.ForgeConfigSpec.ValueSpec value=spec.getSpec().get(java.util.List.of("shelfDisplay","distance"));
        assertTrue(value.test(1));assertTrue(value.test(64));assertFalse(value.test(0));assertFalse(value.test(65));
    }
}

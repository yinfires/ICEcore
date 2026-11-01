package com.yinfires.icecore.worldmap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapEditorFormTest {
    private MapDefinition definition(){var d=new MapDefinition();var r=new MapDefinition.Region();r.id="first";d.regions.add(r);return d;}
    @Test void saveReadsBoundsExpansionAndFeatherTogetherAndRoundTrips(){
        var original=definition();var form=new MapEditorForm(original,"first",12);
        form.bounds=" -32, -48, 700, 800 ";form.expansion="96";form.feather="128";
        var saved=form.apply(original,"first","minecraft:overworld");
        var reopened=MapDefinition.parse(MapDefinition.JSON.toJson(saved));
        assertEquals(-32,reopened.minX);assertEquals(-48,reopened.minZ);assertEquals(700,reopened.maxX);assertEquals(800,reopened.maxZ);
        assertEquals(96,reopened.region("first").expansion);assertEquals(128,reopened.region("first").feather);
        assertEquals(0,original.minX);assertEquals(64,original.region("first").expansion);
    }
    @Test void ownSaveAcknowledgementAdvancesRevisionEvenWithFurtherUnsavedInput(){
        var original=definition();var form=new MapEditorForm(original,"first",12);form.bounds="-10,-20,640,768";
        var first=form.apply(original,"first",original.dimension);form.submitted(first);
        assertTrue(form.awaitingSave());
        form.bounds="-20,-30,800,900";form.expansion="100";form.id="next";
        assertFalse(form.accept(first,13,first,"first",false));assertEquals(13,form.revision());
        assertFalse(form.awaitingSave());
        assertEquals("-20,-30,800,900",form.bounds);assertEquals("100",form.expansion);assertEquals("next",form.id);
        var second=form.apply(first,"first",first.dimension);form.submitted(second);
        assertFalse(form.accept(second,14,second,"first",false));assertEquals(14,form.revision());assertEquals(100,second.region("first").expansion);
    }
    @Test void incomingOtherEditorChangesCannotOverwriteTextOrDrawing(){
        var original=definition();var remote=original.copy();remote.maxX=1024;
        var form=new MapEditorForm(original,"first",1);form.bounds="-100,-100,900,900";
        assertFalse(form.accept(remote,2,original,"first",false));assertEquals(1,form.revision());assertEquals("-100,-100,900,900",form.bounds);
        var drawing=new MapEditorForm(original,"first",1);assertFalse(drawing.accept(remote,2,original,"first",true));assertEquals(1,drawing.revision());
        var clean=new MapEditorForm(original,"first",1);assertTrue(clean.accept(remote,2,original,"first",false));assertEquals(2,clean.revision());
    }
    @Test void unlockOnlySynchronizationKeepsAllTextAndAdvancesRevision(){
        var original=definition();var form=new MapEditorForm(original,"first",1);form.bounds="-1,-2,800,900";form.feather="-";
        assertFalse(form.accept(original,2,original,"first",false));assertEquals(2,form.revision());assertEquals("-",form.feather);
    }
    @Test void invalidFieldIsTransactionalAndKeepsInputForCorrection(){
        var original=definition();var form=new MapEditorForm(original,"first",1);form.bounds="-1,-2,800,900";form.feather="0";
        assertThrows(IllegalArgumentException.class,()->form.apply(original,"first",original.dimension));assertEquals("0",form.feather);assertEquals(512,original.maxX);assertEquals(64,original.region("first").feather);
        form.feather="32";assertEquals(32,form.apply(original,"first",original.dimension).region("first").feather);
    }
    @Test void changingSelectionLoadsItsOwnParametersAndTheCurrentDraftBounds(){
        var d=definition();var second=new MapDefinition.Region();second.id="second";second.expansion=111;second.feather=55;d.regions.add(second);d.maxX=900;
        var form=new MapEditorForm(d,"first",1);form.load(d,"second");assertEquals("second",form.id);assertEquals("111",form.expansion);assertEquals("55",form.feather);assertEquals("0,0,900,512",form.bounds);
    }
    @Test void successfulSnapshotKeepsItsAtlasWhenEditingThenSavingAgain(){
        var original=definition();var form=new MapEditorForm(original,"first",1);
        var published=original.copy();published.atlas="atlas_capture";form.submitted(published);
        form.feather="90";assertFalse(form.accept(published,2,original,"first",false));
        var next=form.apply(original,"first",original.dimension);assertEquals("atlas_capture",next.atlas);assertEquals(90,next.region("first").feather);assertEquals(2,form.revision());
    }
}

package com.yinfires.icecore.workstation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class WorkstationItemOrderTest {
 @Test void lastOccupiedSequenceIsTheOnlyCandidate(){long[] s={1,0,3,2};assertEquals(2,WorkstationItemOrder.lastSlot(s,i->s[i]!=0));}
 @Test void containerRequirementCannotBeSkipped(){assertFalse(WorkstationItemOrder.canTake(true,true,false));assertFalse(WorkstationItemOrder.canTake(true,false,false));assertTrue(WorkstationItemOrder.canTake(true,false,true));assertTrue(WorkstationItemOrder.canTake(false,true,false));assertFalse(WorkstationItemOrder.canTake(false,false,false));}
 @Test void invalidSequenceFallsBackToSlotOrder(){long[] s=new long[3];long n=WorkstationItemOrder.restore(s,new long[0],0,i->i!=1);assertArrayEquals(new long[]{1,0,2},s);assertEquals(3,n);}

 @Test void interleavedItemsCannotSkipTopAndRefilledSlotIsNewest() {
  long[] order = {1, 2, 3, 4, 5, 6, 7, 8, 9};
  String[] required = {"bowl", "", "bottle", "", "bucket", "", "bowl", "", "bottle"};
  for (String held : new String[]{"", "bowl", "bucket"}) {
   int top = WorkstationItemOrder.lastSlot(order, i -> order[i] != 0);
   assertEquals(8, top);
   assertFalse(WorkstationItemOrder.canTake(!required[top].isEmpty(), held.isEmpty(), required[top].equals(held)));
   assertEquals(9, order[top]);
  }
  assertTrue(WorkstationItemOrder.canTake(true, false, true));
  order[8] = 0;
  assertEquals(7, WorkstationItemOrder.lastSlot(order, i -> order[i] != 0));
  assertFalse(WorkstationItemOrder.canTake(false, false, true));
  order[8] = 10;
  assertEquals(8, WorkstationItemOrder.lastSlot(order, i -> order[i] != 0));
 }

 @Test void reloadPreservesOrderAndAdvancesCounter() {
  long[] restored = new long[4];
  assertEquals(13, WorkstationItemOrder.restore(restored, new long[]{8, 0, 12, 3}, 2, i -> i != 1));
  assertArrayEquals(new long[]{8, 0, 12, 3}, restored);
  assertEquals(2, WorkstationItemOrder.lastSlot(restored, i -> i != 1));
  assertEquals(20, WorkstationItemOrder.restore(restored, restored.clone(), 20, i -> i != 1));
 }

 @Test void corruptOrderHasDeterministicFallbackAndEmptySlotsAreCleared() {
  for (long[] saved : new long[][]{{1,1,2}, {0,2,3}, {-1,2,3}, {Long.MAX_VALUE,2,3}}) {
   long[] restored = new long[3];
   assertEquals(4, WorkstationItemOrder.restore(restored, saved, 1, i -> true));
   assertArrayEquals(new long[]{1,2,3}, restored);
  }
  long[] empty = new long[9];
  assertEquals(-1, WorkstationItemOrder.lastSlot(empty, i -> false));
  assertEquals(1, WorkstationItemOrder.restore(empty, new long[]{1}, 0, i -> false));
  assertArrayEquals(new long[9], empty);
 }
}

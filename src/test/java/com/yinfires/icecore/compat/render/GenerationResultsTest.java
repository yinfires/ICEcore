package com.yinfires.icecore.compat.render;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GenerationResultsTest {
 @Test void cachesFalseAndInvalidatesOnIdentityNotEquality() {
  var cache=new GenerationResults<Object,Boolean>();Object key=new Object(),first=new String("same"),second=new String("same");
  assertNull(cache.lookup(first,key));cache.remember(first,first,key,false);assertEquals(false,cache.lookup(first,key));
  assertNull(cache.lookup(second,key));cache.remember(first,second,key,true);assertNull(cache.lookup(second,key));
  cache.remember(second,second,key,true);assertEquals(true,cache.lookup(second,key));
 }
 @Test void oldComputationCannotPolluteNewGeneration() throws Exception {
  var cache=new GenerationResults<Object,Boolean>();Object key=new Object(),a=new Object(),b=new Object();
  cache.lookup(a,key);cache.lookup(b,key);
  Thread old=new Thread(()->cache.remember(a,a,key,true));old.start();old.join();assertNull(cache.lookup(b,key));
 }
 @Test void boundedCacheFallsBackWithoutWrongAnswers() {
  var cache=new GenerationResults<Object,Boolean>();Object source=new Object();cache.lookup(source,new Object());
  for(int i=0;i<4100;i++) cache.remember(source,source,new Object(),true);
  Object last=new Object();cache.remember(source,source,last,true);assertNull(cache.lookup(source,last));
 }
}

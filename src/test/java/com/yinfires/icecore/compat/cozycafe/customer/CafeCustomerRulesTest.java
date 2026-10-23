package com.yinfires.icecore.compat.cozycafe.customer;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CafeCustomerRulesTest {
    @Test void quotaHasContinuousMenuCurveAndRoundsOnlyAtEnd(){
        assertEquals(6,CafeCustomerRules.quota(0,1));
        assertEquals(56,CafeCustomerRules.quota(600,100));
        assertEquals(0,CafeCustomerRules.quota(600,0));
        assertEquals(.825,CafeCustomerRules.menuMultiplier(3),1e-12);
        assertEquals(1+7/60.0,CafeCustomerRules.menuMultiplier(17),1e-12);
        for(int dishes=1;dishes<150;dishes++)for(int rep=0;rep<600;rep++){
            int n=CafeCustomerRules.quota(rep,dishes);
            assertTrue(n<=CafeCustomerRules.quota(rep+1,dishes));
            assertTrue(n<=CafeCustomerRules.quota(rep,dishes+1));
        }
    }
    @Test void exactThirdsUseLongArithmetic(){
        assertEquals(2,CafeCustomerRules.speed(100,300));
        assertEquals(1,CafeCustomerRules.speed(101,300));
        assertEquals(1,CafeCustomerRules.speed(200,300));
        assertEquals(0,CafeCustomerRules.speed(201,300));
        assertEquals(0,CafeCustomerRules.speed(Integer.MAX_VALUE,Integer.MAX_VALUE));
        assertEquals(1,CafeCustomerRules.tip(0));assertEquals(1,CafeCustomerRules.tip(19));assertEquals(2,CafeCustomerRules.tip(20));
    }
    @Test void groupsRemainNormalizedAndSamplingMatchesDistribution(){
        Random random=new Random(4219);
        for(int quota=0;quota<80;quota++){
            double[] weights=CafeCustomerRules.groups(quota);double sum=0,mean=0;
            for(int i=0;i<4;i++){assertTrue(weights[i]>0);sum+=weights[i];mean+=(i+1)*weights[i];}
            assertEquals(1,sum,1e-12);assertEquals(mean,CafeCustomerRules.meanGroup(quota),1e-12);
        }
        int[] counts=new int[4];double[] weights=CafeCustomerRules.groups(24);
        for(int i=0;i<100000;i++)counts[CafeCustomerRules.sample(weights,random.nextDouble())]++;
        for(int i=0;i<4;i++)assertEquals(weights[i],counts[i]/100000.0,.005);
        assertEquals(0,CafeCustomerRules.sample(weights,0));assertEquals(3,CafeCustomerRules.sample(weights,Math.nextDown(1.0)));
    }
    @Test void timingClampsAfterVacancyAndJitter(){
        assertEquals(12000,CafeCustomerRules.paceBase(0,1));assertEquals(9000,CafeCustomerRules.paceBase(3000,1));
        assertEquals(9000,CafeCustomerRules.paceBase(11999,1));assertEquals(18000,CafeCustomerRules.paceBase(24000+6000,2));
        assertEquals(0,CafeCustomerRules.clampReputation(Long.MIN_VALUE,200));assertEquals(200,CafeCustomerRules.clampReputation(Long.MAX_VALUE,200));
        assertEquals(100,CafeCustomerRules.clampReputation(150,100));
        assertEquals(200,CafeCustomerRules.interval(100,56,1,0,200,900,.2,.2));
        assertEquals(900,CafeCustomerRules.interval(100000,6,0,1,200,900,.2,.2));
        for(double free:new double[]{0,.5,1}){
            int low=CafeCustomerRules.interval(12000,24,free,0,200,900,.2,.2);
            int high=CafeCustomerRules.interval(12000,24,free,1,200,900,.2,.2);
            assertTrue(low>=200&&high<=900&&low<=high);
        }
    }
    @Test void coursesRemoveImpossibleCombinations(){
        assertEquals(1,CafeCustomerRules.courseCount(false,false));assertEquals(2,CafeCustomerRules.courses(false,false,0,false));
        assertEquals(2,CafeCustomerRules.courseCount(true,false));assertEquals(3,CafeCustomerRules.courses(true,false,1,false));
        assertEquals(6,CafeCustomerRules.courses(false,true,1,true));
        assertEquals(3,CafeCustomerRules.courseCount(true,true));assertEquals(7,CafeCustomerRules.courses(true,true,2,false));
        assertEquals(3,CafeCustomerRules.courses(true,true,1,true));assertEquals(6,CafeCustomerRules.courses(true,true,1,false));
        assertTrue(CafeCustomerRules.hasLaterCourse(7,1));assertTrue(CafeCustomerRules.hasLaterCourse(7,2));
        assertFalse(CafeCustomerRules.hasLaterCourse(7,3));assertFalse(CafeCustomerRules.hasLaterCourse(2,2));
        assertFalse(CafeCustomerRules.hasLaterCourse(3,2));assertTrue(CafeCustomerRules.hasLaterCourse(6,2));
    }
    @Test void foodSamplingExcludesHateAndFavorsMultipleLikes(){
        Random random=new Random(9001);int[] counts=new int[4];
        for(int i=0;i<100000;i++)counts[CafeCustomerRules.chooseFood(new int[]{0,1,2,10},new int[]{0,0,0,1},.9,random.nextDouble(),random.nextDouble())]++;
        assertEquals(0,counts[3]);assertEquals(.1/3,counts[0]/100000.0,.004);
        assertEquals(.9/2.5+.1/3,counts[1]/100000.0,.006);
        assertEquals(.9*1.5/2.5+.1/3,counts[2]/100000.0,.006);
        assertEquals(1,CafeCustomerRules.chooseFood(new int[]{0,0},new int[]{1,2},.9,.1,.75));
        assertEquals(1,CafeCustomerRules.chooseFood(new int[]{0,0},new int[]{0,0},.9,.1,.75));
    }
}

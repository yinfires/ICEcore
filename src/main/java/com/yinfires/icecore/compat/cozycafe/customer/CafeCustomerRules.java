package com.yinfires.icecore.compat.cozycafe.customer;

/** Pure arithmetic shared by server execution and deterministic regression tests. */
public final class CafeCustomerRules {
    private CafeCustomerRules() {}
    public static int clampReputation(long value,int cap){return (int)Math.max(0,Math.min(cap,value));}
    public static double paceBase(long gameTime,double multiplier){return Math.max(12000-Math.floorMod(gameTime,24000),9000)*multiplier;}
    public static double interpolate(double x, double[] xs, double[] ys) {
        if (x <= xs[0]) return ys[0];
        for (int i=1;i<xs.length;i++) if (x<=xs[i])
            return ys[i-1]+(ys[i]-ys[i-1])*(x-xs[i-1])/(xs[i]-xs[i-1]);
        return ys[ys.length-1];
    }
    public static double menuMultiplier(int count) {
        return interpolate(count,new double[]{1,5,10,25,50,100},new double[]{.75,.90,1,1.25,1.5,1.75});
    }
    public static int quota(int reputation,int dishes) {
        if (dishes<=0) return 0;
        return (int)Math.min(Integer.MAX_VALUE,Math.round((8+.04*Math.max(0,reputation))*menuMultiplier(dishes)));
    }
    public static double[] groups(int quota) {
        double[] result=new double[4];
        double[][] p={{.85,.12,.02,.01},{.65,.23,.09,.03},{.45,.30,.17,.08}};
        for(int i=0;i<4;i++) result[i]=interpolate(quota,new double[]{6,24,56},new double[]{p[0][i],p[1][i],p[2][i]});
        return result;
    }
    public static double meanGroup(int quota) {
        double[] p=groups(quota); double mean=0;
        for(int i=0;i<4;i++) mean+=(i+1)*p[i];
        return mean;
    }
    public static int sample(double[] weights,double unit) {
        double total=0; for(double w:weights) total+=w;
        double target=unit*total;
        for(int i=0;i<weights.length;i++) if((target-=weights[i])<0) return i;
        return weights.length-1;
    }
    public static int speed(int waited,int patience) {
        long w=Math.max(0,waited)*3L;
        return w<=patience ? 2 : w<=2L*patience ? 1 : 0;
    }
    public static long tip(int price) { return Math.max(1L,price/10L); }
    public static int courseCount(boolean drink,boolean dessert){return drink&&dessert?3:drink||dessert?2:1;}
    public static boolean hasLaterCourse(int mask,int nextCourse){return (mask&~((1<<Math.min(3,Math.max(0,nextCourse)))-1))!=0;}
    public static int courses(boolean drink,boolean dessert,int pattern,boolean chooseDrink){
        if(pattern==0)return 2;
        if(pattern==2&&drink&&dessert)return 7;
        return 2|(drink&&dessert?(chooseDrink?1:4):drink?1:4);
    }
    public static int chooseFood(int[] likes,int[] dislikes,double preferenceChance,double poolRoll,double choiceRoll){
        boolean hasSafe=false;for(int n:dislikes)if(n==0)hasSafe=true;
        boolean preferred=false;for(int i=0;i<likes.length;i++)if((!hasSafe||dislikes[i]==0)&&likes[i]>0)preferred=true;
        preferred=preferred&&poolRoll<preferenceChance;
        double[] weights=new double[likes.length];
        for(int i=0;i<likes.length;i++)if((!hasSafe||dislikes[i]==0)&&(!preferred||likes[i]>0))weights[i]=preferred?1+.5*(likes[i]-1):1;
        return sample(weights,choiceRoll);
    }
    public static int interval(double timeTicks,int quota,double freeRatio,double unit,int min,int max,double jitter,double vacancy) {
        double base=timeTicks*meanGroup(quota)/Math.max(1,quota);
        double value=base*(1-vacancy*Math.max(0,Math.min(1,freeRatio)))*(1-jitter+2*jitter*unit);
        return (int)Math.max(min,Math.min(max,Math.round(value)));
    }
}

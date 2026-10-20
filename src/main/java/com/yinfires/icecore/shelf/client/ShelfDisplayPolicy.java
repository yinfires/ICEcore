package com.yinfires.icecore.shelf.client;

final class ShelfDisplayPolicy {
    static boolean visible(boolean enabled,int distance,double squaredDistance) {
        return enabled && squaredDistance<=(double)distance*distance;
    }
    private ShelfDisplayPolicy() {}
}

package com.yinfires.icecore.shelf;

/** Fixed slot coordinates in model pixels; row zero is the front of the top shelf. */
public final class ShelfLayout {
    public static double x(int slot) { return 2.5+(slot%9)*11.0/8; }
    public static double y(int slot) { return 32-(slot/18)*8; }
    public static double z(int slot) { return (slot/9)%2==0 ? 5 : 11; }
    private ShelfLayout() {}
}

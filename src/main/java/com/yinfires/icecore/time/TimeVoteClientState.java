package com.yinfires.icecore.time;

import java.util.List;

public final class TimeVoteClientState {
    private static long revision;
    private static int total;
    private static List<ClientBoundVotePacket.PlayerEntry> confirmed=List.of();
    private TimeVoteClientState(){}
    public static void accept(ClientBoundVotePacket packet){if(packet.revision()<revision)return;revision=packet.revision();total=packet.total();confirmed=packet.confirmed();}
    public static int total(){return total;} public static List<ClientBoundVotePacket.PlayerEntry> confirmed(){return confirmed;}
    public static void reset(){revision=0;total=0;confirmed=List.of();}
}

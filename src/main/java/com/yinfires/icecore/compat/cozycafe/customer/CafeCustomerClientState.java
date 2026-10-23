package com.yinfires.icecore.compat.cozycafe.customer;

import java.util.Map;

/** No client-only signatures: computer getters are safe in dedicated-server class loading. */
public final class CafeCustomerClientState {
    private static long revision=-1;
    private static CafeCustomerSnapshot.Payload payload=new CafeCustomerSnapshot.Payload(CafeCustomerConfig.empty(),Map.of(),Map.of());
    private CafeCustomerClientState(){}
    public static void accept(CafeCustomerSnapshot.Payload next,long version){if(version<revision)return;payload=next.config()==null?new CafeCustomerSnapshot.Payload(payload.config(),next.reputation(),payload.skins()):next;revision=version;}
    public static CafeCustomerSnapshot.Payload payload(){return payload;}
    public static long revision(){return revision;}
    public static int total(){return payload.config().types().stream().mapToInt(t->payload.reputation().getOrDefault(t.id(),0)).sum();}
    public static void clear(){revision=-1;payload=new CafeCustomerSnapshot.Payload(CafeCustomerConfig.empty(),Map.of(),Map.of());}
}

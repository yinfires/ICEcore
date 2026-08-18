package com.yinfires.icecore;

import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(ICECore.MOD_ID)
public final class ICECore {
    public static final String MOD_ID = "icecore";

    public ICECore() {
        ICECoreNetwork.register();
    }
}

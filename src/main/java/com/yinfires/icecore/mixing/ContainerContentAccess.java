package com.yinfires.icecore.mixing;

/** Exposes logical contents that may not be visible through item/fluid capabilities. */
public interface ContainerContentAccess {
    boolean icecore$hasContainerContent();
    int icecore$contentFingerprint();
}

package com.yinfires.icecore.workstation;

/** Optional workstation mixins expose state without leaking target classes. */
public interface WorkstationAccess {
    WorkstationSlotState icecore$slots();
}

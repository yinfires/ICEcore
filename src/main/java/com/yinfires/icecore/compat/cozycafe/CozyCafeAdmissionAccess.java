package com.yinfires.icecore.compat.cozycafe;

/** Invalidates delayed skin callbacks whenever a manager stops admitting guests. */
public interface CozyCafeAdmissionAccess {
    long icecore$admissionGeneration();
}

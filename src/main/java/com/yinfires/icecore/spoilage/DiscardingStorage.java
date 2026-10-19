package com.yinfires.icecore.spoilage;

/** Only the existing trash inventory may destroy expired stacks on access. */
public interface DiscardingStorage {
    void icecore$discardExpired();
}

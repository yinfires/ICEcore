package com.yinfires.icecore.compat.render;
import java.util.Map;
/** Immutable upstream data generation. */
public record DisplayMapSnapshot(Object data,Object scripts,Map<String,?> merged) {}

package com.evolveum.midpoint.ninja.impl;

public interface ActionStateListener {

    default void onBeforeInit(NinjaContext context) {
    }

    default void onBeforeExecution(NinjaContext context) {
    }

    default void onAfterExecution(NinjaContext context) {
    }
}

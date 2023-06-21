package com.evolveum.midpoint.ninja.impl;

/**
 * @deprecated This listener is only used in tests.
 * State should be asserted directly via repository service initialized not through ninja.
 */
@Deprecated
public interface ActionStateListener {

    default void onBeforeInit(NinjaContext context) {
    }

    default void onBeforeExecution(NinjaContext context) {
    }

    default void onAfterExecution(NinjaContext context) {
    }
}

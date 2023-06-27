package com.evolveum.midpoint.ninja.action.upgrade;

public enum UpgradePriority {

    /**
     * Midpoint will not start.
     * </p>
     * Pre-upgrade verify check will check against such issues and will stop upgrade if such critical item is found.
     */
    CRITICALLY,

    /**
     * Midpoint will start, however some operations or tasks may fail because of incorrect configuration.
     * </p>
     * Pre-upgrade verify check will check against such issues and will print out warning if such critical
     * item is found, upgrade process can continue.
     */
    NECESSARY,

    OPTIONAL
}

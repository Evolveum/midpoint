package com.evolveum.midpoint.ninja.action.upgrade;

public enum UpgradePriority {

    CRITICALLY, // mp will not start -> pre-upgrade check by cekoval toto a zastavi

    NECESSARY, // mp will start, but may fail some tasks, pre upgrade-check by mal aspon varovat

    OPTIONAL
}

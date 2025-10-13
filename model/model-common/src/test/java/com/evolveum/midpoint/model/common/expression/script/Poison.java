/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

/**
 * Poisonous class for sandbox testing.
 *
 * Thou shalt not drink poison. Thou shalt be doomed to death.
 * Thou shalt not smell poison. That can have consequences.
 * Just looking at a poison is safe.
 *
 * @author Radovan Semancik
 */
public class Poison {

    public static final String POISON_DRINK_ERROR_MESSAGE = "POISONED!";
    public static final String POISON_DRINK_ERROR_MESSAGE_STATIC = "POISONED(static)!";

    private boolean lookedAt = false;
    private boolean smelled = false;

    public void look() {
        lookedAt = true;
    }

    public void smell() {
        smelled = true;
    }

    public void drink() {
        throw new Error(POISON_DRINK_ERROR_MESSAGE);
    }

    public static void staticDrink() {
        throw new Error(POISON_DRINK_ERROR_MESSAGE_STATIC);
    }

    public boolean isLookedAt() {
        return lookedAt;
    }

    public boolean isSmelled() {
        return smelled;
    }

    public void assertLookedAt() {
        assertTrue("Poison not looked at!", lookedAt);
    }

    public void assertSmelled() {
        assertTrue("Poison was not smelled!", smelled);
    }

    public void assertNotSmelled() {
        assertFalse("Poison was smelled!", smelled);
    }

}

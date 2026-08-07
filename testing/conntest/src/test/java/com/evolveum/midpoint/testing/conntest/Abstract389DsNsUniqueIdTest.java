/*
 * Copyright (c) 2014-2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.conntest;

/**
 * @author semancik
 *
 */
public abstract class Abstract389DsNsUniqueIdTest extends Abstract389DsTest {

    @Override
    public String getPrimaryIdentifierAttributeName() {
        return "nsUniqueId";
    }

    @Override
    protected boolean syncCanDetectDelete() {
        return false;
    }

    @Override
    protected boolean isUsingGroupShortcutAttribute() {
        return false;
    }

}

/*
 * Copyright (c) 2014-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import java.io.File;

import org.testng.annotations.AfterClass;

import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

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

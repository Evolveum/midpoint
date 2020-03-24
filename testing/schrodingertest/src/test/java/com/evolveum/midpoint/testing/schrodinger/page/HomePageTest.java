/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author skublik
 */

public class HomePage extends AbstractSchrodingerTest {

    @Test
    public void test001OpenPageWithSlashOnEndOfUrl() {
        Assert.assertFalse(
                aboutPage.gitDescribe().isEmpty());
    }
}

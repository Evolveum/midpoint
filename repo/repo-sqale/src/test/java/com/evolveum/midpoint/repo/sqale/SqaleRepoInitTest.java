/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class SqaleRepoInitTest extends AbstractSpringTest
        implements InfraTestMixin {

    @Test
    public void init() {

    }
}

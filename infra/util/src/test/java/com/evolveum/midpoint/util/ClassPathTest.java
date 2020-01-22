/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.util.Set;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class ClassPathTest {

    @Test
    public void listClassesLocalTest() {
        Set<Class<?>> cs = ClassPathUtil.listClasses("com.evolveum.midpoint.util");
        assertNotNull(cs);
        assertTrue(cs.contains(ClassPathUtil.class));
    }

    @Test
    public void listClassesJarTest() {
        Set<Class<?>> cs = ClassPathUtil.listClasses("org.testng.annotations");
        assertNotNull(cs);
        assertTrue(cs.contains(Test.class));
    }
}

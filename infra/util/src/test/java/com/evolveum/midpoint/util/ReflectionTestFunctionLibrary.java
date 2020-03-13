/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * @author semancik
 */
public class ReflectionTestFunctionLibrary extends AbstractUnitTest {

    Collection<String> calledIds = new ArrayList<>();

    // Test functions

    public void m(String a1) {
        calledIds.add("m1");
    }

    public void m(String a1, Integer a2) {
        calledIds.add("m2i");
    }

    public void m(String a1, String a2) {
        calledIds.add("m2s");
    }

    public void m(String a1, Object a2) {
        calledIds.add("m2o");
    }

    public void m(String a1, Integer a2, Long a3) {
        calledIds.add("m3");
    }

    public void m(String a1, Integer a2, Long a3, Boolean a4) {
        calledIds.add("m4");
    }

    public void l(String s) {
        calledIds.add("ls");
    }

    public void l(Collection<String> c) {
        calledIds.add("lc");
    }

    // Test functions: varargs

    public void v(String... strings) {
        calledIds.add("v:" + strings.length);
    }

    // Utility

    public boolean wasCalled(String methodId) {
        return calledIds.contains(methodId);
    }

    public void reset() {
        calledIds.clear();
    }

    public Collection<String> getCalledIds() {
        return calledIds;
    }
}

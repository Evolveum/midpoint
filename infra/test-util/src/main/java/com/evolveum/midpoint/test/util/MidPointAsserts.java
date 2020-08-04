/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class MidPointAsserts {

    public static <O extends ObjectType> void assertVersionIncrease(
            PrismObject<O> objectOld, PrismObject<O> objectNew) {
        long versionOld = parseVersion(objectOld);
        long versionNew = parseVersion(objectNew);
        assertTrue("Version not increased (from " + versionOld + " to " + versionNew + ")",
                versionOld < versionNew);
    }

    public static <O extends ObjectType> long parseVersion(PrismObject<O> object) {
        String version = object.getVersion();
        return Long.parseLong(version);
    }

    public static <O extends ObjectType> void assertVersion(PrismObject<O> object, int expectedVersion) {
        assertVersion(object, Integer.toString(expectedVersion));
    }

    public static <O extends ObjectType> void assertVersion(PrismObject<O> object, String expectedVersion) {
        assertEquals("Wrong version for " + object, expectedVersion, object.getVersion());
    }

    public static <O extends ObjectType> void assertOid(PrismObject<O> object, String expectedOid) {
        assertEquals("Wrong OID for " + object, expectedOid, object.getOid());
    }

    @UnusedTestElement
    public static void assertContainsCaseIgnore(String message, Collection<String> actualValues, String expectedValue) {
        AssertJUnit.assertNotNull(message + ", expected " + expectedValue + ", got null", actualValues);
        for (String actualValue : actualValues) {
            if (StringUtils.equalsIgnoreCase(actualValue, expectedValue)) {
                return;
            }
        }
        AssertJUnit.fail(message + ", expected " + expectedValue + ", got " + actualValues);
    }

    public static void assertNotContainsCaseIgnore(String message, Collection<String> actualValues, String expectedValue) {
        if (actualValues == null) {
            return;
        }
        for (String actualValue : actualValues) {
            if (StringUtils.equalsIgnoreCase(actualValue, expectedValue)) {
                AssertJUnit.fail(message + ", expected that value " + expectedValue + " will not be present but it is");
            }
        }
    }

    public static void assertObjectClass(ShadowType shadow, QName expectedStructuralObjectClass, QName... expectedAuxiliaryObjectClasses) {
        assertEquals("Wrong object class in " + shadow, expectedStructuralObjectClass, shadow.getObjectClass());
        PrismAsserts.assertEqualsCollectionUnordered("Wrong auxiliary object classes in " + shadow, shadow.getAuxiliaryObjectClass(), expectedAuxiliaryObjectClasses);
    }

    public static void assertInstanceOf(String message, Object object, Class<?> expectedClass) {
        assertNotNull(message + " is null", object);
        assertTrue(message + " is not instance of " + expectedClass + ", it is " + object.getClass(),
                expectedClass.isAssignableFrom(object.getClass()));
    }

    public static void assertThatReferenceMatches(ObjectReferenceType ref, String desc, String expectedOid, QName expectedType) {
        assertThat(ref).as(desc).isNotNull();
        assertThat(ref.getOid()).as(desc + ".oid").isEqualTo(expectedOid);
        assertThatTypeMatches(ref.getType(), desc + ".type", expectedType);
    }

    // here because of AssertJ dependency (consider moving)
    public static void assertThatTypeMatches(QName actualType, String desc, QName expectedType) {
        assertThat(actualType).as(desc)
                .matches(t -> QNameUtil.match(t, expectedType), "matches " + expectedType);
    }

    // here because of AssertJ dependency (consider moving)
    public static void assertUriMatches(String current, String desc, QName expected) {
        assertThat(current).as(desc)
                .isNotNull()
                .matches(s -> QNameUtil.match(QNameUtil.uriToQName(s, true), expected), "is " + expected);
    }

    // here because of AssertJ dependency (consider moving)
    public static void assertUriMatches(String current, String desc, String expected) {
        assertThat(current).as(desc)
                .isNotNull()
                .matches(s -> QNameUtil.matchUri(s, expected), "is " + expected);
    }
}

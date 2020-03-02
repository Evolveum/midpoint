/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 *
 */
public class TestFilterSimplifier {

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test010All() {
        System.out.println("===[ test010All ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all().buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test020None() {
        System.out.println("===[ test020None ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .none().buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test030Undefined() {
        System.out.println("===[ test030Undefined ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .undefined().buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test100AndLevel1() {
        System.out.println("===[ test100AndLevel1 ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all()
                .and().none()
                .and().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test110AndLevel1WithoutNone() {
        System.out.println("===[ test110AndLevel1WithoutNone ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all()
                .and().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test120AndEmpty() {
        System.out.println("===[ test120AndEmpty ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFactory().createAnd();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test150OrLevel1() {
        System.out.println("===[ test150OrLevel1 ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all()
                .or().none()
                .or().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test160OrLevel1WithoutAll() {
        System.out.println("===[ test160OrLevel1WithoutAll ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .none()
                .or().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test170OrLevel1Undefined() {
        System.out.println("===[ test170OrLevel1Undefined ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFactory().createOr(prismContext.queryFactory().createUndefined());
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test180OrEmpty() {
        System.out.println("===[ test180OrEmpty ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFactory().createOr();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test200AndLevel2() {
        System.out.println("===[ test200AndLevel2 ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all()
                .and().undefined()
                .and()
                    .block()
                        .none().or().none()
                    .endBlock()
                .and()
                    .block()
                        .none().or().none()
                    .endBlock()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test210OrLevel2() {
        System.out.println("===[ test210OrLevel2 ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .none()
                .or().undefined()
                .or()
                    .block()
                        .none().or().none()
                    .endBlock()
                .or()
                    .block()
                        .none().or().none()
                    .endBlock()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test300NotAll() {
        System.out.println("===[ test300NotAll ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().all()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test310NotNone() {
        System.out.println("===[ test310NotNone ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().none()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test320NotNotAll() {
        System.out.println("===[ test320NotNotAll ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().block().not().all().endBlock()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test330NotNotNone() {
        System.out.println("===[ test330NotNotNone ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().block().not().none().endBlock()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test400TypeAll() {
        System.out.println("===[ test400TypeAll ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .type(UserType.class).all()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof TypeFilter);
        TypeFilter typeSimplified = (TypeFilter) simplified;
        assertEquals("Wrong simplified filter type", UserType.COMPLEX_TYPE, typeSimplified.getType());
        assertTrue("Wrong simplified filter subfilter: " + typeSimplified.getFilter(), ObjectQueryUtil.isAll(typeSimplified.getFilter()));
    }

    @Test
    public void test410TypeNone() {
        System.out.println("===[ test410TypeNone ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .type(UserType.class).none()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test420TypeUndefined() {
        System.out.println("===[ test420TypeUndefined ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .type(UserType.class).undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof TypeFilter);
        TypeFilter typeSimplified = (TypeFilter) simplified;
        assertEquals("Wrong simplified filter type", UserType.COMPLEX_TYPE, typeSimplified.getType());
        assertTrue("Wrong simplified filter subfilter: " + typeSimplified.getFilter(), ObjectQueryUtil.isAll(typeSimplified.getFilter()));
    }

    @Test
    public void test500ExistsAll() {
        System.out.println("===[ test500ExistsAll ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT).all()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof ExistsFilter);
        ExistsFilter existsSimplified = (ExistsFilter) simplified;
        PrismAsserts.assertEquivalent("Wrong simplified filter path", UserType.F_ASSIGNMENT, existsSimplified.getFullPath());
        assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
    }

    @Test
    public void test510ExistsNone() {
        System.out.println("===[ test510ExistsNone ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT).none()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test520ExistsUndefined() {
        System.out.println("===[ test520ExistsUndefined ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT).undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        // THEN
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter, prismContext);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof ExistsFilter);
        ExistsFilter existsSimplified = (ExistsFilter) simplified;
        PrismAsserts.assertEquivalent("Wrong simplified filter path", UserType.F_ASSIGNMENT, existsSimplified.getFullPath());
        assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
    }

}

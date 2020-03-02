/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author mederly
 */
public class TestFilterSimplifier extends AbstractSchemaTest {

    @Test
    public void test010All() {
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
        assertEquals("Wrong simplified filter path", UserType.F_ASSIGNMENT, existsSimplified.getFullPath());
        assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
    }

    @Test
    public void test510ExistsNone() {
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
        assertEquals("Wrong simplified filter path", UserType.F_ASSIGNMENT, existsSimplified.getFullPath());
        assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
    }
}

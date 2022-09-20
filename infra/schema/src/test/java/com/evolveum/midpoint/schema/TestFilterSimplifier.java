/*
 * Copyright (C) 2010-2022 Evolveum and contributors
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
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestFilterSimplifier extends AbstractSchemaTest {

    @Test
    public void test010All() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all().buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test020None() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .none().buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test030Undefined() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .undefined().buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test100AndLevel1() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all()
                .and().none()
                .and().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test110AndLevel1WithoutNone() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all()
                .and().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test120AndEmpty() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFactory().createAnd();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test150OrLevel1() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .all()
                .or().none()
                .or().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test160OrLevel1WithoutAll() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .none()
                .or().undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test170OrLevel1Undefined() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFactory().createOr(prismContext.queryFactory().createUndefined());
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test180OrEmpty() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFactory().createOr();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test200AndLevel2() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
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

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test210OrLevel2() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
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

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test300NotAll() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().all()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test310NotNone() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().none()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test320NotNotAll() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().block().not().all().endBlock()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

    @Test
    public void test330NotNotNone() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .not().block().not().none().endBlock()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test400TypeAll() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .type(UserType.class).all()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof TypeFilter);
        TypeFilter typeSimplified = (TypeFilter) simplified;
        assertEquals("Wrong simplified filter type", UserType.COMPLEX_TYPE, typeSimplified.getType());
        assertTrue("Wrong simplified filter subfilter: " + typeSimplified.getFilter(), ObjectQueryUtil.isAll(typeSimplified.getFilter()));
    }

    @Test
    public void test410TypeNone() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .type(UserType.class).none()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test420TypeUndefined() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .type(UserType.class).undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof TypeFilter);
        TypeFilter typeSimplified = (TypeFilter) simplified;
        assertEquals("Wrong simplified filter type", UserType.COMPLEX_TYPE, typeSimplified.getType());
        assertTrue("Wrong simplified filter subfilter: " + typeSimplified.getFilter(), ObjectQueryUtil.isAll(typeSimplified.getFilter()));
    }

    @Test
    public void test500ExistsAll() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT).all()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof ExistsFilter);
        ExistsFilter existsSimplified = (ExistsFilter) simplified;
        PrismAsserts.assertEquivalent("Wrong simplified filter path", UserType.F_ASSIGNMENT, existsSimplified.getFullPath());
        assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
    }

    @Test
    public void test510ExistsNone() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT).none()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

    @Test
    public void test520ExistsUndefined() {
        given();
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        when();
        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT).undefined()
                .buildFilter();
        System.out.println("Original filter:\n" + filter.debugDump());

        then();
        ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
        System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
        assertTrue("Wrong simplified filter: " + simplified, simplified instanceof ExistsFilter);
        ExistsFilter existsSimplified = (ExistsFilter) simplified;
        PrismAsserts.assertEquivalent("Wrong simplified filter path", UserType.F_ASSIGNMENT, existsSimplified.getFullPath());
        assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
    }
}

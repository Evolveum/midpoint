/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.constructInitializedPrismContext;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * See MID-3249.
 *
 * @author mederly
 */
public class TestUnknownItems {

    public static final String TEST_DIR = "src/test/resources/common/xml";

    public static final File WRONG_ITEM_FILE = new File(TEST_DIR + "/user-wrong-item.xml");
    public static final File WRONG_NAMESPACE_FILE = new File(TEST_DIR + "/user-wrong-namespace.xml");

    @BeforeSuite
    public void setupDebug() {
        PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void test010ParseWrongItemStrict() throws Exception {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        // WHEN+THEN
        try {
            prismContext.parseObject(WRONG_ITEM_FILE);
        } catch (SchemaException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void test020ParseWrongItemCompat() throws Exception {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        // WHEN
        PrismObject<UserType> user = prismContext.parserFor(WRONG_ITEM_FILE).compat().parse();

        // THEN
        System.out.println("User:");
        System.out.println(user.debugDump());
        assertNotNull(user);
    }

    // Currently we simply mark the unknown value as raw.
    // This might or might not be correct.
    // (We should probably throw SchemaException instead.)
    // TODO discuss this
    @Test(enabled = false, expectedExceptions = SchemaException.class)
    public void test110ParseWrongNamespaceStrict() throws Exception {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        // WHEN+THEN
        PrismObject<UserType> user = prismContext.parseObject(WRONG_NAMESPACE_FILE);
        System.out.println("User:");
        System.out.println(user.debugDump());
        assertNotNull(user);
    }

    @Test
    public void test120ParseWrongNamespaceCompat() throws Exception {
        // GIVEN
        PrismContext prismContext = constructInitializedPrismContext();

        // WHEN
        PrismObject<UserType> user = prismContext.parserFor(WRONG_NAMESPACE_FILE).compat().parse();

        // THEN
        System.out.println("User:");
        System.out.println(user.debugDump());
        assertNotNull(user);
    }


}

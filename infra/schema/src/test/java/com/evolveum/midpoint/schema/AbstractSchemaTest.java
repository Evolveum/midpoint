/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public abstract class AbstractSchemaTest extends AbstractUnitTest {

    protected static final File COMMON_DIR = new File("src/test/resources/common");

    public static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
    public static final String USER_JACK_OID = "2f9b9299-6f45-498f-bc8e-8d17c6b93b20";
    public static final String USER_JACK_NAME = "jack";
    public static final long USER_JACK_ASSIGNMENT_ID = 111L;

    public static final File USER_BILL_FILE = new File(COMMON_DIR, "user-bill.xml");

    public static final File ROLE_CONSTRUCTION_FILE = new File(COMMON_DIR, "role-construction.xml");
    public static final String ROLE_CONSTRUCTION_OID = "cc7dd820-b653-11e3-936d-001e8c717e5b";
    public static final long ROLE_CONSTRUCTION_INDUCEMENT_ID = 1001L;
    public static final String ROLE_CONSTRUCTION_RESOURCE_OID = "10000000-0000-0000-0000-000000000004";

    public static final String NS_MODEL_EXT = "http://midpoint.evolveum.com/xml/ns/public/model/extension-3";
    public static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/test/extension";
    public static final ItemName EXT_STRING_TYPE = new ItemName(NS_EXT, "stringType");
    public static final ItemName EXT_DIFFERENT_STRING_TYPE = new ItemName(NS_EXT, "differentStringType");
    public static final ItemPath EXT_STRING_TYPE_PATH = ItemPath.create(ObjectType.F_EXTENSION, EXT_STRING_TYPE);
    static final ItemPath EXT_DIFFERENT_STRING_TYPE_PATH = ItemPath.create(ObjectType.F_EXTENSION, EXT_DIFFERENT_STRING_TYPE);

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        SchemaDebugUtil.initialize(); // Make sure the pretty printer is activated
    }

    protected PrismObjectDefinition<UserType> getUserDefinition() {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    }

    protected void displayValue(String title, DebugDumpable value) {
        PrismTestUtil.display(title, value);
    }

    public void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }
}

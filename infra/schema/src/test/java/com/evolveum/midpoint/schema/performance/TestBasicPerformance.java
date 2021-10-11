/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.performance;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.Test;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

public class TestBasicPerformance extends AbstractSchemaPerformanceTest {

    @Test
    public void test010Dummy() throws Exception {
        measure("currentTimeMillis", () -> System.currentTimeMillis());
        measure("long numbers addition", () -> System.currentTimeMillis() + 2000L);
    }

    @Test
    public void test100FindNameProperty() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("findProperty(name)", () -> jack.findProperty(UserType.F_NAME));
    }

    @Test
    public void test110FindNameItem() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("findItem(name)", () -> jack.findItem(UserType.F_NAME));
    }

    @Test
    public void test120FindExtensionProperty() throws Exception {
        PrismObject<UserType> jack = getJack();
        ItemPath bar23 = ItemPath.create(UserType.F_EXTENSION, "bar23");
        measure("findProperty(extension/bar23)", () -> jack.findProperty(bar23));
    }

    @Test
    public void test130FindExtensionItem() throws Exception {
        PrismObject<UserType> jack = getJack();
        ItemPath bar23 = ItemPath.create(UserType.F_EXTENSION, "bar23");
        measure("findItem(extension/bar23)", () -> jack.findItem(bar23));
    }

    @Test
    public void test200SetName() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("setName(name)", () -> {
            jack.asObjectable().setName(PolyStringType.fromOrig("jack_" + Math.random()));
            return true;
        });
    }

    @Test
    public void test210SetNameViaProperty() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("findProperty(name).setRealValue", () -> {
            jack.findProperty(UserType.F_NAME).setRealValue(PolyString.fromOrig("jack_" + Math.random()));
            return true;
        });
    }

    @Test
    public void test215SetNameViaPropertyUsingExistingValue() throws Exception {
        PrismObject<UserType> jack = getJack();
        PolyString realValue = PolyString.fromOrig("jack_" + Math.random());
        measure("findProperty(name).setRealValue(existing)", () -> {
            jack.findProperty(UserType.F_NAME).setRealValue(realValue);
            return true;
        });
    }

    @Test
    public void test220SetExtensionItemString() throws Exception {
        PrismObject<UserType> jack = getJack();
        ItemPath bar23 = ItemPath.create(UserType.F_EXTENSION, "bar23");
        measure("findProperty(extension/bar23).setRealValue", () -> {
            jack.findProperty(bar23).setRealValue("jack_" + Math.random());
            return 1;
        });
    }

    @Test
    public void test230SetExtensionItemPolyString() throws Exception {
        PrismObject<UserType> jack = getJack();
        ItemPath bar23 = ItemPath.create(UserType.F_EXTENSION, "bar23");
        measure("findProperty(extension/bar23).setRealValue(polystring)", () -> {
            jack.findProperty(bar23).setRealValue(PolyString.fromOrig("jack_" + Math.random()));
            return 1;
        });
    }

    @Test
    public void test300Clone() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("jack.clone", () -> jack.clone());
    }

    @Test
    public void test310ParseXml() throws Exception {
        PrismObject<UserType> jack = getJack();
        String string = getPrismContext().xmlSerializer().serialize(jack);
        measure("parse XML (" + string.length() + " chars)", () -> getPrismContext().parserFor(string).xml().parse());
        measure("parse XML to XNode (" + string.length() + " chars)", () -> getPrismContext().parserFor(string).xml().parseToXNode());
    }

    @Test
    public void test320ParseJson() throws Exception {
        PrismObject<UserType> jack = getJack();
        String string = getPrismContext().jsonSerializer().serialize(jack);
        measure("parse JSON (" + string.length() + " chars)", () -> getPrismContext().parserFor(string).json().parse());
        measure("parse JSON to XNode (" + string.length() + " chars)", () -> getPrismContext().parserFor(string).json().parseToXNode());
    }

    @Test
    public void test330ParseYaml() throws Exception {
        PrismObject<UserType> jack = getJack();
        String string = getPrismContext().yamlSerializer().serialize(jack);
        measure("parse YAML (" + string.length() + " chars)", () -> getPrismContext().parserFor(string).yaml().parse());
        measure("parse YAML to XNode (" + string.length() + " chars)", () -> getPrismContext().parserFor(string).yaml().parseToXNode());
    }

    @Test
    public void test340ParseXNode() throws Exception {
        PrismObject<UserType> jack = getJack();
        RootXNode xnode = getPrismContext().xnodeSerializer().serialize(jack);
        measure("parse XNode", () -> getPrismContext().parserFor(xnode).parse());
    }

    @Test
    public void test350SerializeToXml() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("serialize to XML", () -> getPrismContext().xmlSerializer().serialize(jack));
    }

    @Test
    public void test360SerializeToJson() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("serialize to JSON", () -> getPrismContext().jsonSerializer().serialize(jack));
    }

    @Test
    public void test370SerializeToYaml() throws Exception {
        PrismObject<UserType> jack = getJack();
        measure("serialize to YAML", () -> getPrismContext().yamlSerializer().serialize(jack));
    }

    @Test
    public void test400FindNameDefinition() throws Exception {
        SchemaRegistry schemaRegistry = getPrismContext().getSchemaRegistry();
        PrismObjectDefinition<UserType> userDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
        measure("userDefinition.findItemDefinition(UserType.F_NAME)", () -> userDefinition.findItemDefinition(UserType.F_NAME));
    }

    @Test
    public void test410FindAdminGuiConfigurationDefinition() throws Exception {
        SchemaRegistry schemaRegistry = getPrismContext().getSchemaRegistry();
        PrismObjectDefinition<UserType> userDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
        measure("userDefinition.findItemDefinition(UserType.F_ADMIN_GUI_CONFIGURATION)", () -> userDefinition.findItemDefinition(UserType.F_ADMIN_GUI_CONFIGURATION));
    }

    @Test
    public void test420FindUserDefinition() throws Exception {
        SchemaRegistry schemaRegistry = getPrismContext().getSchemaRegistry();
        measure("schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class)", () -> schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class));
    }
}

/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class TestParseObjectTemplate extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/object-template");
    private static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "object-template.xml");
    private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");
    private static final File WRONG_TEMPLATE_FILE = new File(TEST_DIR, "wrong-template.xml");

    @Test
    public void testParseObjectTemplateFileSingle() throws Exception {
        single(OBJECT_TEMPLATE_FILE,
                new QName(SchemaConstantsGenerated.NS_COMMON, "objectTemplate"));
    }

    @Test
    public void testParseUserTemplateFileSingle() throws Exception {
        single(USER_TEMPLATE_FILE,
                new QName(SchemaConstantsGenerated.NS_COMMON, "objectTemplate"));
    }

    @Test
    public void testParseObjectTemplateFileRoundTrip() throws Exception {
        roundTrip(OBJECT_TEMPLATE_FILE,
                new QName(SchemaConstantsGenerated.NS_COMMON, "objectTemplate"));
    }

    @Test
    public void testAccessObjectTemplateMultithreaded() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        int THREADS = 50;

        // WHEN
        PrismObject<ObjectTemplateType> template = prismContext.parseObject(OBJECT_TEMPLATE_FILE);
        template.freeze();    // this is necessary in order to eliminate thread-unsafe DOM value parsers
        MappingType mapping = template.asObjectable().getMapping().stream()
                .filter(m -> "Access role assignment".equals(m.getName()))
                .findAny().orElse(null);
        assertNotNull("The mapping was not found", mapping);
        AssignmentTargetSearchExpressionEvaluatorType evaluator = (AssignmentTargetSearchExpressionEvaluatorType)
                mapping.getExpression().getExpressionEvaluator().get(0).getValue();

        AtomicInteger errors = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    ObjectFilter filter = prismContext.getQueryConverter().createObjectFilter(RoleType.class, evaluator.getFilter());
                    EqualFilter equalFilter = (EqualFilter) filter;
                    assertNotNull(equalFilter.getExpression());
                    ExpressionType expression = (ExpressionType) equalFilter.getExpression().getExpression();
                    ScriptExpressionEvaluatorType script = (ScriptExpressionEvaluatorType) expression.getExpressionEvaluator().get(0).getValue();
                    String code = script.getCode();
                    assertNotNull("No code", code);
                    assertEquals("Wrong code", "return memberOf.split(\";\", -1)[0]", code.trim());
                } catch (Throwable t) {
                    errors.incrementAndGet();
                    throw new AssertionError("Got exception: " + t.getMessage(), t);
                }
            });
            thread.setName("Executor #" + i);
            thread.start();
            threads.add(thread);
        }

        // THEN
        waitForCompletion(threads, 20000);

        assertEquals("Wrong # of errors", 0, errors.get());
        // TODO some asserts on correct parsing maybe

        System.out.println("Frozen object:\n" + template.debugDump());
    }

    @Test
    public void testParseUserTemplateFileRoundTrip() throws Exception {
        roundTrip(USER_TEMPLATE_FILE,
                new QName(SchemaConstantsGenerated.NS_COMMON, "objectTemplate"));
    }

    @Test
    public void testParseWrongTemplateFile() throws Exception {
        File file = WRONG_TEMPLATE_FILE;

        // GIVEN
        PrismContext prismContext = getPrismContext();

        // WHEN
        try {
            PrismObject<ObjectTemplateType> object = prismContext.parseObject(file);
            System.out.println("Parsed object - SHOULD NOT OCCUR:");
            System.out.println(object.debugDump());
            fail("Object was successfully parsed while it should not!");
        }
        // THEN
        catch (SchemaException e) {
            // ok
        }
    }

    private void single(File file, QName elementName) throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();

        // WHEN
        PrismObject<ObjectTemplateType> object = prismContext.parseObject(file);

        // THEN
        System.out.println("Parsed object:");
        System.out.println(object.debugDump());

        assertObjectTemplate(object, elementName);
        assertObjectTemplateInternals(object);
    }

    private void roundTrip(File file, QName elementName) throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();

        // WHEN
        PrismObject<ObjectTemplateType> object = prismContext.parseObject(file);

        // THEN
        System.out.println("Parsed object:");
        System.out.println(object.debugDump());

        assertObjectTemplate(object, elementName);
        // do NOT go to the assertObjectTemplateInternals(...)
        // that will parse the raw values and it may change the clean state

        // WHEN
        String xml = prismContext.xmlSerializer().serialize(object);

        // THEN
        System.out.println("Serialized object:");
        System.out.println(xml);

        // TODO

        // WHEN
        PrismObject<ObjectTemplateType> reparsedObject = prismContext.parseObject(xml);

        // THEN
        System.out.println("Re-parsed object:");
        System.out.println(reparsedObject.debugDump());

        assertObjectTemplate(reparsedObject, elementName);
        assertObjectTemplateInternals(reparsedObject);
    }

    private void assertObjectTemplate(PrismObject<ObjectTemplateType> object, QName elementName) {
        object.checkConsistence();
        assertObjectTemplatePrism(object, elementName);
    }

    private void assertObjectTemplatePrism(PrismObject<ObjectTemplateType> object, QName elementName) {

        assertEquals("Wrong oid", "10000000-0000-0000-0000-000000000002", object.getOid());
        PrismObjectDefinition<ObjectTemplateType> objectDefinition = object.getDefinition();
        assertNotNull("No object definition", objectDefinition);
        PrismAsserts.assertObjectDefinition(objectDefinition, elementName,
                ObjectTemplateType.COMPLEX_TYPE, ObjectTemplateType.class);
        assertEquals("Wrong class", ObjectTemplateType.class, object.getCompileTimeClass());
        assertEquals("Wrong object item name", elementName, object.getElementName());
        ObjectTemplateType objectType = object.asObjectable();
        assertNotNull("asObjectable resulted in null", objectType);

        assertPropertyValue(object, "name", PrismTestUtil.createPolyString("Default User Template"));
        assertPropertyDefinition(object, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

    }

    // checks raw values of mappings
    // should be called only on reparsed values in order to catch some raw-data-related serialization issues (MID-2196)
    private void assertObjectTemplateInternals(PrismObject<ObjectTemplateType> object) throws SchemaException {
        int assignmentValuesFound = 0;
        for (ObjectTemplateMappingType mappingType : object.asObjectable().getMapping()) {
            if (mappingType.getExpression() != null) {
                if (mappingType.getTarget() != null &&
                        mappingType.getTarget().getPath() != null &&
                        UserType.F_ASSIGNMENT.equivalent(mappingType.getTarget().getPath().getItemPath())) {
                    ItemDefinition assignmentDef =
                            getPrismContext().getSchemaRegistry()
                                    .findObjectDefinitionByCompileTimeClass(UserType.class)
                                    .findItemDefinition(UserType.F_ASSIGNMENT);
                    for (JAXBElement evaluator : mappingType.getExpression().getExpressionEvaluator()) {
                        if (evaluator.getValue() instanceof RawType) {
                            RawType rawType = (RawType) evaluator.getValue();
                            System.out.println("\nraw assignment:\n" + rawType);
                            Item assignment = rawType.getParsedItem(assignmentDef);
                            System.out.println("\nassignment:\n" + assignment.debugDump());
                            assignmentValuesFound++;
                        }
                    }
                }
            }
        }
        assertEquals("wrong # of assignment values found in mapping", 2, assignmentValuesFound);
    }

    private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
            int maxOccurs) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }

    // todo deduplicate with TestUtil
    private static void waitForCompletion(List<Thread> threads, long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            boolean anyAlive = threads.stream().anyMatch(Thread::isAlive);
            if (!anyAlive) {
                break;
            } else {
                Thread.sleep(100);
            }
        }
    }
}

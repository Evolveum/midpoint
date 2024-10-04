/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.parseObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.merger.SimpleObjectMergeOperation;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestInitialObjects extends AbstractUnitTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestInitialObjects.class);

    private static final File INITIAL_OBJECTS_DIR = new File("./src/main/resources/initial-objects");

    record FileMergeResult<O extends ObjectType>(File file, PrismObject<O> before, PrismObject<O> after, boolean problem) {
    }

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void mergeInitialObjects() throws Exception {
        List<FileMergeResult> results = new ArrayList<>();

        testMergeOnFiles(new File[]{INITIAL_OBJECTS_DIR}, results);

        long success = results.stream().filter(r -> !r.problem()).count();
        long failed = results.size() - success;

        LOGGER.info("Success: " + success + ", Failed: " + failed + ", Total: " + results.size());

        results.stream()
                .filter(r -> r.problem())
                .forEach(r -> LOGGER.error("Problem with file: " + r.file()
//                        + "\nfirst:\n" + r.before().debugDump(1)
//                        + "\nsecond:\n" + r.after().debugDump(1)
                        + "\ndelta:\n" + r.before().diff(r.after()).debugDump(1)));

        Assertions.assertThat(failed)
                .isEqualTo(0L)
                .withFailMessage("Failed merge for " + failed + " files");
    }

    public void testMergeOnFiles(File[] files, List<FileMergeResult> results) throws SchemaException, IOException, ConfigurationException {
        for (File file : files) {
            if (file.isDirectory()) {
                testMergeOnFiles(FileUtils.listFiles(file, new String[]{"json", "xml", "yaml"}, false).toArray(new File[0]), results);
            } else {
                FileMergeResult result = testMergeOnFile(file);
                results.add(result);
            }
        }
    }

    private <O extends ObjectType> FileMergeResult testMergeOnFile(File file) throws SchemaException, IOException, ConfigurationException {
        PrismObject<O> object = getPrismContext().parseObject(file);
        PrismObject<O> objectBeforeMerge = object.cloneComplex(CloneStrategy.REUSE);

        LOGGER.trace("Object before merge:\n{}", objectBeforeMerge.debugDump());

        SimpleObjectMergeOperation.merge(object, objectBeforeMerge);

        LOGGER.trace("Object after merge:\n{}", object.debugDump());

        boolean problem = !object.equivalent(objectBeforeMerge);

        return new FileMergeResult(file, objectBeforeMerge, object, problem);
    }

    /**
     * Not a real test.
     *
     * Lists multi-value complex properties and containers from all schema definitions.
     * Also checks initial objects whether they contain such properties/containers.
     *
     * Used to generate CSV for documentation/development purposes only.
     */
    @Test(enabled = false)
    public void listComplexPropertiesAndMultiValueContainers() throws Exception {
        List<ItemDefinition<?>> initialObjects = new ArrayList<>();
        Set<Definition> visited = new HashSet<>();

        Collection<File> files = FileUtils.listFiles(INITIAL_OBJECTS_DIR, new String[] { "xml" }, true);
        for (File file : files) {
            PrismObject<?> object = parseObject(file);
            object.accept(visitable -> {
                if (visitable instanceof Item<?, ?> item) {
                    processDefinition(item.getDefinition(), initialObjects, visited, false);
                }
            });
        }

        Set<QName> initialObjectsTypes = initialObjects.stream()
                .map(ItemDefinition::getTypeName)
                .collect(Collectors.toSet());

        List<ItemDefinition<?>> fullSchema = new ArrayList<>();
        visited.clear();

        SchemaRegistry registry = PrismTestUtil.getPrismContext().getSchemaRegistry();
        for (PrismSchema schema : registry.getSchemas()) {
            Collection<Definition> definitions = schema.getDefinitions();

            definitions.stream()
                    .filter(d -> d instanceof PrismObjectDefinition<?>)
                    .forEach(d -> processDefinition(d, fullSchema, visited, true));
        }

        List<String> dump = fullSchema.stream()
                .map(id -> map(id, initialObjectsTypes.contains(id.getTypeName())))
                .distinct()
                .sorted()
                .toList();

        System.out.println(StringUtils.joinWith(";",
                "Definition class",
                "Type QName",
                "Primitive",
                "Deprecated",
                "Removed",
                "Experimental",
                "Initial Object",
                "Merger",
                "NaturalKey"
        ));
        dump.forEach(System.out::println);
    }

    private void processDefinition(Definition def, List<ItemDefinition<?>> result, Set<Definition> visited, boolean recursive) {
        if (visited.contains(def)) {
            return;
        }
        visited.add(def);

        if (!(def instanceof ItemDefinition<?> id)) {
            return;
        }

        if (id instanceof PrismPropertyDefinition<?> ppd) {
            if (ppd.isMultiValue()) {
                Class type = ppd.getTypeClass();
                if (!isPrimitive(type)
                        && !PolyString.class.equals(type)
                        && !PolyStringType.class.equals(type)
                        && !ItemPathType.class.equals(type)
                        && !QName.class.equals(type)
                        && !ProtectedStringType.class.equals(type)
                        && !Duration.class.equals(type)) {
                    result.add(id);
                }
            }
        } else if (id instanceof PrismContainerDefinition<?> pcd) {
            if (pcd.isMultiValue()) {
                result.add(id);
            }

            if (recursive) {
                pcd.getDefinitions().forEach(d -> processDefinition(d, result, visited, recursive));
            }
        }
    }

    private String map(ItemDefinition id, boolean initialObject) {
        Class<?> clazz = id.getTypeClass();

        return StringUtils.joinWith(";",
                id.getClass().getSimpleName(),
                id.getTypeName().toString()
                        .replace("http://midpoint.evolveum.com/xml/ns/public", "..")
                        .replace("http://prism.evolveum.com/xml/ns/public", "prism.."),
                isPrimitive(clazz) ? "PRIMITIVE" : "",
                id.isDeprecated() ? "DEPRECATED" : "",
                id.isRemoved() ? "REMOVED" : "",
                id.isExperimental() ? "EXPERIMENTAL" : "",
                initialObject ? "INITIAL" : "",
                id.getMergerIdentifier(),
                id.getNaturalKeyConstituents()
        );
    }

    private boolean isPrimitive(Class<?> clazz) {
        return clazz != null && (clazz.isPrimitive()
                || ClassUtils.wrapperToPrimitive(clazz) != null
                || clazz.equals(String.class)
                || Enum.class.isAssignableFrom(clazz));
    }
}

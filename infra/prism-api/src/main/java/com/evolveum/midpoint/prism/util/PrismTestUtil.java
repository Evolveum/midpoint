/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that statically instantiates the prism contexts and provides convenient static version of the PrismContext
 * and processor classes.
 *
 * This is usable for tests. DO NOT use this in the main code. Although it is placed in "main" for convenience,
 * is should only be used in tests.
 *
 * @author semancik
 */
public class PrismTestUtil {

    private static final Trace LOGGER = TraceManager.getTrace(PrismTestUtil.class);

    private static final QName DEFAULT_ELEMENT_NAME = new QName("http://midpoint.evolveum.com/xml/ns/test/whatever-1.xsd", "whatever");

    private static final String OBJECT_TITLE_OUT_PREFIX = "\n*** ";
    private static final String OBJECT_TITLE_LOG_PREFIX = "*** ";

    private static PrismContext prismContext;
    private static PrismContextFactory prismContextFactory;

    public static void resetPrismContext(PrismContextFactory newPrismContextFactory) throws SchemaException, SAXException, IOException {
        if (prismContextFactory == newPrismContextFactory) {
            // Exactly the same factory instance, nothing to do.
            return;
        }
        setFactory(newPrismContextFactory);
        resetPrismContext();
    }

    public static void setFactory(PrismContextFactory newPrismContextFactory) {
        PrismTestUtil.prismContextFactory = newPrismContextFactory;
    }

    public static void resetPrismContext() throws SchemaException, SAXException, IOException {
        prismContext = createInitializedPrismContext();
    }

    public static PrismContext createPrismContext() throws SchemaException, FileNotFoundException {
        if (prismContextFactory == null) {
            throw new IllegalStateException("Cannot create prism context, no prism factory is set");
        }
        PrismContext prismContext = prismContextFactory.createPrismContext();
        prismContext.setExtraValidation(true);
        return prismContext;
    }

    public static PrismContext createInitializedPrismContext() throws SchemaException, SAXException, IOException {
        PrismContext newPrismContext = createPrismContext();
        newPrismContext.initialize();
        return newPrismContext;
    }

    public static PrismContext getPrismContext() {
        if (prismContext == null) {
            throw new IllegalStateException("Prism context is not set in PrismTestUtil. Maybe a missing call to resetPrismContext(..) in test initialization?");
        }
        return prismContext;
    }

    public static void setPrismContext(PrismContext prismContext) {
        PrismTestUtil.prismContext = prismContext;
    }

    public static SchemaRegistry getSchemaRegistry() {
        return prismContext.getSchemaRegistry();
    }

    // ==========================
    // == parsing
    // ==========================

    public static <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException, IOException {
        return getPrismContext().parseObject(file);
    }

    public static <T extends Objectable> PrismObject<T> parseObject(String xmlString) throws SchemaException {
        return getPrismContext().parseObject(xmlString);
    }

    public static <T extends Objectable> T parseObjectable(File file, Class<T> clazz) throws SchemaException, IOException {
        return (T) parseObject(file).asObjectable();
    }

    public static List<PrismObject<? extends Objectable>> parseObjects(File file) throws SchemaException, IOException {
        return getPrismContext().parserFor(file).parseObjects();
    }

    // ==========================
    // == Serializing
    // ==========================

    public static String serializeObjectToString(PrismObject<? extends Objectable> object, String language) throws SchemaException {
        return getPrismContext().serializerFor(language).serialize(object);
    }

    public static String serializeObjectToString(PrismObject<? extends Objectable> object) throws SchemaException {
        return getPrismContext().xmlSerializer().serialize(object);
    }

    public static String serializeAtomicValue(Object object, QName elementName) throws SchemaException {
        return getPrismContext().xmlSerializer().serializeRealValue(object, elementName);
    }

    public static String serializeAnyData(Object o, QName defaultRootElementName) throws SchemaException {
        return getPrismContext().xmlSerializer().serializeAnyData(o, defaultRootElementName);
    }

    public static String serializeJaxbElementToString(JAXBElement element) throws SchemaException {
        return serializeAnyData(element.getValue(), element.getName());
    }

    public static String serializeAnyDataWrapped(Object o) throws SchemaException {
        return serializeAnyData(o, DEFAULT_ELEMENT_NAME);
    }



    // ==========================
    // == Here was parsing from JAXB.
    // ==========================

    public static <T> T parseAtomicValue(File file, QName type) throws SchemaException, IOException {
        return getPrismContext().parserFor(file).type(type).parseRealValue();
    }

    public static <T> T parseAtomicValue(String data, QName type) throws SchemaException {
        return getPrismContext().parserFor(data).type(type).parseRealValue();
    }

    public static <T> T parseAtomicValueCompat(String data, QName type) throws SchemaException {
        return getPrismContext().parserFor(data).compat().type(type).parseRealValue();
    }

    public static <T> T parseAnyValue(File file) throws SchemaException, IOException {
        return getPrismContext().parserFor(file).parseRealValue();
    }

    public static <T extends Objectable> PrismObjectDefinition<T> getObjectDefinition(Class<T> compileTimeClass) {
        return getSchemaRegistry().findObjectDefinitionByCompileTimeClass(compileTimeClass);
    }

    public static PolyString createPolyString(String orig) {
        PolyString polyString = new PolyString(orig);
        polyString.recompute(getPrismContext().getDefaultPolyStringNormalizer());
        return polyString;
    }

    public static PolyString createPolyString(String orig, String norm) {
        return new PolyString(orig, norm);
    }

    public static PolyStringType createPolyStringType(String string) {
        return new PolyStringType(createPolyString(string));
    }

    public static PolyStringType createPolyStringType(String orig, String norm) {
        return new PolyStringType(createPolyString(orig, norm));
    }

    public static SearchFilterType unmarshalFilter(File file) throws Exception {
        return prismContext.parserFor(file).parseRealValue(SearchFilterType.class);
    }

    public static ObjectFilter getFilterCondition(ObjectFilter filter, int index) {
        if (!(filter instanceof NaryLogicalFilter)) {
            throw new IllegalArgumentException("Filter not an instance of n-ary logical filter.");
        }
        return ((LogicalFilter) filter).getConditions().get(index);
    }

    public static void display(String title, DebugDumpable dumpable) {
        System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
        System.out.println(dumpable == null ? "null" : dumpable.debugDump(1));
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title  + "\n"
                + (dumpable == null ? "null" : dumpable.debugDump(1)));
    }

    public static void display(String title, Object value) {
        System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
        System.out.println(PrettyPrinter.prettyPrint(value));
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n"
                + PrettyPrinter.prettyPrint(value));
    }

    public static void displayCollection(String title, Collection<?> collection) {
        System.out.println(OBJECT_TITLE_OUT_PREFIX + title + " (" + collection.size() + ")");
        for (Object object : collection) {
            System.out.println(" - " + PrettyPrinter.prettyPrint(object));
        }
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + " (" + collection.size() + ")" + "\n" +
                collection.stream().map(object -> PrettyPrinter.prettyPrint(object)).collect(Collectors.joining("\n")));
    }

    public static void displayQuery(ObjectQuery query) {
        LOGGER.trace("object query:\n{}\n", query);
        System.out.println("object query:\n" + query + "\n");
        if (query != null) {
            LOGGER.trace("object query debug dump:\n{}\n", query.debugDump());
            System.out.println("object query debug dump:\n" + query.debugDump() + "\n");
        }
    }

    public static void displayQueryXml(String xml) {
        LOGGER.trace("object query XML:\n{}", xml);
        System.out.println("object query XML:\n" + xml);
    }

    public static void displayText(String text) {
        LOGGER.trace(text);
        System.out.println(text);
    }

    public static void displayQueryType(QueryType queryType) throws SchemaException {
        displaySearchFilterType(queryType.getFilter());
    }

    public static void displaySearchFilterType(SearchFilterType filterType) throws SchemaException {
        MapXNode mapXNode = filterType.getFilterClauseXNode();

        String dumpX = mapXNode.debugDump();
        LOGGER.info(dumpX);
        System.out.println("filter clause xnode:\n" + dumpX + "\n");

        String dumpXml = prismContext.xmlSerializer().serialize(
                prismContext.xnodeFactory().root(new QName("filterClauseXNode"), mapXNode));
        System.out.println("filter clause xnode serialized:\n" + dumpXml + "\n");
    }

    public static ParsingContext createDefaultParsingContext() {
        return getPrismContext().getDefaultParsingContext();
    }
}

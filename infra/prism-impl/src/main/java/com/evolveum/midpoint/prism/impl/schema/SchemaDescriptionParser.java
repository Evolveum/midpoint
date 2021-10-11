/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.schema;

import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for SchemaDescription objects. TODO Rework, along with SchemaDescriptionImpl.
 */
class SchemaDescriptionParser {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaDescription.class);

    public static final String NS_WSDL11 = "http://schemas.xmlsoap.org/wsdl/";

    public static final QName QNAME_DEFINITIONS = new QName(NS_WSDL11, "definitions");

    public static final QName QNAME_TYPES = new QName(NS_WSDL11, "types");

    static SchemaDescriptionImpl parseResource(String resourcePath) throws SchemaException {
        SchemaDescriptionImpl desc = new SchemaDescriptionImpl("system resource " + resourcePath, resourcePath);
        desc.setStreamable(() -> {
            InputStream inputStream = SchemaRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IllegalStateException("Cannot fetch system resource for schema " + resourcePath);
            }
            return inputStream;
        });
        parseFromInputStream(desc);
        return desc;
    }

    static List<SchemaDescriptionImpl> parseWsdlResource(String resourcePath) throws SchemaException {
        List<SchemaDescriptionImpl> schemaDescriptions = new ArrayList<>();

        InputStream inputStream = SchemaRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Cannot fetch system resource for schema " + resourcePath);
        }
        Node node;
        try {
            node = DOMUtil.parse(inputStream);
        } catch (IOException e) {
            throw new SchemaException("Cannot parse schema from system resource " + resourcePath, e);
        }
        Element rootElement = node instanceof Element ? (Element) node : DOMUtil.getFirstChildElement(node);
        QName rootElementQName = DOMUtil.getQName(rootElement);
        if (QNAME_DEFINITIONS.equals(rootElementQName)) {
            Element types = DOMUtil.getChildElement(rootElement, QNAME_TYPES);
            if (types == null) {
                LOGGER.warn("No <types> section in WSDL document in system resource " + resourcePath);
                return schemaDescriptions;
            }
            List<Element> schemaElements = DOMUtil.getChildElements(types, DOMUtil.XSD_SCHEMA_ELEMENT);
            if (schemaElements.isEmpty()) {
                LOGGER.warn("No schemas in <types> section in WSDL document in system resource " + resourcePath);
                return schemaDescriptions;
            }
            int number = 1;
            for (Element schemaElement : schemaElements) {
                SchemaDescriptionImpl desc = new SchemaDescriptionImpl("schema #" + (number++) + " in system resource " + resourcePath, null);
                desc.setNode(schemaElement);
                fetchBasicInfoFromSchema(desc);
                schemaDescriptions.add(desc);
                LOGGER.trace("Schema registered from {}", desc.getSourceDescription());
            }
            return schemaDescriptions;
        } else {
            throw new SchemaException("WSDL system resource " + resourcePath + " does not start with wsdl:definitions element");
        }
    }

    static SchemaDescriptionImpl parseInputStream(InputStream input, String description) throws SchemaException {
        if (input == null) {
            throw new NullPointerException("Input stream must not be null");
        }
        SchemaDescriptionImpl desc = new SchemaDescriptionImpl("inputStream " + description, null);
        desc.setStreamable(() -> input);
        parseFromInputStream(desc);
        return desc;
    }

    public static SchemaDescriptionImpl parseFile(File file) throws SchemaException {
        SchemaDescriptionImpl desc = new SchemaDescriptionImpl("file " + file.getPath(), file.getPath());
        desc.setStreamable(() -> {
            InputStream inputStream;
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Cannot fetch file for schema " + file, e);
            }
            return inputStream;
        });
        parseFromInputStream(desc);
        return desc;
    }

    private static void parseFromInputStream(SchemaDescriptionImpl desc) throws SchemaException {
        InputStream inputStream = desc.openInputStream();
        try {
            desc.setNode(DOMUtil.parse(inputStream));
        } catch (IOException e) {
            throw new SchemaException("Cannot parse schema from " + desc.getSourceDescription(), e);
        }
        fetchBasicInfoFromSchema(desc);
    }

    static SchemaDescriptionImpl parseNode(Node node, String sourceDescription) throws SchemaException {
        SchemaDescriptionImpl desc = new SchemaDescriptionImpl(sourceDescription, null);
        desc.setNode(node);
        fetchBasicInfoFromSchema(desc);
        return desc;
    }

    private static void fetchBasicInfoFromSchema(SchemaDescriptionImpl desc) throws SchemaException {
        Element rootElement = desc.getDomElement();
        if (DOMUtil.XSD_SCHEMA_ELEMENT.equals(DOMUtil.getQName(rootElement))) {
            String targetNamespace = DOMUtil.getAttribute(rootElement, DOMUtil.XSD_ATTR_TARGET_NAMESPACE);
            if (targetNamespace != null) {
                desc.setNamespace(targetNamespace);
            } else {
                throw new SchemaException("Schema " + desc.getSourceDescription() + " does not have targetNamespace attribute");
            }
        } else {
            throw new SchemaException("Schema " + desc.getSourceDescription() + " does not start with xsd:schema element");
        }
    }
}

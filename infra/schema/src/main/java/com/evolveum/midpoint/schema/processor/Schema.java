/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.processor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Schema as a collection of definitions. This is a midPoint-specific view of
 * schema definition. It is just a collection of definitions grouped under a
 * specific namespace.
 * 
 * The schema and all the public classes in this package define a schema
 * meta-model. It is supposed to be used for run-time schema interpretation. It
 * will not be a convenient tool to work with static data model objects such as
 * user or role. But it is needed for interpreting dynamic schemas for resource
 * objects, extensions and so on.
 * 
 * Schema is immutable.
 * 
 * @author Radovan Semancik
 * 
 */
public class Schema {

	private String namespace;
	private Set<Definition> definitions;
	static final String INDENT = "  ";
	
	public Schema(String namespace) {
		if (StringUtils.isEmpty(namespace)) {
			throw new IllegalArgumentException("Namespace can't be null or empty.");
		}
		this.namespace = namespace;
	}

	/**
	 * Returns schema namespace.
	 * 
	 * All schema definitions are placed in the returned namespace.
	 * 
	 * @return schema namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns set of definitions.
	 * 
	 * The set contains all definitions of all types that were parsed. Order of
	 * definitions is insignificant.
	 * 
	 * @return set of definitions
	 */
	public Set<Definition> getDefinitions() {
		if (definitions == null) {
			definitions = new HashSet<Definition>();
		}
		return definitions;
	}

//	public static Schema parse(String text) throws SchemaProcessorException {
//		if (StringUtils.isEmpty(text)) {
//			throw new IllegalArgumentException("String argument (schema) can't be empty.");
//		}
//
//		try {
//			return parse(new ByteArrayInputStream(text.getBytes("utf-8")));
//		} catch (UnsupportedEncodingException ex) {
//			throw new SchemaProcessorException("Unsupported encoding used: " + ex.getMessage());
//		}
//	}
//
//	public static Schema parse(InputStream input) throws SchemaProcessorException {
//		if (input == null) {
//			throw new IllegalArgumentException("Input stream must not be null.");
//		}
//
//		DomToSchemaProcessor processor = new DomToSchemaProcessor();
//		return processor.parseDom(input);
//	}	

	public static Schema parse(Element schema) throws SchemaProcessorException {
		if (schema == null) {
			throw new IllegalArgumentException("Input stream must not be null.");
		}
		
		DomToSchemaProcessor processor = new DomToSchemaProcessor();
		return processor.parseDom(schema);
	}

	public static Document parseSchema(Schema schema) throws SchemaProcessorException {
		if (schema == null) {
			throw new IllegalArgumentException("Schema can't be null.");
		}

		SchemaToDomProcessor processor = new SchemaToDomProcessor();
		return processor.parseSchema(schema);
	}

	// TODO: Methods for searching the schema, such as findDefinitionByName(),
	// etc.

	/**
	 * Finds a PropertyContainerDefinition by the type name.
	 * 
	 * @param typeName
	 *            property container type name
	 * @return found property container definition
	 * @throws IllegalStateException
	 *             if more than one definition is found
	 */
	public PropertyContainerDefinition findContainerDefinitionByType(QName typeName) {
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definition instanceof PropertyContainerDefinition
					&& typeName.equals(definition.getTypeName())) {
				return (PropertyContainerDefinition) definition;
			}
		}
		return null;
	}
	
	public String debugDump() {
		StringBuilder sb = new StringBuilder();
		sb.append("Schema ns=");
		sb.append(getNamespace());
		sb.append("\n");
		for (Definition def: getDefinitions()) {
			sb.append(def.debugDump(1));
		}
		return sb.toString();
	}
}

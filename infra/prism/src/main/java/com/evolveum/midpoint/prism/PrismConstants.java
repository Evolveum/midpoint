/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import javax.xml.namespace.QName;


/**
 * @author semancik
 *
 */
public class PrismConstants {

	public static final String EXTENSION_LOCAL_NAME = "extension";
	public static final String NAME_LOCAL_NAME = "name";

	public static final String ATTRIBUTE_ID_LOCAL_NAME = "id";
	public static final String ATTRIBUTE_OID_LOCAL_NAME = "oid";
	public static final String ATTRIBUTE_VERSION_LOCAL_NAME = "version";
	public static final String ATTRIBUTE_REF_TYPE_LOCAL_NAME = "type";
	public static final String ATTRIBUTE_RELATION_LOCAL_NAME = "relation";
	
	public static final String ELEMENT_DESCRIPTION_LOCAL_NAME = "description";
	public static final String ELEMENT_FILTER_LOCAL_NAME = "filter";
	
	public static final String NS_PREFIX = "http://prism.evolveum.com/xml/ns/public/";
	public static final String NS_ANNOTATION = NS_PREFIX + "annotation-3";
	public static final String PREFIX_NS_ANNOTATION = "a";
	public static final String NS_TYPES = NS_PREFIX + "types-3";
	public static final String PREFIX_NS_TYPES = "t";
	public static final String NS_QUERY = NS_PREFIX + "query-3";
	public static final String PREFIX_NS_QUERY = "q";
	
	public static final String NS_MATCHING_RULE = NS_PREFIX + "matching-rule-3";
	public static final String PREFIX_NS_MATCHING = "mr";
	
	public static final String NS_PREFIX_CRYPTO = NS_PREFIX + "crypto/";
	public static final String NS_PREFIX_CRYPTO_ALGORITHM = NS_PREFIX_CRYPTO + "algorithm/";
	public static final String NS_CRYPTO_ALGORITHM_PBKD = NS_PREFIX_CRYPTO_ALGORITHM + "pbkd-3";

	// Annotations

	public static final QName A_PROPERTY_CONTAINER = new QName(NS_ANNOTATION, "container");
	public static final QName A_OBJECT = new QName(NS_ANNOTATION, "object");

	public static final QName A_DEFAULT_NAMESPACE = new QName(NS_ANNOTATION, "defaultNamespace");
	public static final QName A_IGNORED_NAMESPACE = new QName(NS_ANNOTATION, "ignoredNamespace");

	public static final QName A_TYPE = new QName(NS_ANNOTATION, "type");
	public static final QName A_DISPLAY_NAME = new QName(NS_ANNOTATION, "displayName");
	public static final QName A_DISPLAY_ORDER = new QName(NS_ANNOTATION, "displayOrder");
	public static final QName A_HELP = new QName(NS_ANNOTATION, "help");	
	public static final QName A_ACCESS = new QName(NS_ANNOTATION, "access");
	public static final String A_ACCESS_CREATE = "create";
	public static final String A_ACCESS_UPDATE = "update";
	public static final String A_ACCESS_READ = "read";
	public static final QName A_INDEXED = new QName(NS_ANNOTATION, "indexed");
	public static final QName A_IGNORE = new QName(NS_ANNOTATION, "ignore");
	public static final QName A_OPERATIONAL = new QName(NS_ANNOTATION, "operational");
	public static final QName A_EXTENSION = new QName(NS_ANNOTATION, "extension");
	public static final QName A_EXTENSION_REF = new QName(NS_ANNOTATION, "ref");
	public static final QName A_OBJECT_REFERENCE = new QName(NS_ANNOTATION, "objectReference");
	public static final QName A_OBJECT_REFERENCE_TARGET_TYPE = new QName(NS_ANNOTATION, "objectReferenceTargetType");
	public static final QName A_COMPOSITE = new QName(NS_ANNOTATION, "composite");
	public static final QName A_DEPRECATED = new QName(NS_ANNOTATION, "deprecated");
	public static final QName A_LABEL = new QName(NS_ANNOTATION, "label");
	public static final QName A_MATCHING_RULE = new QName(NS_ANNOTATION, "matchingRule");
	public static final QName A_EMPHASIZED = new QName(NS_ANNOTATION, "emphasized");
	public static final QName A_VALUE_ENUMERATION_REF = new QName(NS_ANNOTATION, "valueEnumerationRef");
	public static final QName A_HETEROGENEOUS_LIST_ITEM = new QName(NS_ANNOTATION, "heterogeneousListItem");

	public static final QName SCHEMA_DOCUMENTATION = new QName(W3C_XML_SCHEMA_NS_URI, "documentation");
	public static final QName SCHEMA_APP_INFO = new QName(W3C_XML_SCHEMA_NS_URI, "appinfo");
	
	public static final QName A_MAX_OCCURS = new QName(NS_ANNOTATION, "maxOccurs");
	public static final String MULTIPLICITY_UNBONUNDED = "unbounded";
	
	public static final QName A_NAMESPACE = new QName(NS_ANNOTATION, "namespace");
	public static final String A_NAMESPACE_PREFIX = "prefix";
	public static final String A_NAMESPACE_URL = "url";
	
	//Query constants
	public static final QName Q_OID = new QName(NS_QUERY, "oid");
	public static final QName Q_TYPE = new QName(NS_QUERY, "type");
	public static final QName Q_RELATION = new QName(NS_QUERY, "relation");
	public static final QName Q_VALUE = new QName(NS_QUERY, "value");
	public static final QName Q_ORDER_BY = new QName(NS_QUERY, "orderBy");
	public static final QName Q_ANY = new QName(NS_QUERY, "any");

	// Path constants
	public static final String T_PARENT_LOCAL_PART = "parent";
	public static final QName T_PARENT = new QName(NS_TYPES, T_PARENT_LOCAL_PART);
	public static final QName T_OBJECT_REFERENCE = new QName(NS_TYPES, "objectReference");
	public static final String T_ID_LOCAL_PART = "id";
	public static final QName T_ID = new QName(NS_TYPES, T_ID_LOCAL_PART);

	// Misc
	
	public static final Class DEFAULT_VALUE_CLASS = String.class;
	
	public static final QName POLYSTRING_TYPE_QNAME = new QName(NS_TYPES, "PolyStringType");
	public static final QName POLYSTRING_ELEMENT_ORIG_QNAME = new QName(NS_TYPES, "orig");
	public static final QName POLYSTRING_ELEMENT_NORM_QNAME = new QName(NS_TYPES, "norm");

    // a bit of hack: by this local name we know if a object is a reference (c:ObjectReferenceType)
    public static final String REFERENCE_TYPE_NAME = "ObjectReferenceType";
    
    public static final boolean EQUALS_DEFAULT_IGNORE_METADATA = true;
    public static final boolean EQUALS_DEFAULT_IS_LITERAL = false;
    
    public static final String EXPRESSION_LOCAL_PART = "expression";
}

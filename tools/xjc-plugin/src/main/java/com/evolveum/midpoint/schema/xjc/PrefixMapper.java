/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.xjc;

import javax.xml.XMLConstants;

/**
 * @author lazyman
 */
public enum PrefixMapper {

    W("http://midpoint.evolveum.com/xml/ns/public/communication/workflow-1.xsd", "WORKFLOW"),

    C("http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd", "COMMON"),

    T("http://prism.evolveum.com/xml/ns/public/types-2", "TYPES"),

    R("http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd", "RESOURCE"),

    R_CAP("http://midpoint.evolveum.com/xml/ns/public/resource/capabilities-1.xsd", "CAPABILITIES"),

    A("http://midpoint.evolveum.com/xml/ns/public/common/annotation-1.xsd", "ANNOTATION"),

    S("http://midpoint.evolveum.com/xml/ns/public/model/situation-1.xsd", "SITUATION"),

    ICF_S("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-1.xsd", "ICF_SCHEMA"),

    ICF_C("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-1.xsd", "ICF_CONFIGURATION"),

    ENC("http://www.w3.org/2001/04/xmlenc#", "XML_ENC"),

    DSIG("http://www.w3.org/2000/09/xmldsig#", "XML_DSIG"),

    XSD(XMLConstants.W3C_XML_SCHEMA_NS_URI, "XSD");

    public static final String DEFAULT_PREFIX = "O_";
    private String namespace;
    private String name;

    private PrefixMapper(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getNamespaceName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return this.name() + "_";
    }

    public static String getPrefix(String namespace) {
        for (PrefixMapper mapper : PrefixMapper.values()) {
            if (mapper.getNamespace().equals(namespace)) {
                return mapper.getPrefix();
            }
        }

        return DEFAULT_PREFIX;
    }

    public static String getNamespace(String prefix) {
        for (PrefixMapper mapper : PrefixMapper.values()) {
            if (mapper.getPrefix().equals(prefix)) {
                return mapper.getNamespace();
            }
        }

        return null;
    }
}

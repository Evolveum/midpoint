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

/**
 * @author lazyman
 */
public enum PrefixMapper {

    W("http://midpoint.evolveum.com/xml/ns/public/communication/workflow-1.xsd", "W_", "WORKFLOW"),

    C("http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd", "C_", "COMMON"),

    R("http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd", "R_", "RESOURCE"),

    R_CAP("http://midpoint.evolveum.com/xml/ns/public/resource/capabilities-1.xsd", "R_CAP_", "CAPABILITIES"),

    A("http://midpoint.evolveum.com/xml/ns/public/common/annotation-1.xsd", "A_", "ANNOTATION"),

    S("http://midpoint.evolveum.com/xml/ns/public/model/situation-1.xsd", "S_", "SITUATION"),

    ICF_S("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-1.xsd", "ICF_S_", "ICF_RESOURCE"),

    ICF_C("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-1.xsd", "ICF_C_", "ICF_SCHEMA"),

    ENC("http://www.w3.org/2001/04/xmlenc#", "ENC_", "XML_ENC"),

    DSIG("http://www.w3.org/2000/09/xmldsig#", "DSIG_", "XML_DSIG");

    public static final String DEFAULT_PREFIX = "O_";
    private String namespace;
    private String prefix;
    private String name;

    private PrefixMapper(String namespace, String prefix, String name) {
        this.namespace = namespace;
        this.prefix = prefix;
        this.name = name;
    }

    public String getNamespaceName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
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

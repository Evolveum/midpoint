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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.util.jaxb;

/**
 *
 * @author lazyman
 */
public enum NamespaceEnum {
    
    IDM("http://midpoint.evolveum.com/xml/ns/identity/1#", ""),
    C("http://midpoint.evolveum.com/xml/ns/common/1#", "");
    

    private String namespace;

    private String prefix;

    private NamespaceEnum(String namespace, String prefix) {
        this.namespace = namespace;
        this.prefix = prefix;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return prefix + ":" + namespace;
    }
}

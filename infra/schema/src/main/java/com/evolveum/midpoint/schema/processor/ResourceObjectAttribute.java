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

import javax.xml.namespace.QName;
import java.util.Set;

/**
 * Resource Object Attribute is a Property of Resource Object. All that applies
 * to property applies also to attribute, e.g. only a whole attributes can be
 * changed, they may be simple or complex types, they should be representable in
 * XML, etc. In addition, attribute definition may have some annotations that
 * suggest its purpose and use on the Resource.
 * <p/>
 * Resource Object Attribute understands resource-specific annotations such as
 * native attribute name.
 * <p/>
 * Resource Object Attribute is mutable.
 *
 * @author Radovan Semancik
 */
public class ResourceObjectAttribute extends Property {

//    /**
//     * Default constructor.
//     * The constructors should be used only occasionally (if used at all).
//     * Use the factory methods in the ResourceObjectDefintion instead.
//     */
//    public ResourceObjectAttribute() {
//        super();
//    }
//
//    /**
//     * The constructors should be used only occasionally (if used at all).
//     * Use the factory methods in the ResourceObjectDefintion instead.
//     *
//     * @param name       attribute name (element name)
//     * @param definition attribute definition (schema)
//     * @param values     initial attribute values
//     */
//    public ResourceObjectAttribute(QName name, PropertyDefinition definition,
//                                   Set<PropertyValue<Object>> values) {
//        super(name, definition, values);
//    }
//
//    /**
//     * The constructors should be used only occasionally (if used at all).
//     * Use the factory methods in the ResourceObjectDefintion instead.
//     *
//     * @param name       attribute name (element name)
//     * @param definition attribute definition (schema)
//     */
//    public ResourceObjectAttribute(QName name, PropertyDefinition definition) {
//        super(name, definition);
//    }

    public ResourceObjectAttribute(QName name, PropertyDefinition definition, Object element, PropertyPath parentPath) {
        super(name, definition, element, parentPath);
    }

//    /**
//     * The constructors should be used only occasionally (if used at all).
//     * Use the factory methods in the ResourceObjectDefintion instead.
//     *
//     * @param name attribute name (element name)
//     */
//    public ResourceObjectAttribute(QName name) {
//        super(name);
//    }

    public ResourceObjectAttributeDefinition getDefinition() {
        return (ResourceObjectAttributeDefinition) super.getDefinition();
    }

    /**
     * Returns native attribute name.
     * <p/>
     * Native name of the attribute is a name as it is used on the resource or
     * as seen by the connector. It is used for diagnostics purposes and may be
     * used by the connector itself. As the attribute names in XSD have to
     * comply with XML element name limitations, this may be the only way how to
     * determine original attribute name.
     * <p/>
     * Returns null if native attribute name is not set or unknown.
     * <p/>
     * The name should be the same as the one used by the resource, if the
     * resource supports naming of attributes. E.g. in case of LDAP this
     * annotation should contain "cn", "givenName", etc. If the resource is not
     * that flexible, the native attribute names may be hardcoded (e.g.
     * "username", "homeDirectory") or may not be present at all.
     *
     * @return native attribute name
     */
    public String getNativeAttributeName() {
        return getDefinition() == null ? null : getDefinition()
                .getNativeAttributeName();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "ROA";
    }

}
/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;

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
public class ResourceAttributeImpl<T> extends PrismPropertyImpl<T> implements ResourceAttribute<T> {

    private static final long serialVersionUID = -6149194956029296486L;

    public ResourceAttributeImpl(QName name, ResourceAttributeDefinition<T> definition, PrismContext prismContext) {
        super(name, definition, prismContext);
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

    @Override
    public ResourceAttributeDefinition<T> getDefinition() {
        return (ResourceAttributeDefinition<T>) super.getDefinition();
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
    @Override
    public String getNativeAttributeName() {
        return getDefinition() == null ? null : getDefinition()
                .getNativeAttributeName();
    }

    @Override
    public ResourceAttribute<T> clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public ResourceAttribute<T> cloneComplex(CloneStrategy strategy) {
        ResourceAttributeImpl<T> clone = new ResourceAttributeImpl<>(getElementName(), getDefinition(), getPrismContext());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, ResourceAttributeImpl<T> clone) {
        super.copyValues(strategy, clone);
        // Nothing to copy
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "RA";
    }

}

/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;

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
public class ResourceAttribute<T> extends PrismProperty<T> {

    private static final long serialVersionUID = -6149194956029296486L;

    public ResourceAttribute(QName name, ResourceAttributeDefinition<T> definition, PrismContext prismContext) {
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
    public String getNativeAttributeName() {
        return getDefinition() == null ? null : getDefinition()
                .getNativeAttributeName();
    }

    @Override
	public ResourceAttribute<T> clone() {
    	ResourceAttribute<T> clone = new ResourceAttribute<T>(getElementName(), getDefinition(), getPrismContext());
    	copyValues(clone);
    	return clone;
	}

	protected void copyValues(ResourceAttribute<T> clone) {
		super.copyValues(clone);
		// Nothing to copy
	}

	/**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "RA";
    }

}

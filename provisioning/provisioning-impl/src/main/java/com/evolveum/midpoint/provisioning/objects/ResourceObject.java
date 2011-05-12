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

package com.evolveum.midpoint.provisioning.objects;

import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ResourceObject {

    public static final String code_id = "$Id$";

    /**
     * Name of the {@link ResourceObject}.
     */
    private final ResourceObjectDefinition definition;

    /**
     * Values of the {@link ResourceObject}.
     */
    private final List<ResourceAttribute> values;

    public ResourceObject(ResourceObjectDefinition definition) {
        this.definition = definition;
        this.values = new ArrayList<ResourceAttribute>();
    }

    public ResourceObject(ResourceObjectDefinition definition, List<ResourceAttribute> values) {
        this.definition = definition;
        this.values = values;
    }

    public ResourceObjectDefinition getDefinition() {
        return definition;
    }

    public void addValue(ResourceAttribute attribute) {
        values.add(attribute);
    }

    public ResourceAttribute addValue(QName qname) {
        return addValue(qname, false);
    }
    public ResourceAttribute addValue(QName qname,boolean required) {
        ResourceAttributeDefinition rad = definition.getAttributeDefinition(qname);
        if (required && rad==null){
            throw new IllegalArgumentException("Attribute definition is missing for "+qname);
        }
        ResourceAttribute ra = new ResourceAttribute(rad);
        values.add(ra);
        return ra;
    }

    public List<ResourceAttribute> getValues() {
        return values;
    }

    /**
     * Return values only for a specific type of attributes.
     * @param attribute
     * @return
     */
    public ResourceAttribute getValue(QName attribute) {
        List<ResourceAttribute> result = getValues(attribute);
        if (result==null){
            throw new IllegalArgumentException("No such vale "+attribute);
        }
        if (result.size() > 1) {
            throw new IllegalArgumentException("More then one attribute is exists. Use getValues! " + attribute);
        }
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    /**
     * Return values only for a specific type of attributes.
     * @param attribute
     * @return
     */
    public List<ResourceAttribute> getValues(QName attribute) {
        List<ResourceAttribute> result = new ArrayList<ResourceAttribute>();
        for (ResourceAttribute value : values) {
            if (value.getDefinition().getQName().equals(attribute)) {
                result.add(value);
            }

        }
        return result;
    }

    public void removeValue(QName attribute) {
        for (Iterator<ResourceAttribute> it = values.iterator(); it.hasNext();) {
            ResourceAttribute resourceAttribute = it.next();
            if (resourceAttribute.getDefinition().getQName().equals(attribute)){
                it.remove();
            }
        }
    }

    public ResourceAttribute getIdentifier() {
        // This is quite wrong, it assumes that there is only one identifier
        // which is not the case. We can have multiple identifiers (composite
        // identifier). This should be fixed later (OPENIDM-359)
        ResourceAttributeDefinition identifierDefinition = getDefinition().getPrimaryIdentifier();
        return getValue(identifierDefinition.getQName());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+getDefinition()+","+getIdentifier();
    }
}

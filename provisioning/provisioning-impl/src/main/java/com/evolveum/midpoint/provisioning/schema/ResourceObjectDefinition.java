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

package com.evolveum.midpoint.provisioning.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * There is confusion about the targetNamespace but this is where we have
 * all other schemas. Don't use the name of the resource because it
 * can be renamed and we have problem. Use the UUID like this
 *
 * http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2
 *
 * @author Vilo Repan
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ResourceObjectDefinition {

    private final QName qname;

    private String name;

    private final String nativeObjectClass;

    /**
     * Available attribute values indexed by QName.
     */
    private Map<QName, ResourceAttributeDefinition> attributes = new HashMap<QName, ResourceAttributeDefinition>();

    private List<ResourceObjectDefinition> superclasses;

    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#container
     */
    private final boolean container;

    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#operation
     */
    private List<Operation> operations = null;

    private ResourceSchema parentSchema;

    public ResourceObjectDefinition(QName qname) {
        this(qname, qname.getLocalPart(), false);
    }

    public ResourceObjectDefinition(QName qname, String nativeObjectClass) {
        this(qname, nativeObjectClass, false);
    }

    public ResourceObjectDefinition(QName qname, boolean container) {
        this(qname, qname.getLocalPart(), container);
    }

    public ResourceObjectDefinition(QName qname, String nativeObjectClass, boolean container) {
        if (qname == null) {
            throw new IllegalArgumentException("Qname can't be null.");
        }
        if (nativeObjectClass == null) {
            throw new IllegalArgumentException("Native object class can't be null.");
        }
        this.qname = qname;
        this.nativeObjectClass = nativeObjectClass;
        this.container = container;
    }

    /**
     * Gets the native name of the represented object class.
     *
     * If the name is not set by the constructor {@link ResourceObjectDefinition(QName, String, boolean)}
     * then it's always the getLocalPart of the Qname
     * 
     * @return
     */
    public String getNativeObjectClass() {
        return nativeObjectClass;
    }

    /**
     * This has yet to be figured out. Not used yet.
     * We have a flat objectclass structure for now.
     * @return
     */
    public List getSuperclasses() {
        if (superclasses == null) {
            superclasses = new ArrayList<ResourceObjectDefinition>();
        }

        return superclasses;
    }

    public Collection<ResourceAttributeDefinition> getAttributesCopy() {
        return new ArrayList(attributes.values());
    }

    protected Collection<ResourceAttributeDefinition> getAttributes() {
        return attributes.values();
    }

    /**
     * Get attribute definition by QName;
     * @param qname
     */
    public  ResourceAttributeDefinition getAttributeDefinition(QName qname) {
        return attributes.get(qname);
    }

    /**
     * XML QName of the object class. Primary, globally-unique identifier.
     * 
     * @return
     */
    public QName getQName() {
        return qname;
    }

    /**
     * Human-readable name or l10n catalog key
     * 
     * @return
     */
    public String getName() {
        if (name == null) {
            return qname.getLocalPart();
        }

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isContainer() {
        return container;
    }

    public List<Operation> getOperations() {
        if (operations == null) {
            operations = new ArrayList<Operation>();
        }
        return operations;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(qname);

        return builder.toString();
    }

    public void addAttribute(ResourceAttributeDefinition raf) {
        attributes.put(raf.getQName(), raf);
        raf.setParentDefinition(this);
    }
    // Povisioning method, these method are used mostly by
    // the midPoint-Provisioning

    public Collection<ResourceAttributeDefinition> getRequiredAttributes() {
        Collection<ResourceAttributeDefinition> result = new ArrayList<ResourceAttributeDefinition>();
        for (ResourceAttributeDefinition a : getAttributes()) {
            if (a.isRequired()) {
                result.add(a);
            }
        }
        return Collections.unmodifiableCollection(result);
    }

    public Collection<ResourceAttributeDefinition> getRequiredAttributsForCreate() {
        Collection<ResourceAttributeDefinition> result = new ArrayList<ResourceAttributeDefinition>();
        Collection<ResourceAttributeDefinition> identifiers = new ArrayList<ResourceAttributeDefinition>();
        boolean hasSecondaryIdentifier = false;
        for (ResourceAttributeDefinition a : getAttributes()) {
            if (a.isRequired()) {
                result.add(a);
//  Identifiers are not strictly required for create. They may be generated by
//  the resource.
//            } else if (!hasSecondaryIdentifier && a.isIdentifier()) {
//                identifiers.add(a);
//            } else if (a.isSecondaryIdentifier()) {
//                if (!hasSecondaryIdentifier) {
//                    identifiers.clear();
//                    hasSecondaryIdentifier = true;
//                }
//                identifiers.add(a);
            }
        }
        result.addAll(identifiers);
        return Collections.unmodifiableCollection(result);
    }

    public ResourceAttributeDefinition getPrimaryIdentifier() {
        ResourceAttributeDefinition id = null;
        for (ResourceAttributeDefinition a : getAttributes()) {
            if (a.isIdentifier()) {
                return a;
            }
        }
        return id;
    }

    public ResourceAttributeDefinition getSecondaryIdentifier() {
        ResourceAttributeDefinition id = null;
        for (ResourceAttributeDefinition a : getAttributes()) {
            if (a.isSecondaryIdentifier()) {
                return a;
            } else if (a.isIdentifier()) {
                id = a;
            }
        }
        return id;
    }

    public ResourceSchema getParentSchema() {
        return parentSchema;
    }

    public void setParentSchema(ResourceSchema parentSchema) {
        this.parentSchema = parentSchema;
    }

    
}

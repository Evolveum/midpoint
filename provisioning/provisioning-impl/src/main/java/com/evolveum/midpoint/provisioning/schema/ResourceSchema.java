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

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.provisioning.conversion.Converter;
import com.evolveum.midpoint.provisioning.conversion.ConverterFactory;
import com.evolveum.midpoint.provisioning.conversion.DefaultConverterFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * There is confusion about the targetNamespace but this is where we have
 * all other schemas. Don't use the name of the resource because it
 * can be renamed and we have problem. Use the UUID like this
 *
 * http://midpoint.evolveum.com/xml/ns/public/resource/instance(s?)/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2
 * 
 * @author Vilo Repan
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ResourceSchema {

    private List<ResourceObjectDefinition> objectclasses;

    private String resourceNamespace;

    private Set<String> importList;

    private DefaultConverterFactory converterFactory = new DefaultConverterFactory();

    public ResourceSchema(String resourceNamespace) {
        this.resourceNamespace = resourceNamespace;
    }


    public Iterator<ResourceObjectDefinition> getObjectClassesIterator() {
        return getObjectClasses().iterator();
    }

    public List<ResourceObjectDefinition> getObjectClassesCopy() {
        return new ArrayList(getObjectClasses());
    }
    /**
     * Returns list of objectclasses in the schema.
     *
     * Don't add additional element to the returned list. Instead  use addObjectClass.
     * 
     * @return
     */
    protected List<ResourceObjectDefinition> getObjectClasses() {
        if (objectclasses == null) {
            objectclasses = new ArrayList<ResourceObjectDefinition>();
        }

        return objectclasses;
    }

    public void addObjectClass(ResourceObjectDefinition def){
        getObjectClasses().add(def);
        def.setParentSchema(this);
    }

    /**
     * Returns list which contains namespaces which will be
     * used as imports when this object will be generated to XSD
     * 
     * @return
     */
    public Set<String> getImportList() {
        if (importList == null) {
            importList = new HashSet<String>();
        }

        return importList;
    }

    public String getResourceNamespace() {
        return resourceNamespace;
    }

    public ResourceObjectDefinition getObjectDefinition(QName objectType) {
        for (ResourceObjectDefinition def : objectclasses) {
            if (def.getQName().equals(objectType)) {
                return def;
            }
        }
        // This should not throw an exception!
        // First of all it is supposed to be getter. Getters should not throw exceptions
        // Also the better way would be to return null and let the calling code
        // provide fallback or throw more meaningfull error
        throw new MidPointException("Unsupported ObjectType in resource schema: " + objectType);
    }

    public void addConverter(Converter conveter){
        converterFactory.addConverter(conveter);
    }

    public ConverterFactory getConverterFactory() {
        return converterFactory;
    }

    

}

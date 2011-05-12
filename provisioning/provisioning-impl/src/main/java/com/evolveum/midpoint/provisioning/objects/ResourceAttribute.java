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

import com.evolveum.midpoint.provisioning.conversion.DefaultConverterFactory;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Node;

/**
 * This is same like a {@link  ResourceShadowObject} but it's already parsed and the
 * provisioner use this class to generate {@link ResourceShadowObject}
 *
 * It's not finished yet but it holds all data need to generate the XML.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ResourceAttribute {

    public static final String code_id = "$Id$";

    /**
     * Name of the {@link ResourceAttribute}.
     */
    private final ResourceAttributeDefinition definition;

    /**
     * Values of the {@link ResourceAttribute}.
     */
    private final List<Node> values;

    public ResourceAttribute(ResourceAttributeDefinition definition) {
        this.definition = definition;
        this.values = new ArrayList<Node>();
    }

    public ResourceAttribute(ResourceAttributeDefinition definition, List<Node> value) {
        this.definition = definition;
        this.values = value;
    }

    public ResourceAttributeDefinition getDefinition() {
        return definition;
    }

    /**
     * Add Java object value, converted to XML representation.
     * @param o
     * @todo use converters here.
     */
    public void addJavaValue(Object o) {
        addValue(definition.getConverterFactory().getConverter(definition.getType()).convertToXML(definition.getQName(), o));

    }

    public <T> T getSingleJavaValue(Class<T> clazz) {
        return (T) getSingleJavaValue();
    }

    public Object getSingleJavaValue() {
        return definition.getConverterFactory().getConverter(definition.getType()).convertToJava(values.get(0));
    }

    public  Collection  getJavaValues() {
        List l = new ArrayList();
        for (Node value : values){
            l.add(definition.getConverterFactory().getConverter(definition.getType()).convertToJava(value));
        }
        return l;
    }

    public void addValue(Node e) {
        values.add(e);
    }

    public List<Node> getValues() {
        return values;
    }

    /**
     * Return object value as a Java object.
     */
    public List<Object> getObjectValue() {
        return new ArrayList();
    }

    @Override
    public String toString() {
        // TODO add attirbute type and/or name
        return this.getClass().getSimpleName()+"("+getDefinition().getQName().getLocalPart()+","+getJavaValues().toString()+")";
    }
}

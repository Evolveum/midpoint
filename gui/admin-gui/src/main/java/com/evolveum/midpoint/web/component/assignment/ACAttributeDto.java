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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAttributeDefinitionType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;

/**
 * @author lazyman
 */
public class ACAttributeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_VALUE = "value";

    private PrismPropertyDefinition definition;
    private ResourceAttributeDefinitionType construction;

    public ACAttributeDto(PrismPropertyDefinition definition, ResourceAttributeDefinitionType construction) {
        Validate.notNull(definition, "Prism property definition must not be null.");
        Validate.notNull(construction, "Value construction must not be null.");

        this.definition = definition;
        this.construction = construction;
    }

    public PrismPropertyDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        String name = definition.getDisplayName();
        return StringUtils.isNotEmpty(name) ? name : definition.getName().getLocalPart();
    }

    public String getValue() {
//        JAXBElement element = construction.getValueConstructor();
//        if (element == null) {
//            return null;
//        }
//        Object value = element.getValue();
//        return value != null ? value.toString() : null;
    	// TODO
    	return null;
    }

    public void setValue(String value) {
    	// TODO
//        if (value == null) {
//            construction.setValueConstructor(null);
//        } else {
//            construction.setValueConstructor(new ObjectFactory().createValue(value));
//        }
//
//        construction.getSequence().getValueConstructor().clear();
    }
}

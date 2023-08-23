/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Map;

public class CorrelationVerificationToken extends AbstractAuthenticationToken {

    private String correlatorName;
    private Map<ItemPath, String> attributes;

    public CorrelationVerificationToken(Map<ItemPath, String> attributes, String correlatorName) {
        super(null);
        this.attributes = attributes;
        this.correlatorName = correlatorName;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Map<ItemPath, String> getPrincipal() {
        return null;
    }


    @Override
    public Map<ItemPath, String> getDetails() {
        return attributes;
    }

    public String getCorrelatorName() {
        return correlatorName;
    }

    public FocusType getPreFocus(Class<? extends FocusType> focusType) {
        return getPreFocus(focusType, attributes);
    }

    public FocusType getPreFocus(Class<? extends FocusType> focusType, Map<ItemPath, String> attributes) {
        PrismObject<? extends FocusType> newObject = null;
        try {
            newObject = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusType).instantiate();
        } catch (SchemaException e) {
            System.out.println("Error while creating new object");
            return new UserType();
        }
        for (Map.Entry<ItemPath, String> entry : attributes.entrySet()) {
            try {
                PrismProperty item = newObject.findOrCreateProperty(entry.getKey());
                item.addRealValue(convertValueIfNecessary((PrismPropertyDefinition) item.getDefinition(), entry.getValue()));
            } catch (SchemaException e) {
                System.out.println("Error while adding attributes to object");
                //throw new RuntimeException(e);
                //TODO logging?
            }
        }

        return newObject.asObjectable();
    }

    private Object convertValueIfNecessary(PrismPropertyDefinition def, String value) {
        if (QNameUtil.match(DOMUtil.XSD_DATETIME, def.getTypeName())) {
            return XmlTypeConverter.createXMLGregorianCalendar(value);
        }
        if (QNameUtil.match(PolyStringType.COMPLEX_TYPE, def.getTypeName())) {
            return new PolyString(value);
        }
        return value;
    }
}

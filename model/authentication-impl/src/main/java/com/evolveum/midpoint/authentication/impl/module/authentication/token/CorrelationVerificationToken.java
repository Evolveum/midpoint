/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication.token;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Map;

public class CorrelationVerificationToken extends AbstractAuthenticationToken {

    private String correlatorName;
    private Map<ItemPath, String> attributes;

    public CorrelationVerificationToken(Map<ItemPath, String> attributes) {
        super(null);
        this.attributes = attributes;
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
        return "blabla";//correlatorName;
    }

    public FocusType getPreFocus(Class<? extends FocusType> focusType) {
        PrismObject<? extends FocusType> newObject = null;
        try {
            newObject = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusType).instantiate();
        } catch (SchemaException e) {
            System.out.println("Error while creating new object");
            return new UserType();
        }
        try {
            for (Map.Entry<ItemPath, String> entry : attributes.entrySet()) {
                    PrismProperty<String> item = newObject.findOrCreateProperty(entry.getKey());
//                    PrismPropertyValue<?> pValue = PrismContext.get().itemFactory().createPropertyValue(entry.getValue());
                    item.addRealValue(entry.getValue());
            }

        } catch (SchemaException e) {
            System.out.println("Error while adding attributes to object");
            //throw new RuntimeException(e);
            //TODO logging?
        }
        return newObject.asObjectable();
    }
}

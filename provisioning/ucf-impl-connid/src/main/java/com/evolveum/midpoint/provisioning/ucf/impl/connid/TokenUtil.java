/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.identityconnectors.framework.common.objects.SyncToken;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Utility methods to work with the token extension item and SyncToken objects.
 */
public class TokenUtil {

    @NotNull
    static <T> PrismProperty<T> createTokenPropertyFromRealValue(T realValue, PrismContext prismContext) {
        QName type = XsdTypeMapper.toXsdType(realValue.getClass());

        MutablePrismPropertyDefinition<T> propDef = prismContext.definitionFactory().createPropertyDefinition(SchemaConstants.SYNC_TOKEN, type);
        propDef.setDynamic(true);
        propDef.setMaxOccurs(1);
        propDef.setIndexed(false); // redundant, as dynamic extension items are not indexed by default
        PrismProperty<T> property = propDef.instantiate();
        property.addRealValue(realValue);
        return property;
    }

    static SyncToken getSyncToken(PrismProperty tokenProperty) throws SchemaException {
        if (tokenProperty == null || tokenProperty.getValues().isEmpty()) {
            return null;
        } else if (tokenProperty.getValues().size() > 1) {
            throw new SchemaException("Unexpected number of attributes in SyncToken. SyncToken is single-value attribute and can contain only one value.");
        } else {
            Object tokenValue = tokenProperty.getAnyRealValue();
            if (tokenValue != null) {
                return new SyncToken(tokenValue);
            } else {
                return null;
            }
        }
    }

    @Contract("!null, _ -> !null; null, _ -> null")
    static <T> PrismProperty<T> createTokenProperty(SyncToken syncToken, PrismContext prismContext) {
        if (syncToken != null) {
            //noinspection unchecked
            T realValue = (T) syncToken.getValue();
            return createTokenPropertyFromRealValue(realValue, prismContext);
        } else {
            return null;
        }
    }
}

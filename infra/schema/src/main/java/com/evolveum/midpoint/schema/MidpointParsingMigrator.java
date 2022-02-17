/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.marshaller.ParsingMigrator;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationPartType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleLocalizableMessageType;

public class MidpointParsingMigrator implements ParsingMigrator {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T tryParsingPrimitiveAsBean(PrimitiveXNode<T> primitive, Class<T> beanClass, ParsingContext pc) {
        if (LocalizableMessageType.class.equals(beanClass)) {
            return (T) new SingleLocalizableMessageType().fallbackMessage(primitive.getStringValue());
        } else if (InformationType.class.equals(beanClass)) {
            // This is to allow specifying plain text where InformationType is expected. It is not very clean; and
            // quite experimental for now.
            return (T) stringToInformationType(primitive.getStringValue());
        } else {
            return null;
        }
    }

    public static InformationType stringToInformationType(String s) {
        InformationType info = new InformationType();
        InformationPartType part = new InformationPartType();
        part.setLocalizableText(LocalizationUtil.createForFallbackMessage(s));
        info.getPart().add(part);
        return info;
    }
}

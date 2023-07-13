/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;

import javax.xml.namespace.QName;

//TODO very simplified, what about same local part and different namespace?
public class ResourceObjectClassChoiceRenderer implements IChoiceRenderer<QName> {
    @Override
    public Object getDisplayValue(QName resourceObjectTypeDefinitionType) {
        if (resourceObjectTypeDefinitionType == null) {
            return null;
        }
        return resourceObjectTypeDefinitionType.getLocalPart();
    }

}

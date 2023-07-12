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
import org.apache.wicket.model.IModel;

import java.util.List;

public class ResourceObjectTypeChoiceRenderer implements IChoiceRenderer<ResourceObjectTypeDefinitionType> {
    @Override
    public Object getDisplayValue(ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        if (resourceObjectTypeDefinitionType == null) {
            return null;
        }
        String displayName = resourceObjectTypeDefinitionType.getDisplayName();
        if (StringUtils.isBlank(displayName)) {
            displayName = resourceObjectTypeDefinitionType.getKind().value();
            if (resourceObjectTypeDefinitionType.getIntent() != null) {
                displayName += " (" + resourceObjectTypeDefinitionType.getIntent() + ")";
            }
        }
        return displayName;
    }

}

/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import java.util.List;

public class ResourceObjectTypeChoiceRenderer implements IChoiceRenderer<ResourceObjectTypeDefinition> {
    @Override
    public Object getDisplayValue(ResourceObjectTypeDefinition resourceObjectTypeDefinition) {
        if (resourceObjectTypeDefinition == null) {
            return null;
        }
        String displayName = resourceObjectTypeDefinition.getDefinitionBean().getDisplayName();
        if (StringUtils.isBlank(displayName)) {
            displayName = resourceObjectTypeDefinition.getKind().value();
            if (resourceObjectTypeDefinition.getIntent() != null) {
                displayName += " (" + resourceObjectTypeDefinition.getIntent() + ")";
            }
        }
        return displayName;
    }

}

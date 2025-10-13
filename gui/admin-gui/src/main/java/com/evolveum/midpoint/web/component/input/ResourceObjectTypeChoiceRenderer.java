/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.List;

public class ResourceObjectTypeChoiceRenderer implements IChoiceRenderer<ResourceObjectTypeIdentification> {

    private final ResourceDetailsModel objectDetailsModels;

    public ResourceObjectTypeChoiceRenderer(ResourceDetailsModel objectDetailsModels) {
        this.objectDetailsModels = objectDetailsModels;
    }

    @Override
    public Object getDisplayValue(ResourceObjectTypeIdentification resourceObjectTypeIdentification) {
        if (resourceObjectTypeIdentification == null) {
            return null;
        }

        ResourceObjectTypeDefinition resourceObjectTypeDefinition = objectDetailsModels.getObjectTypeDefinition(
                resourceObjectTypeIdentification.getKind(),
                resourceObjectTypeIdentification.getIntent());


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

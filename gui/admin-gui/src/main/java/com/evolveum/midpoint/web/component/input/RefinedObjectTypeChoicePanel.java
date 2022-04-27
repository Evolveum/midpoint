/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 */
public class RefinedObjectTypeChoicePanel extends DropDownChoicePanel<ResourceObjectTypeDefinition> {

    public RefinedObjectTypeChoicePanel(String id, IModel<ResourceObjectTypeDefinition> model, IModel<PrismObject<ResourceType>> resourceModel) {
        super(id, model, createChoiceModel(resourceModel), createRenderer(), false);
    }

    private static IModel<? extends List<? extends ResourceObjectTypeDefinition>> createChoiceModel(final IModel<PrismObject<ResourceType>> resourceModel) {
        return new IModel<List<? extends ResourceObjectTypeDefinition>>() {
            @Override
            public List<? extends ResourceObjectTypeDefinition> getObject() {
                ResourceSchema refinedSchema;
                try {
                    refinedSchema = ResourceSchemaFactory.getCompleteSchema(resourceModel.getObject());
                } catch (SchemaException | ConfigurationException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
                return new ArrayList<>(
                        refinedSchema.getObjectTypeDefinitions());
            }

            @Override
            public void detach() {
            }

            @Override
            public void setObject(List<? extends ResourceObjectTypeDefinition> object) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static IChoiceRenderer<ResourceObjectTypeDefinition> createRenderer() {
        return new IChoiceRenderer<ResourceObjectTypeDefinition>() {

            @Override
            public Object getDisplayValue(ResourceObjectTypeDefinition object) {
                if (object.getDisplayName() != null) {
                    return object.getDisplayName();
                }
                return object.getHumanReadableName();
            }

            @Override
            public String getIdValue(ResourceObjectTypeDefinition object, int index) {
                return Integer.toString(index);
            }

            @Override
            public ResourceObjectTypeDefinition getObject(String id, IModel<? extends List<? extends ResourceObjectTypeDefinition>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }
        };
    }

}

/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 */
public class RefinedObjectTypeChoicePanel extends DropDownChoicePanel<RefinedObjectClassDefinition> {

    public RefinedObjectTypeChoicePanel(String id, IModel<RefinedObjectClassDefinition> model, IModel<PrismObject<ResourceType>> resourceModel) {
        super(id, model, createChoiceModel(resourceModel), createRenderer(), false);
    }

    private static IModel<? extends List<? extends RefinedObjectClassDefinition>> createChoiceModel(final IModel<PrismObject<ResourceType>> resourceModel) {
        return new IModel<List<? extends RefinedObjectClassDefinition>>() {
            @Override
            public List<? extends RefinedObjectClassDefinition> getObject() {
                RefinedResourceSchema refinedSchema;
                try {
                    refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceModel.getObject());
                } catch (SchemaException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
                List<? extends RefinedObjectClassDefinition> refinedDefinitions = refinedSchema.getRefinedDefinitions();
                List<? extends RefinedObjectClassDefinition> defs = new ArrayList<>();
                for (RefinedObjectClassDefinition rdef : refinedDefinitions) {
                    if (rdef.getKind() != null) {
                        ((List) defs).add(rdef);
                    }
                }
                return defs;
            }

            @Override
            public void detach() {
            }

            @Override
            public void setObject(List<? extends RefinedObjectClassDefinition> object) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static IChoiceRenderer<RefinedObjectClassDefinition> createRenderer() {
        return new IChoiceRenderer<RefinedObjectClassDefinition>() {

            @Override
            public Object getDisplayValue(RefinedObjectClassDefinition object) {
                if (object.getDisplayName() != null) {
                    return object.getDisplayName();
                }
                return object.getHumanReadableName();
            }

            @Override
            public String getIdValue(RefinedObjectClassDefinition object, int index) {
                return Integer.toString(index);
            }

            @Override
            public RefinedObjectClassDefinition getObject(String id, IModel<? extends List<? extends RefinedObjectClassDefinition>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }
        };
    }

}

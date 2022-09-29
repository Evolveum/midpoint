/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.SourceOrTargetOfMappingPanelFactory;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SourceMappingProvider extends ChoiceProvider<VariableBindingDefinitionType> {

    private static final long serialVersionUID = 1L;

    private final IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> rowModel;

    public <IW extends ItemWrapper> SourceMappingProvider(IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> rowModel) {
        this.rowModel = rowModel;
    }

    @Override
    public String getDisplayValue(VariableBindingDefinitionType value) {
        return getIdValue(value);
    }

    @Override
    public String getIdValue(VariableBindingDefinitionType value) {
        return GuiDisplayNameUtil.getDisplayName(value);
    }

    @Override
    public void query(String text, int page, Response<VariableBindingDefinitionType> response) {

        List<String> choices = collectAvailableDefinitions(text);

        response.addAll(toChoices(choices));
    }

    @Override
    public Collection<VariableBindingDefinitionType> toChoices(Collection<String> values) {
        return values.stream()
                .map(value -> new VariableBindingDefinitionType()
                        .path(PrismContext.get().itemPathParser().asItemPathType(
                                "$" + ExpressionConstants.VAR_FOCUS + "/" + value
                        )))
                .collect(Collectors.toList());
    }

    public List<String> collectAvailableDefinitions(String input, ResourceObjectTypeDefinitionType resourceObjectType) {
        ComplexTypeDefinition resourceDef =
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(ResourceType.COMPLEX_TYPE);

        PrismPropertyDefinition<QName> typeDef = resourceDef.findPropertyDefinition(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_FOCUS,
                ResourceObjectFocusSpecificationType.F_TYPE));

        QName type = typeDef.defaultValue();

        if (resourceObjectType != null
                && resourceObjectType.getFocus() != null
                && resourceObjectType.getFocus().getType() != null) {
            type = resourceObjectType.getFocus().getType();
        }

        if (type == null) {
            type = UserType.COMPLEX_TYPE;
        }

        PrismObjectDefinition<Objectable> focusDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(type);

        List<String> toSelect = new ArrayList<>();
        if (StringUtils.isNotBlank(input) && input.lastIndexOf("/") == (input.length() - 1)) {
            input = input.substring(0, input.length() - 1);
        }
        if (StringUtils.isNotBlank(input) && input.contains("/")) {
            int lastIndexOfSeparator = input.lastIndexOf("/");
            String superPath = input.substring(0, lastIndexOfSeparator);
            String suffix = input.substring(lastIndexOfSeparator + 1);
            ItemDefinition<?> superDef = focusDef.findItemDefinition(ItemPath.create(superPath.split("/")));
            if (superDef != null && superDef instanceof PrismContainerDefinition) {
                collectItems(((PrismContainerDefinition) superDef).getDefinitions(), suffix, toSelect, true);
                return toSelect.stream().map(subPath -> superPath + "/" + subPath).collect(Collectors.toList());
            }
        } else {
            collectItems(focusDef.getDefinitions(), input, toSelect, true);
        }
        return toSelect;
    }

    public List<String> collectAvailableDefinitions(String input) {
        PrismPropertyWrapper<VariableBindingDefinitionType> wrapper = rowModel.getObject();

        ResourceObjectTypeDefinitionType resourceObjectType = getResourceObjectType(wrapper);

        return collectAvailableDefinitions(input, resourceObjectType);
    }

    private static void collectItems(
            Collection<? extends ItemDefinition> definitions,
            String input,
            List<String> toSelect,
            boolean showContainers) {
        if (definitions == null) {
            return;
        }

        for (ItemDefinition<?> def : definitions) {
            if (StringUtils.isNotBlank(input) && !def.getItemName().getLocalPart().startsWith(input)) {
                continue;
            }

            if (def instanceof PrismContainerDefinition) {
                if (!showContainers) {
                    toSelect.add(def.getItemName().getLocalPart());
                    continue;
                }
                List<String> subChoices = new ArrayList<>();
                collectItems(((PrismContainerDefinition) def).getDefinitions(), "", subChoices, false);
                subChoices.forEach(value -> toSelect.add(def.getItemName().getLocalPart() + "/" + value));
                continue;
            }

            toSelect.add(def.getItemName().getLocalPart());
        }
    }

    private ResourceObjectTypeDefinitionType getResourceObjectType(PrismPropertyWrapper<VariableBindingDefinitionType> propertyWrapper) {

        if (propertyWrapper != null
                && propertyWrapper.getParent() != null
                && propertyWrapper.getParent().getParent() != null
                && propertyWrapper.getParent().getParent().getParent() != null
                && propertyWrapper.getParent().getParent().getParent().getParent() != null) {
            PrismContainerValueWrapper containerValue = propertyWrapper.getParent().getParent().getParent().getParent().getParent();

            if (containerValue != null && containerValue.getRealValue() instanceof ResourceObjectTypeDefinitionType) {
                return (ResourceObjectTypeDefinitionType) containerValue.getRealValue();
            }
        }
        return null;
    }
}

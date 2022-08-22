/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteItemDefinitionPanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.AutoCompleteDisplayableValueConverter;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SourceOrTargetOfMappingPanelFactory extends VariableBindingDefinitionTypePanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return super.match(wrapper)
                && (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_INBOUND,
                InboundMappingType.F_TARGET))
                || wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_INBOUND,
                MappingType.F_SOURCE)));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {

        ResourceObjectTypeDefinitionType resourceObjectType = getResourceObjectType(panelCtx.unwrapWrapperModel());
        if (resourceObjectType.getFocus() != null) {
            PrismContainerDefinition resourceDef = resourceObjectType.asPrismContainerValue().getDefinition();
            PrismPropertyDefinition<QName> typeDef = resourceDef.findPropertyDefinition(ItemPath.create(
                    ResourceObjectTypeDefinitionType.F_FOCUS,
                    ResourceObjectFocusSpecificationType.F_TYPE));
            QName type = typeDef.defaultValue();

            if (resourceObjectType.getFocus().getType() != null) {
                type = resourceObjectType.getFocus().getType();
            }

            if (type == null) {
                type = UserType.COMPLEX_TYPE;
            }

            PrismObjectDefinition<Objectable> focusDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(type);

            IModel<String> valueModel = new IModel<>() {
                @Override
                public String getObject() {
                    VariableBindingDefinitionType value = panelCtx.getRealValueModel().getObject();
                    if (value == null || value.getPath() == null) {
                        return null;
                    }
                    ItemPathType path = value.getPath();

                    return path.getItemPath().stripVariableSegment().toString();
                }

                @Override
                public void setObject(String object) {
                    if (StringUtils.isBlank(object)) {
                        panelCtx.getRealValueModel().setObject(null);
                    }
                    VariableBindingDefinitionType def = new VariableBindingDefinitionType()
                            .path(PrismContext.get().itemPathParser().asItemPathType(
                                    "$" + ExpressionConstants.VAR_FOCUS + "/" + object
                            ));
                    panelCtx.getRealValueModel().setObject(def);
                }
            };

            AutoCompleteTextPanel<String> panel = new AutoCompleteTextPanel<>(
                    panelCtx.getComponentId(), valueModel, String.class, true) {
                @Override
                public Iterator<String> getIterator(String input) {
                    return collectAvailableDefinitions(input, focusDef).iterator();
                }
            };
            panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            return panel;
        }

        return super.getPanel(panelCtx);
    }

    private List<String> collectAvailableDefinitions(String input, PrismObjectDefinition focusDef) {
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

    private void collectItems(
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

        if (propertyWrapper.getParent() != null
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

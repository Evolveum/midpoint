/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.itempath;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author katka
 */
@Component
public class CorrelatorItemRefPanelFactory extends ItemPathPanelFactory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return ItemPathType.COMPLEX_TYPE.equals(wrapper.getTypeName())
                && wrapper.getParentContainerValue(CorrelationDefinitionType.class) != null
                && (wrapper.getParentContainerValue(ResourceObjectTypeDefinitionType.class) != null
                || wrapper.getParentContainerValue(ShadowAssociationDefinitionType.class) != null);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        PrismPropertyWrapper<ItemPathType> item = panelCtx.unwrapWrapperModel();
        List<ResourceAttributeDefinitionType> attributeDefinitions = getAttributeDefinition(item);

        if (attributeDefinitions.isEmpty()) {
            return super.getPanel(panelCtx);
        }

        List<ItemPathType> itemPaths = getTargetsOfInboundMappings(attributeDefinitions);
        if (itemPaths == null || itemPaths.isEmpty()) {
            IModel<String> valueModel = new IModel<>() {
                @Override
                public String getObject() {
                    return GuiDisplayNameUtil.getDisplayName(panelCtx.getRealValueModel().getObject());
                }

                @Override
                public void setObject(String object) {
                    panelCtx.getRealValueModel().setObject(PrismContext.get().itemPathParser().asItemPathType(object));
                }
            };

            AutoCompleteTextPanel<String> panel = new AutoCompleteTextPanel<>(
                    panelCtx.getComponentId(), valueModel, String.class, true) {
                @Override
                public Iterator<String> getIterator(String input) {
                    return iteratorForAvariableDefinitions(input, item);
                }
            };
            panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            return panel;
        }

        DropDownChoicePanel<ItemPathType> typePanel = new DropDownChoicePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                Model.ofList(itemPaths), true);
        typePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        typePanel.setOutputMarkupId(true);
        return typePanel;
    }

    private Iterator<String> iteratorForAvariableDefinitions(String input, PrismPropertyWrapper<ItemPathType> item) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = item.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (objectType != null) {
            FocusDefinitionsMappingProvider provider = new FocusDefinitionsMappingProvider(null);
            return provider.collectAvailableDefinitions(input, objectType.getRealValue()).iterator();
        }

        if (item.getParentContainerValue(ShadowAssociationDefinitionType.class) != null) {
            FocusDefinitionsMappingProvider provider = new FocusDefinitionsMappingProvider(null) {
                @Override
                protected PrismContainerDefinition<? extends Containerable> getFocusTypeDefinition(ResourceObjectTypeDefinitionType resourceObjectType) {
                    return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);
                }
            };
            return provider.collectAvailableDefinitions(input, null).iterator();
        }

        return Collections.emptyIterator();
    }

    private List<ItemPathType> getTargetsOfInboundMappings(List<ResourceAttributeDefinitionType> attributeDefinitions) {
        List<ItemPathType> targets = new ArrayList<>();
        attributeDefinitions.forEach(attributeMapping -> attributeMapping.getInbound()
                        .forEach(inboundMapping -> {
                            if (inboundMapping.getTarget() != null && inboundMapping.getTarget().getPath() != null) {
                                targets.add(new ItemPathType(inboundMapping.getTarget().getPath().getItemPath().stripVariableSegment()));
                            }
                        }));
        return targets;
    }

    private List<ResourceAttributeDefinitionType> getAttributeDefinition(PrismPropertyWrapper<ItemPathType> item) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = item.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (objectType != null) {
            return objectType.getRealValue().getAttribute();
        }

        PrismContainerValueWrapper<ShadowAssociationDefinitionType> association = item.getParentContainerValue(ShadowAssociationDefinitionType.class);
        if (association != null) {
            List<ResourceAttributeDefinitionType> attributes = new ArrayList<>();
            attributes.addAll(association.getRealValue().getAttribute());
            attributes.addAll(association.getRealValue().getObjectRef());
            return attributes;
        }

        return Collections.emptyList();
    }
}

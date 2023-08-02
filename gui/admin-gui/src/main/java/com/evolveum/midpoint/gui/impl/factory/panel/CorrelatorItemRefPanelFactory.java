/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.SourceMappingProvider;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.Serializable;
import java.util.ArrayList;
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
                && (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_CORRELATION,
                CorrelationDefinitionType.F_CORRELATORS,
                CompositeCorrelatorType.F_ITEMS,
                ItemsSubCorrelatorType.F_ITEM,
                CorrelationItemType.F_REF
        )));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        PrismPropertyWrapper<ItemPathType> item = panelCtx.unwrapWrapperModel();
        ResourceObjectTypeDefinitionType objectType = getObjectType(item);

        if (objectType == null) {
            return super.getPanel(panelCtx);
        }

        List<ItemPathType> itemPaths = getTargetsOfInboundMappings(objectType);
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
                    SourceMappingProvider provider = new SourceMappingProvider(null);
                    return provider.collectAvailableDefinitions(input, objectType).iterator();
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

    private List<ItemPathType> getTargetsOfInboundMappings(ResourceObjectTypeDefinitionType objectType) {
        List<ItemPathType> targets = new ArrayList<>();
        objectType.getAttribute()
                .forEach(attributeMapping -> attributeMapping.getInbound()
                        .forEach( inboundMapping -> {
                            if (inboundMapping.getTarget() != null && inboundMapping.getTarget().getPath() != null) {
                                targets.add(new ItemPathType(inboundMapping.getTarget().getPath().getItemPath().stripVariableSegment()));
                            }
                        }));
        return targets;
    }

    private ResourceObjectTypeDefinitionType getObjectType(PrismPropertyWrapper<ItemPathType> item) {
        if (item.getParent() != null
                && item.getParent().getParent() != null
                && item.getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent().getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent() != null) {
            Object objectType =
                    item.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getRealValue();
            if (objectType instanceof ResourceObjectTypeDefinitionType) {
                return (ResourceObjectTypeDefinitionType) objectType;
            }
        }
        return null;
    }
}

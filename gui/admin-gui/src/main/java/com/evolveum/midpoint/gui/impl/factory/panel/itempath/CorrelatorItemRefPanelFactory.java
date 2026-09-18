/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.itempath;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.web.component.input.PopoverActionChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationMappingCreationUtil.createMapping;

/**
 * @author katka
 */
@Component
public class CorrelatorItemRefPanelFactory extends ItemPathPanelFactory implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

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

        List<ItemPathType> itemPaths = getTargetsOfInboundMappings(item);

        PopoverActionChoicePanel<ItemPathType> typePanel =
                new PopoverActionChoicePanel<>(
                        panelCtx.getComponentId(),
                        panelCtx.getRealValueModel(),
                        Model.ofList(itemPaths),
                        true) {

                    @Override
                    protected IModel<String> getChoicesIconCssModel() {
                        return Model.of("fa fa-tag fa-fw text-muted me-1");
                    }

                    @Override
                    protected IModel<String> getActionIconCssModel() {
                        return Model.of("fas fa-plus-circle me-1");
                    }

                    @Override
                    protected void onActionClick(
                            AjaxRequestTarget target,
                            PopoverActionChoicePanel<ItemPathType> component) {

                        PrismContainerValueWrapper<CorrelationItemType> correlationItem =
                                item.getParentContainerValue(CorrelationItemType.class);

                        createMappingPerformed(
                                target,
                                (PageBase) panelCtx.getPageBase(),
                                () -> correlationItem,
                                component);
                    }

                    @Override
                    protected void onChoiceSelected(
                            AjaxRequestTarget target,
                            PopoverActionChoicePanel<ItemPathType> component) {
                        // Nothing else to refresh here.
                    }
                };

        typePanel.setOutputMarkupId(true);

        return typePanel;
    }

    private void createMappingPerformed(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull IModel<PrismContainerValueWrapper<CorrelationItemType>> rowModel,
            @NotNull PopoverActionChoicePanel<ItemPathType> panel) {

        PrismContainerValueWrapper<CorrelationItemType> correlationItem = rowModel.getObject();

        if (correlationItem == null) {
            return;
        }

        PrismContainerValueWrapper<ItemsSubCorrelatorType> correlator = correlationItem
                .getParentContainerValue(ItemsSubCorrelatorType.class);

        if (correlator == null) {
            return;
        }

        createMapping(target, pageBase,
                () -> correlator,
                ajaxTarget -> ajaxTarget.add(panel),
                rowModel);
    }

    private List<ItemPathType> getTargetsOfInboundMappings(PrismPropertyWrapper<ItemPathType> item) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = item
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (objectType != null) {
            List<ResourceAttributeDefinitionType> attributeDefinitions = objectType.getRealValue().getAttribute();
            List<ItemPathType> targets = new ArrayList<>();
            attributeDefinitions.forEach(attributeMapping -> attributeMapping.getInbound()
                    .forEach(inboundMapping -> {
                        if (inboundMapping.getTarget() != null && inboundMapping.getTarget().getPath() != null) {
                            targets.add(new ItemPathType(inboundMapping.getTarget().getPath().getItemPath().stripVariableSegment()));
                        }
                    }));
            return targets;
        }

        PrismContainerValueWrapper<AssociationSynchronizationExpressionEvaluatorType> association = item
                .getParentContainerValue(AssociationSynchronizationExpressionEvaluatorType.class);
        if (association != null) {
            List<AttributeInboundMappingsDefinitionType> attributeDefinitions = new ArrayList<>();
            attributeDefinitions.addAll(association.getRealValue().getAttribute());
            attributeDefinitions.addAll(association.getRealValue().getObjectRef());
            List<ItemPathType> targets = new ArrayList<>();
            attributeDefinitions.forEach(attributeMapping -> attributeMapping.getMapping()
                    .forEach(mapping -> {
                        if (mapping.getTarget() != null && mapping.getTarget().getPath() != null) {
                            targets.add(new ItemPathType(mapping.getTarget().getPath().getItemPath().stripVariableSegment()));
                        }
                    }));
            return targets;
        }

        return Collections.emptyList();
    }
}

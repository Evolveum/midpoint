/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.expression;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

@Component
public class ObjectTypeMappingExpressionPanelFactory extends AbstractGuiComponentFactory<ExpressionType> implements Serializable {

    private static final ItemPath INBOUND_EXPRESSION_PATH = ItemPath.create(
            ResourceType.F_SCHEMA_HANDLING,
            SchemaHandlingType.F_OBJECT_TYPE,
            ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
            ResourceAttributeDefinitionType.F_INBOUND,
            InboundMappingType.F_EXPRESSION);

    private static final ItemPath OUTBOUND_EXPRESSION_PATH = ItemPath.create(
            ResourceType.F_SCHEMA_HANDLING,
            SchemaHandlingType.F_OBJECT_TYPE,
            ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
            ResourceAttributeDefinitionType.F_OUTBOUND,
            OutboundMappingType.F_EXPRESSION);

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ExpressionType> panelCtx) {

        return new ExpressionPanel(panelCtx.getComponentId(),
                panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel()) {

            @Override
            protected IModel<String> getExpressionDrawerTitle(IModel<String> title) {
                IModel<String> expressionDrawerTitle = super.getExpressionDrawerTitle(title);
                return createDrawerTitle(expressionDrawerTitle, panelCtx);
            }

            @Override
            protected boolean isReadOnly() {
                IModel<PrismPropertyWrapper<ExpressionType>> itemWrapperModel = panelCtx.getItemWrapperModel();
                if (itemWrapperModel != null && itemWrapperModel.getObject() != null) {
                    return itemWrapperModel.getObject().isReadOnly();
                }
                return super.isReadOnly();
            }

            @Override
            protected List<ExpressionPanel.RecognizedEvaluator> getChoices() {
                return ObjectTypeMappingExpressionPanelFactory.this.getChoices(super.getChoices());
            }

            @Override
            public List<CollapsedItem<DrawerModel>> getDrawerCollapsedItems() {
                List<CollapsedItem<DrawerModel>> drawerCollapsedItems = super.getDrawerCollapsedItems();

                if (getSelectedEvaluatorType() != RecognizedEvaluator.SCRIPT) {
                    return drawerCollapsedItems;
                }

                PrismContainerValueWrapper<IterationSpecificationType> iterationValueWrapper = findIterationValueWrapper(panelCtx);

                if (iterationValueWrapper == null) {
                    return drawerCollapsedItems;
                }

                CollapsedItem<DrawerModel> collapsedItem = new CollapsedItem<>() {

                    @Override
                    public IModel<String> getIcon() {
                        return Model.of("fa fa-sort-numeric-asc");
                    }

                    @Override

                    public IModel<String> getTitle() {
                        IModel<String> title = createStringResource("IterationSettings.button.iterationSettings");
                        return createDrawerTitle(title, panelCtx);

                    }

                    @Override
                    public org.apache.wicket.Component getPanel(String id, DrawerModel model) {
                        ContainerDrawerPanel<IterationSpecificationType> components = new ContainerDrawerPanel<>(
                                id, () -> iterationValueWrapper, null,
                                createStringResource("IterationSettings.definition.info"));

                        components.info(translate("IterationSettings.description"));

                        return components;
                    }
                };

                drawerCollapsedItems.add(collapsedItem);
                return drawerCollapsedItems;

            }

            @Override
            protected String getAdditionalCssClassForTypeChoice() {
                return getAdditionalLabelClass(panelCtx.unwrapWrapperModel());
            }
        };
    }

    private String getAdditionalLabelClass(PrismPropertyWrapper<ExpressionType> wrapper) {
        return !wrapper.isMetadata() && wrapper.isReadOnly() ? "prism-value-label-readonly" : null;
    }

    private static PrismContainerValueWrapper<IterationSpecificationType> findIterationValueWrapper(
            PrismPropertyPanelContext<ExpressionType> panelCtx) {
        PrismPropertyWrapper<ExpressionType> expressionWrapper = panelCtx.getItemWrapperModel().getObject();
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectTypeValueWrapper = expressionWrapper
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        try {

            PrismContainerWrapper<IterationSpecificationType> iterationContainer = objectTypeValueWrapper
                    .findContainer(ResourceObjectTypeDefinitionType.F_ITERATION);
            return iterationContainer.getValue();
        } catch (SchemaException e) {
            return null;
        }
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(
            IW wrapper, VW valueWrapper) {

        ItemPath targetPath = wrapper.getPath().namedSegmentsOnly();

        return targetPath.equivalent(INBOUND_EXPRESSION_PATH)
                || targetPath.equivalent(OUTBOUND_EXPRESSION_PATH);
    }

    @Override
    public Integer getOrder() {
        return 90;
    }

    protected List<ExpressionPanel.RecognizedEvaluator> getChoices(List<ExpressionPanel.RecognizedEvaluator> parentChoices) {
        parentChoices.removeIf(choice ->
                ExpressionPanel.RecognizedEvaluator.ASSOCIATION_FROM_LINK == choice
                        || ExpressionPanel.RecognizedEvaluator.SHADOW_OWNER_REFERENCE_SEARCH == choice);
        return parentChoices;
    }

    private IModel<String> createDrawerTitle(IModel<String> baseTitle, PrismPropertyPanelContext<ExpressionType> panelCtx) {
        String mappingName = getMappingName(panelCtx);

        if (StringUtils.isEmpty(mappingName)) {
            return baseTitle;
        }

        return createStringResource(
                "ObjectTypeMappingExpressionPanelFactory.for.mapping",
                new Object[] { baseTitle.getObject(), mappingName });    }

    private static @Nullable String getMappingName(PrismPropertyPanelContext<ExpressionType> panelCtx) {
        PrismPropertyWrapper<ExpressionType> expressionWrapper = panelCtx.getItemWrapperModel().getObject();

        PrismContainerValueWrapper<MappingType> mappingValueWrapper =
                expressionWrapper.getParentContainerValue(InboundMappingType.class);

        if (mappingValueWrapper == null) {
            mappingValueWrapper =
                    expressionWrapper.getParentContainerValue(OutboundMappingType.class);
        }

        if (mappingValueWrapper == null) {
            return null;
        }

        MappingType mapping = mappingValueWrapper.getRealValue();
        return mapping != null ? mapping.getName() : null;
    }

}

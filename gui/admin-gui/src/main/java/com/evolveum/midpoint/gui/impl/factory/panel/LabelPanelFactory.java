/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.LookupTableLabelPanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;

@Component
public class LabelPanelFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return (wrapper.isReadOnly() || wrapper.isMetadata()) && wrapper instanceof PrismPropertyWrapper;
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismPropertyPanelContext<T> panelCtx) {
        String lookupTableOid = panelCtx.getPredefinedValuesOid();
        Label labelPanel;
        T object = panelCtx.getRealValueModel().getObject();
        if (lookupTableOid != null) {
            labelPanel = new LookupTableLabelPanel(panelCtx.getComponentId(), panelCtx.getRealValueStringModel(), lookupTableOid);
        } else if (object instanceof Enum<?>) {
            labelPanel = new Label(panelCtx.getComponentId(), WebComponentUtil.createLocalizedModelForEnum((Enum<?>) object, panelCtx.getPageBase()));
        } else if (object instanceof PolyString) {
            labelPanel = new Label(panelCtx.getComponentId(), LocalizationUtil.translatePolyString((PolyString) object));
        } else if (object instanceof Boolean) {
            labelPanel = new Label(panelCtx.getComponentId(), WebComponentUtil.createLocalizedModelForBoolean((Boolean) object));
        } else if (object instanceof ProtectedStringType) {
            if (StringUtils.isNotEmpty(((ProtectedStringType) object).getClearValue())
                    || ((ProtectedStringType) object).getEncryptedDataType() != null) {
                labelPanel = new Label(panelCtx.getComponentId(), panelCtx.getPageBase().createStringResource("passwordPanel.passwordSet"));
            } else {
                labelPanel = new Label(panelCtx.getComponentId(), Model.of());
            }
        } else if (object instanceof VariableBindingDefinitionType variableBindingDefinition) {
            ItemPathType path = variableBindingDefinition.getPath();
            labelPanel = new Label(panelCtx.getComponentId(), Model.of(path != null ? path.toString() : ""));
        } else if (panelCtx.getDefinitionName().equivalent(MappingType.F_EXPRESSION)) {
            return createExpressionReadOnlyPanel(panelCtx);
        } else if (object instanceof SearchFilterType) {
            labelPanel = new Label(panelCtx.getComponentId(), ((SearchFilterType) object).getText());
        } else {
            labelPanel = new Label(panelCtx.getComponentId(), panelCtx.getRealValueStringModel());
        }
        labelPanel.add(AttributeModifier.append("class", "prism-property-value-label"));
        return labelPanel;
    }

    private static <T> @NotNull ExpressionPanel createExpressionReadOnlyPanel(@NotNull PrismPropertyPanelContext<T> panelCtx) {
        ExpressionType expression = (ExpressionType) panelCtx.getRealValueModel().getObject();

        IModel<ExpressionType> expressionModel = new LoadableModel<>() {
            @Override
            protected ExpressionType load() {
                if (expression == null) {
                    return new ExpressionType();
                }
                return expression;
            }
        };

        return new ExpressionPanel(panelCtx.getComponentId(),
                expressionModel) {
            @Override
            protected boolean isReadOnly() {
                return true;
            }
        };
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}

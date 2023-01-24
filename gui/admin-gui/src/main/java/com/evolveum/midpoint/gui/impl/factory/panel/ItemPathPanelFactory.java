/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.web.util.ExpressionValidator;

import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author katka
 */
@Component
public class ItemPathPanelFactory extends AbstractGuiComponentFactory<ItemPathType> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired private transient GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return ItemPathType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        return new ItemPathPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel().getObject()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                panelCtx.getRealValueModel().setObject(new ItemPathType(itemPathDto.toItemPath()));

            }
        };
    }

    @Override
    public void configure(PrismPropertyPanelContext<ItemPathType> panelCtx, org.apache.wicket.Component component) {
        if (!(component instanceof InputPanel)) {
            panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(component));
            return;
        }
        InputPanel panel = (InputPanel) component;
        final List<FormComponent> formComponents = panel.getFormComponents();
        for (FormComponent<ItemPathType> formComponent : formComponents) {
            PrismPropertyWrapper<ItemPathType> propertyWrapper = panelCtx.unwrapWrapperModel();
            IModel<String> label = LambdaModel.of(propertyWrapper::getDisplayName);
            formComponent.setLabel(label);
            if (panelCtx.isMandatory()) {
                formComponent.add(new NotNullValidator<>("Required"));
            }
            formComponent.add(panelCtx.getAjaxEventBehavior());
            formComponent.add(panelCtx.getVisibleEnableBehavior());
        }

        ExpressionValidator ev = panelCtx.getExpressionValidator();
        if (ev != null) {
            panel.getValidatableComponent().add(ev);
        }
        panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(panel.getValidatableComponent()));
    }
}

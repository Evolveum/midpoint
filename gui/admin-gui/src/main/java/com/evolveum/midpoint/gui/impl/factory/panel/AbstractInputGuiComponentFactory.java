/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.validator.ChoiceRequiredValidator;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.web.component.behavior.CaretPreservingOnChangeBehavior;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;

import com.evolveum.midpoint.web.util.ExpressionValidator;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import static java.util.Map.entry;

/**
 * abstract factory for all InputPanel panels
 *
 * @param <T>
 */
public abstract class AbstractInputGuiComponentFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    private final static String IS_AI_FLAG_FIELD_CLASS = "is-ai-flag";

    @Autowired private GuiComponentRegistry componentRegistry;

    private static final Map<Class<? extends Containerable>, List<ItemName>> SCHEMA_CHOICES_DIFINITIONS = Map.ofEntries(
            entry(NotificationMessageAttachmentType.class,
                    List.of(NotificationMessageAttachmentType.F_CONTENT, NotificationMessageAttachmentType.F_CONTENT_FROM_FILE)));

    public GuiComponentRegistry getRegistry() {
        return componentRegistry;
    }

    @Override
    public Component createPanel(PrismPropertyPanelContext<T> panelCtx) {
        InputPanel panel = getPanel(panelCtx);
        return panel;
    }

    @Override
    public void configure(PrismPropertyPanelContext<T> panelCtx, Component component) {
        if (!(component instanceof InputPanel)) {
            return;
        }
        InputPanel panel = (InputPanel) component;
        final List<FormComponent> formComponents = panel.getFormComponents();
        for (FormComponent<T> formComponent : formComponents) {
            PrismPropertyWrapper<T> propertyWrapper = panelCtx.unwrapWrapperModel();
            IModel<String> label = LambdaModel.of(propertyWrapper::getDisplayName);
            formComponent.setLabel(label);

            markIfAiGeneratedValue(formComponent, propertyWrapper);

            Class<? extends Containerable> parentClass = getChoicesParentClass(panelCtx);
            if (parentClass != null) {
                panel.getValidatableComponent().add(
                        new ChoiceRequiredValidator(SCHEMA_CHOICES_DIFINITIONS.get(parentClass), panelCtx.getItemWrapperModel()));
            } else if (panelCtx.isMandatory()) {
                formComponent.add(new NotNullValidator<>("Required", panelCtx.getItemWrapperModel()));
            }

            if (formComponent instanceof TextField) {
                formComponent.add(new AttributeModifier("size", "42"));
            }
            formComponent.add(panelCtx.getVisibleEnableBehavior());
            if (panelCtx.getAttributeValuesMap() != null) {
                panelCtx.getAttributeValuesMap().keySet().stream()
                        .forEach(a -> formComponent.add(AttributeAppender.replace(a, panelCtx.getAttributeValuesMap().get(a))));
            }
        }

        if (panelCtx.getAjaxEventBehavior() != null) {
            panel.getBaseFormComponent().add(panelCtx.getAjaxEventBehavior());
        }

        ExpressionValidator ev = panelCtx.getExpressionValidator();
        if (ev != null) {
            panel.getValidatableComponent().add(ev);
        }
        panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(panel.getValidatableComponent()));

    }

    /**
     * Adds a CSS class to the given form component if any of the property values are marked as AI-provided.
     * <p>
     * If the field was originally AI-generated (based on the old value), attaches an AJAX "blur" behavior
     * that refreshes the component after the user leaves the field, ensuring smooth UI updates without
     * affecting cursor position while typing.
     */
    private static <T> void markIfAiGeneratedValue(
            @NotNull FormComponent<T> formComponent,
            @NotNull PrismPropertyWrapper<T> propertyWrapper) {

        boolean isAiRelated = hasAiMark(propertyWrapper, false);
        if (isAiRelated) {
            formComponent.setOutputMarkupId(true);

            formComponent.add(AttributeModifier.append("class", () -> {
                boolean hasAiProvidedValue = hasAiMark(propertyWrapper, true);
                return hasAiProvidedValue && !formComponent.hasErrorMessage()
                        ? IS_AI_FLAG_FIELD_CLASS
                        : "";
            }));

            formComponent.setOutputMarkupId(true);
            formComponent.add(new CaretPreservingOnChangeBehavior());
        }
    }

    private static <T> boolean hasAiMark(
            @NotNull PrismPropertyWrapper<T> propertyWrapper, boolean newValue) {
        return propertyWrapper.getValues() != null &&
                propertyWrapper.getValues().stream()
                        .anyMatch(vw -> AiUtil.isMarkedAsAiProvided(
                                newValue ? vw.getNewValue() : vw.getOldValue()));
    }

    private Class<? extends Containerable> getChoicesParentClass(PrismPropertyPanelContext<T> panelCtx) {
        ItemWrapper<?, ?> wrapper = panelCtx.unwrapWrapperModel();
        if (wrapper == null) {
            return null;
        }

        for (Class<? extends Containerable> parentClass : SCHEMA_CHOICES_DIFINITIONS.keySet()) {
            if (wrapper.getParentContainerValue(parentClass) == null) {
                continue;
            }

            if (SCHEMA_CHOICES_DIFINITIONS.get(parentClass).stream().anyMatch(choice -> choice.equivalent(wrapper.getItemName()))) {
                return parentClass;
            }
        }
        return null;
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    protected abstract InputPanel getPanel(PrismPropertyPanelContext<T> panelCtx);
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;

import org.apache.wicket.validation.Validatable;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validator.MappingNameValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;
import com.evolveum.midpoint.gui.impl.event.FormComponentUpdatingEvent;
import com.evolveum.midpoint.gui.impl.util.MappingNameGenerator;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;

@org.springframework.stereotype.Component
public class MappingNamePanelFactory extends TextPanelFactory<String> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingNamePanelFactory.class);

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper == null || wrapper.getPath().isEmpty() || wrapper.getPath().lastName() == null) {
            return false;
        }

        if (!AbstractMappingType.F_NAME.equivalent(wrapper.getItemName())) {
            return false;
        }

        if (wrapper.getParent() == null) {
            return false;
        }

        return AbstractMappingType.class.isAssignableFrom(wrapper.getParent().getParent().getTypeClass());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        panelCtx.setMandatoryHandler(itemWrapper -> true);
        return super.getPanel(panelCtx);
    }

    @Override
    public void configure(PrismPropertyPanelContext<String> panelCtx, Component component) {
        super.configure(panelCtx, component);
        InputPanel panel = (InputPanel) component;
        //noinspection unchecked
        panel.getValidatableComponent().add(createValidator(panelCtx));
        panel.setOutputMarkupId(true);
        panel.add(createMappingNameBehavior(panelCtx, panel));
    }

    /**
     * Creates a listener for changes belonging to this mapping or its enclosing attribute or template item reference.
     * Manual name changes remove SYSTEM provenance and do not trigger immediate regeneration.
     * Name generation waits until the AJAX response because the change event precedes the model update.
     */
    private Behavior createMappingNameBehavior(PrismPropertyPanelContext<String> panelCtx, InputPanel panel) {
        return new Behavior() {
            /** Schedules generation for a related form update after its input model has been updated. */
            @Override
            public void onEvent(Component component, IEvent<?> event) {
                if (!(event.getPayload() instanceof FormComponentUpdatingEvent update) || !panelCtx.isEditable()) {
                    return;
                }
                if (update.component() == panel.getValidatableComponent()) {
                    clearSystemMarkOnManualNameChange(panelCtx, panel, update.AjaxRequestTarget());
                    return;
                }
                if (isChangeForMapping(update.component(), panelCtx.unwrapWrapperModel().getParent())) {
                    scheduleMappingNameUpdate(panelCtx, panel, update.AjaxRequestTarget());
                }
            }
        };
    }

    /**
     * Removes SYSTEM provenance only when the user submits a different value for a marked name.
     * The event precedes input processing, so the submitted text is compared with the current model.
     */
    private void clearSystemMarkOnManualNameChange(
            PrismPropertyPanelContext<String> panelCtx, InputPanel panel, AjaxRequestTarget target) {
        String submittedName = panel.getValidatableComponent().getInput();
        PrismValue nameValue = panelCtx.getValueWrapperModel().getObject().getNewValue();
        if (SmartMetadataUtil.isMarkedAsSystemProvided(nameValue)
                && !Objects.equals(submittedName, panelCtx.getRealValueModel().getObject())) {
            SmartMetadataUtil.unmarkAsSystemProvided(nameValue);
            if (submittedName != null && !submittedName.isEmpty()) {
                showAutomaticNamingDisabledToast(target, submittedName);
            }
        }
    }

    /**
     * Defers name generation until the changed input has updated its model, then validates and refreshes the name.
     */
    private void scheduleMappingNameUpdate(
            PrismPropertyPanelContext<String> panelCtx, InputPanel panel, AjaxRequestTarget target) {
        target.addListener(new AjaxRequestTarget.IListener() {
            /** Uses the updated endpoints before rendering the name and its validation feedback. */
            @Override
            public void onBeforeRespond(Map<String, Component> components, AjaxRequestTarget responseTarget) {
                if (updateMappingName(panelCtx, panel, responseTarget)) {
                    var input = panel.getValidatableComponent();
                    input.getFeedbackMessages().clear();

                    var value = new Validatable<>(panelCtx.getRealValueModel().getObject());
                    createValidator(panelCtx).validate(value);
                    value.getErrors().forEach(input::error);

                    responseTarget.add(panel, panelCtx.getFeedback());
                }
            }
        });
    }

    /**
     * Fills an empty name or refreshes an editable name value marked as SYSTEM-provided.
     * Custom names and names on incomplete mappings are preserved.
     */
    private boolean updateMappingName(
            PrismPropertyPanelContext<String> panelCtx, InputPanel panel, AjaxRequestTarget target) {
        var input = panel.getValidatableComponent();
        var nameModel = panelCtx.getRealValueModel();

        String currentName = input.hasRawInput()
                ? input.getRawInput()
                : nameModel.getObject();

        if (!panelCtx.isEditable()) {
            return false;
        }

        try {
            String name = MappingNameGenerator.suggestUpdatedName(
                    panelCtx.unwrapWrapperModel().getParent(), currentName);
            if (name != null) {
                panelCtx.getRealValueModel().setObject(name);
                PrismValue nameValue = panelCtx.getValueWrapperModel().getObject().getNewValue();
                SmartMetadataUtil.markAsSystemProvided(nameValue);
                panel.getBaseFormComponent().clearInput();
                target.add(panel);
                showMappingNameUpdatedToast(target, name);
                return true;
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't generate mapping name", e);
        }
        return false;
    }

    /** Notifies the user when an automatic mapping name suggestion is applied. */
    private void showMappingNameUpdatedToast(AjaxRequestTarget target, String name) {
        new Toast()
                .info()
                .title(translate(
                        "MappingNamePanelFactory.nameUpdated.title"))
                .icon("fa fa-exclamation")
                .autohide(true)
                .delay(5_000)
                .body(translate(
                        "MappingNamePanelFactory.nameUpdated.body", StringEscapeUtils.escapeHtml4(name)))
                .show(target);
    }

    /** Explains how to restore automatic naming after a manual name change. */
    private void showAutomaticNamingDisabledToast(AjaxRequestTarget target, String name) {
        new Toast()
                .warning()
                .title(translate(
                        "MappingNamePanelFactory.automaticNamingDisabled.title"))
                .icon("fa fa-exclamation-triangle")
                .autohide(true)
                .delay(5_000)
                .body(translate(
                        "MappingNamePanelFactory.automaticNamingDisabled.body", StringEscapeUtils.escapeHtml4(name)))
                .show(target);
    }

    /**
     * Resolves wrapper models up the component hierarchy to scope a change to one mapping.
     * A change to the enclosing attribute or template item reference also affects the mapping's implicit endpoint.
     */
    private static boolean isChangeForMapping(Component component, PrismContainerValueWrapper<?> mapping) {
        for (Component current = component; current != null; current = current.getParent()) {
            Object model = current.getDefaultModelObject();
            PrismContainerValueWrapper<?> parent;
            ItemWrapper<?, ?> item;
            if (model instanceof PrismContainerValueWrapper<?> container) {
                item = container.getParent();
                parent = container;
            } else {
                if (model instanceof ItemWrapper<?, ?> itemWrapper) {
                    item = itemWrapper;
                } else if (model instanceof PrismValueWrapper<?> value) {
                    item = value.getParent();
                } else {
                    continue;
                }
                parent = item != null ? item.getParent() : null;
            }
            if (parent != null && item != null && parent == mapping.getParent().getParent()
                    && parent.getRealValue() instanceof ItemRefinedDefinitionType
                    && ItemRefinedDefinitionType.F_REF.equivalent(item.getItemName())) {
                return true;
            }
            while (parent != null) {
                if (parent.getRealValue() instanceof MappingType) {
                    return parent == mapping;
                }
                parent = parent.getParent() != null ? parent.getParent().getParent() : null;
            }
        }
        return false;
    }

    protected MappingNameValidator createValidator(PrismPropertyPanelContext<String> panelCtx) {
        return new MappingNameValidator(panelCtx.getItemWrapperModel());
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}

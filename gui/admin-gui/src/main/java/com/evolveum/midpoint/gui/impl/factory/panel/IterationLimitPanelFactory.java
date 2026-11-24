/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.form.FormComponent;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.event.FormComponentUpdatingEvent;
import com.evolveum.midpoint.gui.impl.event.ItemValueRemovedEvent;
import com.evolveum.midpoint.gui.impl.validator.IterationLimitValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;

/** Applies to iteration limit inputs in standard forms and directly constructed drawer editors. */
@org.springframework.stereotype.Component
public class IterationLimitPanelFactory extends TextPanelFactory<Integer> {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return wrapper instanceof PrismPropertyWrapper && !wrapper.isMetadata() && wrapper.getParent() != null
                && wrapper.getParent().getDefinition() != null
                && IterationSpecificationType.COMPLEX_TYPE.equals(wrapper.getParent().getDefinition().getTypeName())
                && (IterationSpecificationType.F_END.equivalent(wrapper.getItemName())
                || IterationSpecificationType.F_MAX_ITERATIONS.equivalent(wrapper.getItemName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(PrismPropertyPanelContext<Integer> panelCtx, Component component) {
        super.configure(panelCtx, component);
        FormComponent<Integer> input = ((InputPanel) component).getValidatableComponent();
        var validator = createValidator(panelCtx, input);
        input.add(validator);
        input.setOutputMarkupId(true);
        // Add a warning style for custom warning messages.
        input.add(AttributeAppender.append("class", () ->
                validator.hasWarning() && !input.hasErrorMessage() ? "border-warning" : ""));
        input.add(new IterationLimitBehavior(panelCtx, input, validator));
    }

    /** Creates the non-blocking warning check for an iteration limit. */
    protected IterationLimitValidator createValidator(
            PrismPropertyPanelContext<Integer> panelCtx, FormComponent<Integer> input) {
        return new IterationLimitValidator(panelCtx.getItemWrapperModel(), input);
    }

    /** Keeps iteration warnings up to date when either limit changes or is removed. */
    private static class IterationLimitBehavior extends Behavior {
        private final PrismPropertyPanelContext<Integer> context;

        private final FormComponent<Integer> input;
        private final IterationLimitValidator validator;

        private IterationLimitBehavior(PrismPropertyPanelContext<Integer> context,
                FormComponent<Integer> input, IterationLimitValidator validator) {
            this.context = context;
            this.input = input;
            this.validator = validator;
        }

        @Override
        public void onConfigure(Component component) {
            validator.refreshWarning(input.getModelObject()); //initial display
        }

        @Override
        public void onEvent(Component component, IEvent<?> event) {
            if (event.getPayload() instanceof ItemValueRemovedEvent removed) {
                var item = context.unwrapWrapperModel();
                if (removed.item() != item && removed.item().getParent() == item.getParent()
                        && (IterationSpecificationType.F_END.equivalent(removed.item().getItemName())
                        || IterationSpecificationType.F_MAX_ITERATIONS.equivalent(removed.item().getItemName()))) {
                    // The removed field is refreshed by ItemPanel. Only refresh its surviving sibling here.
                    scheduleWarningRefresh(removed.target());
                }
                return;
            }
            if (!(event.getPayload() instanceof FormComponentUpdatingEvent update)) {
                return;
            }
            for (var source : update.component().getBehaviors(IterationLimitBehavior.class)) {
                if (source.context.unwrapWrapperModel().getParent() == context.unwrapWrapperModel().getParent()) {
                    scheduleWarningRefresh(update.AjaxRequestTarget());
                    break;
                }
            }
        }

        /** Refreshes the warning and field styling after input processing finishes. */
        private void scheduleWarningRefresh(AjaxRequestTarget target) {
            target.addListener(new AjaxRequestTarget.IListener() {
                @Override
                public void onBeforeRespond(Map<String, Component> components, AjaxRequestTarget responseTarget) {
                    validator.refreshWarning(input.getModelObject());
                    responseTarget.add(input, context.getFeedback());
                }
            });
        }
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }
}

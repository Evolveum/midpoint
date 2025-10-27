/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.web.component.behavior;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * An {@link OnChangeAjaxBehavior} that preserves the text caret position
 * across AJAX updates for input components.
 * <p>
 * Useful for components where maintaining cursor location improves user experience
 * during dynamic updates.
 */
public class CaretPreservingOnChangeBehavior extends OnChangeAjaxBehavior {

    /** Debounce for instant single-click components (checkboxes). */
    protected static final Duration INSTANT_DEBOUNCE = Duration.ZERO;

    /** For responsive selection inputs (dropdowns, radios). */
    protected static final Duration RESPONSIVE_DEBOUNCE = Duration.ofMillis(200);

    /** For typing-based inputs (text fields, text areas). */
    protected static final Duration TYPING_DEBOUNCE = Duration.ofMillis(800);

    public CaretPreservingOnChangeBehavior() {
    }

    /**
     * Adds client-side logic to capture caret position before the AJAX call.
     * Injects JavaScript parameters representing the selection start and end indices.
     */
    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);

        attributes.setThrottlingSettings(new ThrottlingSettings(getDebounceInterval(), true));
        attributes.getDynamicExtraParameters().add(
                "var el = Wicket.$('" + getAffectedComponentMarkupId() + "');" +
                        "return el ? { caretStart: el.selectionStart, caretEnd: el.selectionEnd } : {};"
        );
    }

    /**
     * Restores the caret position after the component is refreshed by AJAX.
     * Also invokes {@link #onCaretAwareUpdate(AjaxRequestTarget)} for additional updates.
     */
    @Override
    protected void onUpdate(AjaxRequestTarget target) {
        var params = RequestCycle.get().getRequest().getRequestParameters();
        int start = params.getParameterValue("caretStart").toInt(0);
        int end = params.getParameterValue("caretEnd").toInt(0);

        onCaretAwareUpdate(target);

        // Restore caret position after AJAX update (consider moving to midpoint-theme.js)
        target.appendJavaScript(String.format(
                "setTimeout(function() {" +
                        "  var el = Wicket.$('%s');" +
                        "  if (el && el.setSelectionRange) {" +
                        "    el.focus();" +
                        "    el.setSelectionRange(%d, %d);" +
                        "  }" +
                        "}, 0);",
                getAffectedComponentMarkupId(), start, end
        ));
    }

    /**
     * Returns the markup ID of the affected component.
     * This is used in generated JavaScript for caret restoration.
     */
    private String getAffectedComponentMarkupId() {
        return getAffectedComponent().getMarkupId();
    }

    /**
     * Performs the actual component update during the AJAX callback.
     * Subclasses may override to extend update behavior.
     */
    protected void onCaretAwareUpdate(@NotNull AjaxRequestTarget target) {
        target.add(getAffectedComponent());
    }

    /**
     * Returns the component affected by the caret preservation logic.
     * Default implementation returns {@link #getComponent()}.
     */
    protected Component getAffectedComponent() {
        return getComponent();
    }

    /**
     * Determines debounce interval dynamically based on component type.
     * <p>
     * - {@link CheckBox}: {@link #INSTANT_DEBOUNCE}<br>
     * - {@link DropDownChoice}: {@link #RESPONSIVE_DEBOUNCE}<br>
     * - Other (e.g. text fields): {@link #TYPING_DEBOUNCE}
     */
    protected Duration getDebounceInterval() {
        Component component = getAffectedComponent();

        if (component instanceof CheckBox) {
            return INSTANT_DEBOUNCE;
        }

        if (component instanceof DropDownChoice) {
            return RESPONSIVE_DEBOUNCE;
        }

        return TYPING_DEBOUNCE;
    }
}

/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.togglebutton;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

/**
 * Simple button that toggles two states (on-off, alphasort-numericsort, etc).
 * The button changes the icon when pressed.
 *
 * @author semancik
 */
public abstract class ToggleIconButton<T> extends AjaxLink<T> {
    private static final long serialVersionUID = 1L;

    private String cssClassOff;
    private String cssClassOn;

    public ToggleIconButton(String id) {
        super(id);
        initLayout();
    }

    public ToggleIconButton(String id, String cssClassOff, String cssClassOn) {
        super(id);
        this.cssClassOff = cssClassOff;
        this.cssClassOn = cssClassOn;
        initLayout();
    }

    public ToggleIconButton(String id, IModel<T> model) {
        super(id, model);
        initLayout();
    }

    public ToggleIconButton(String id, IModel<T> model, String cssClassOff, String cssClassOn) {
        super(id, model);
        this.cssClassOff = cssClassOff;
        this.cssClassOn = cssClassOn;
        initLayout();
    }

    private void initLayout() {
        setEscapeModelStrings(false);

        setBody(() -> {
            String cssClass = isOn() ? cssClassOn : cssClassOff;

            return "<i class=\"" + Strings.escapeMarkup(cssClass) + "\"></i>";
        });
    }

    public abstract boolean isOn();

}

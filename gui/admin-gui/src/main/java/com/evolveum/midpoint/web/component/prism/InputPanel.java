/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.gui.api.Validatable;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class InputPanel extends Panel implements Validatable {

    private static final long serialVersionUID = 1L;

	public InputPanel(String id) {
        super(id);
    }

    public List<FormComponent> getFormComponents() {
        return Arrays.asList(getBaseFormComponent());
    }

    public abstract FormComponent getBaseFormComponent();

    @Override
    public FormComponent getValidatableComponent() {
        return getBaseFormComponent();
    }
}

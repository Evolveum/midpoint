/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api;

import org.apache.wicket.markup.html.form.FormComponent;

import java.io.Serializable;

public interface Validatable extends Serializable {

    FormComponent getValidatableComponent();
}

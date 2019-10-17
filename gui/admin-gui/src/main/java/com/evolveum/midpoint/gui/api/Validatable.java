/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api;

import org.apache.wicket.markup.html.form.FormComponent;

import java.io.Serializable;

public interface Validatable extends Serializable {

    FormComponent getValidatableComponent();
}

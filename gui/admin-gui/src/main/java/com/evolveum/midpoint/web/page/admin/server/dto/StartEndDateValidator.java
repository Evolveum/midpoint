/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;

public class StartEndDateValidator extends AbstractFormValidator {

    private DateTimeField notBefore;
    private DateTimeField notAfter;

    public StartEndDateValidator(DateTimeField notBefore, DateTimeField notAfter) {
        this.notBefore = notBefore;
        this.notAfter = notAfter;
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        return new FormComponent[] { notBefore, notAfter};
    }

    @Override
    public void validate(Form<?> form) {
        if (notBefore.getConvertedInput() == null || notAfter.getConvertedInput() == null)
            return;

        if (notBefore.getConvertedInput().after(notAfter.getConvertedInput())) {
            error(notBefore, "pageTask.notStartBefore.error1");
        }
    }

}

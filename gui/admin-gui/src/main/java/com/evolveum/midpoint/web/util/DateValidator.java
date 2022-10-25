/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import java.util.Date;

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;

import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author lazyman
 */
public class DateValidator extends AbstractFormValidator {

    private static final long serialVersionUID = 1L;

    private ItemPath identifier;
    private DateTimeField dateFrom;
    private DateTimeField dateTo;
    private String messageKey = "DateValidator.defaultErrorMessage";

    public DateValidator() {
        this(null, null);
    }

    public DateValidator(DateTimeField dateFrom, DateTimeField dateTo) {
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        if (dateFrom == null || dateTo == null) {
            return new FormComponent[0];
        }

        if (!dateFrom.isVisibleInHierarchy() || !dateTo.isVisibleInHierarchy()) {
            // fields are not visible, should not be validated
            return new FormComponent[0];
        }

        return new FormComponent[] { dateFrom, dateTo };
    }

    @Override
    public void validate(Form<?> form) {
        if (dateFrom == null || dateTo == null) {
            return;
        }

        Date from = dateFrom.getConvertedInput();
        Date to = dateTo.getConvertedInput();

        if (from == null || to == null) {
            return;
        }

        if (from.after(to)) {
            form.error(form.getString(getMessageKey()));
        }
    }

    public void setDateFrom(DateTimeField dateFrom) {
        this.dateFrom = dateFrom;
    }

    public void setDateTo(DateTimeField dateTo) {
        this.dateTo = dateTo;
    }

    public ItemPath getIdentifier() {
        return identifier;
    }

    public void setIdentifier(ItemPath identifier) {
        this.identifier = identifier;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }
}

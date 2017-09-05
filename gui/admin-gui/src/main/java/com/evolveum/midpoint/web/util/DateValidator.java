/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;

import java.util.Date;

/**
 * @author lazyman
 */
public class DateValidator extends AbstractFormValidator {

   private static final long serialVersionUID = 1L;

   private ItemPath identifier;
    private DateTimeField dateFrom;
    private DateTimeField dateTo;
    private String messageKey = "DateValidator.message.fromAfterTo";

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

        return new FormComponent[]{dateFrom, dateTo};
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

    public void setMessageKey(String messageKey){
        this.messageKey = messageKey;
    }
}

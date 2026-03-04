/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import java.util.List;

import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component
public class UniqueObjectNameValidatorFactory extends ItemValidatorFactory {

    public static final String IDENTIFIER = "UniqueObjectName";

    public UniqueObjectNameValidatorFactory() {
        super(IDENTIFIER);
    }

    @Override
    public void attachValidator(InputPanel panel, ItemValidationContext context) {
        FormComponent<?> formComponent = panel.getBaseFormComponent();
        if (formComponent instanceof AbstractTextComponent<?> text) {
            UniqueObjectNameValidator<?> validator = new UniqueObjectNameValidator<>(context.type(), context.page());
            text.add(validator);
        }
    }

    private static class UniqueObjectNameValidator<O extends ObjectType> implements IValidator<Object> {

        private final Class<O> type;

        private final PageBase pageBase;

        public UniqueObjectNameValidator(Class<O> type, PageBase pageBase) {
            this.type = type;
            this.pageBase = pageBase;
        }

        @Override
        public void validate(@NotNull IValidatable validatable) {
            Object value = validatable.getValue();
            String strValue = value != null ? value.toString() : null;

            ObjectQuery query = PrismContext.get().queryFor(type)
                    .item(ObjectType.F_NAME).eqPoly(strValue).matchingNorm()
                    .maxSize(1)
                    .build();

            List<PrismObject<O>> result = WebModelServiceUtils.searchObjects(type, query, null, pageBase);
            if (result.isEmpty()) {
                return;
            }

            ValidationError error = new ValidationError(this);
            error.addKey("UniqueObjectNameValidator.notUnique");

            validatable.error(error);
        }
    }
}

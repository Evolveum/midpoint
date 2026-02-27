/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import java.util.List;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component
public class UniqueObjectNameValidatorFactory extends ItemValidatorFactory<String> {

    public UniqueObjectNameValidatorFactory() {
        super("UniqueObjectName");
    }

    @Override
    public IValidator<String> createValidatorInstance(ItemValidationContext context) {
        return new UniqueObjectNameValidator<>(context.type(), context.page());
    }

    private static class UniqueObjectNameValidator<O extends ObjectType> implements IValidator<String> {

        private final Class<O> type;

        private final PageBase pageBase;

        public UniqueObjectNameValidator(Class<O> type, PageBase pageBase) {
            this.type = type;
            this.pageBase = pageBase;
        }

        @Override
        public void validate(IValidatable<String> validatable) {
            String value = validatable.getValue();

            ObjectQuery query = PrismContext.get().queryFor(type)
                    .item(ObjectType.F_NAME).eqPoly(value)
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

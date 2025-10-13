/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.util.QNameUtil;

public class RelationValidator implements IValidator<String> {

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (QNameUtil.isUri(value)) {
            return;
        }

        ValidationError error = new ValidationError();
        error.addKey("RelationPanel.relation.identifier.must.be.qname");
        error.setMessage("Relation identifier must be in the form of URI.");
        validatable.error(error);
    }
}

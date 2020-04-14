/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.util.QNameUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

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
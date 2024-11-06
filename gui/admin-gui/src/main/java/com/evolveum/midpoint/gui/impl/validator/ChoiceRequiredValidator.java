/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChoiceRequiredValidator<T> implements INullAcceptingValidator<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ChoiceRequiredValidator.class);

    private final List<ItemName> choices;
    private final IModel<PrismPropertyWrapper> itemWrapper;

    public ChoiceRequiredValidator(List<ItemName> choices, IModel<PrismPropertyWrapper> itemWrapper) {
        this.choices = choices;
        this.itemWrapper = itemWrapper;
    }

    @Override
    public void validate(IValidatable<T> validatable) {
        T value = validatable.getValue();
        if (value != null) {
            return;
        }

        PrismContainerValueWrapper parent = itemWrapper.getObject().getParent();
        if (parent == null) {
            return;
        }

        List<String> names = new ArrayList<>();
        for (ItemName choice : choices) {
            try {
                ItemWrapper choiceItem = parent.findItem(choice);

                names.add("'" + WebPrismUtil.getLocalizedDisplayName(choiceItem.getItem()) + "'");

                if (choice.equivalent(itemWrapper.getObject().getItemName())) {
                    continue;
                }

                if (choiceItem != null && !choiceItem.getValues().isEmpty()) {
                    for (PrismValueWrapper choiceItemValue : (List<PrismValueWrapper>)choiceItem.getValues()) {
                        if (choiceItemValue != null && choiceItemValue.getRealValue() != null) {
                            return;
                        }
                    }
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find item for path '" + choice + "' in parent " + parent );
            }
        }

        ValidationError error = new ValidationError();
        error.setMessage(LocalizationUtil.translate(
                "ChoiceRequiredValidator.missingChoiceValues",
                new Object[] { String.join(", ", names) }));
        validatable.error(error);
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.CreateObjectForReferenceValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectFocusSpecificationType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import javax.xml.namespace.QName;

public class ResourceObjectFocusTypeValidator implements INullAcceptingValidator<QName> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectFocusTypeValidator.class);

    private final IModel<PrismPropertyWrapper<QName>> itemModel;

    public ResourceObjectFocusTypeValidator(IModel<PrismPropertyWrapper<QName>> itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public void validate(IValidatable<QName> validatable) {
        PrismContainerValueWrapper<ResourceObjectFocusSpecificationType> parent =
                itemModel.getObject().getParentContainerValue(ResourceObjectFocusSpecificationType.class);

        if (parent == null) {
            return;
        }

        boolean checkNull = false;
        try {
            PrismReferenceWrapper<Referencable> archetypeWrapper =
                    parent.findReference(ResourceObjectFocusSpecificationType.F_ARCHETYPE_REF);
            if (archetypeWrapper == null) {
                return;
            }
            PrismReferenceValueWrapperImpl<Referencable> archetypeValueWrapper = archetypeWrapper.getValue();
            if (!(archetypeValueWrapper instanceof CreateObjectForReferenceValueWrapper<Referencable>)) {
                return;
            }
            checkNull = archetypeValueWrapper.isNewObjectModelCreated();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find archetype reference wrapper in " + parent, e);
        }

        QName value = validatable.getValue();
        if (value == null && checkNull) {
            ValidationError error = new ValidationError();
            error.setMessage(LocalizationUtil.translate("ResourceObjectFocusTypeValidator.nullValueDuringCreatingOfArchetype", new Object[] {value}));
            validatable.error(error);
        }
    }
}

/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

public class BaseContextConsistencyValidator implements IValidator<Object>, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(BaseContextConsistencyValidator.class);

    private final IModel<PrismPropertyWrapper<?>> itemModel;

    public BaseContextConsistencyValidator(IModel<PrismPropertyWrapper<?>> itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public void validate(IValidatable<Object> validatable) {

        PrismPropertyWrapper<?> item = itemModel.getObject();
        if (item == null) {
            return;
        }

        try {
            PrismContainerValueWrapper<?> parent =
                    item.getParentContainerValue(ResourceObjectTypeDefinitionType.class);

            if (parent == null) {
                return;
            }

            PrismPropertyWrapper<QName> objectClassWrapper =
                    parent.findProperty(ItemPath.create(
                            ResourceObjectTypeDefinitionType.F_DELINEATION,
                            ResourceObjectTypeDelineationType.F_BASE_CONTEXT,
                            ResourceObjectReferenceType.F_OBJECT_CLASS));

            PrismPropertyWrapper<SearchFilterType> filterWrapper =
                    parent.findProperty(ItemPath.create(
                            ResourceObjectTypeDefinitionType.F_DELINEATION,
                            ResourceObjectTypeDelineationType.F_BASE_CONTEXT,
                            ResourceObjectReferenceType.F_FILTER));

            QName objectClass = objectClassWrapper != null && objectClassWrapper.getValue() != null
                    ? objectClassWrapper.getValue().getRealValue()
                    : null;

            boolean hasObjectClass = objectClass != null;
            boolean hasFilter = hasNonEmptyFilter(filterWrapper);

            if (hasFilter && !hasObjectClass) {
                validatable.error(new ValidationError()
                        .setMessage(LocalizationUtil.translate(
                                "ResourceObjectTypeDelineation.baseContext.objectClass.required")));
            }

        } catch (Exception e) {
            LOGGER.debug("Couldn't resolve baseContext consistency.", e);
        }
    }

    private boolean hasNonEmptyFilter(PrismPropertyWrapper<SearchFilterType> wrapper) {
        if (wrapper == null || wrapper.getValues() == null) {
            return false;
        }

        List<? extends PrismPropertyValueWrapper<SearchFilterType>> values = wrapper.getValues();

        return values != null && !values.isEmpty();
    }

}

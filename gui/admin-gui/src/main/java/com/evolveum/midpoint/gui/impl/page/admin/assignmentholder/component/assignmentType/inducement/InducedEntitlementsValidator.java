/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InducedEntitlementsValidator implements MidpointFormValidator, Serializable {

    @Override
    public Collection<SimpleValidationError> validateObject(PrismObject<? extends ObjectType> object, Collection<ObjectDelta<? extends ObjectType>> deltas) {
        List<SimpleValidationError> errors = new ArrayList<>();
        for (ObjectDelta<?> delta : deltas) {
            if (AbstractRoleType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                switch (delta.getChangeType()) {
                    case MODIFY:
                        Collection<? extends ItemDelta<?, ?>> itemDeltas = delta.getModifications();
                        for (ItemDelta<?, ?> itemDelta : itemDeltas) {
                            if (itemDelta.getPath().equivalent(AbstractRoleType.F_INDUCEMENT) && itemDelta.getValuesToAdd() != null) {
                                for (PrismValue value : itemDelta.getValuesToAdd()) {
                                    errors.addAll(validateInducement(value.getRealValue()));
                                }
                            }
                        }
                        break;
                    case ADD:
                        if (delta != null && delta.getObjectToAdd().asObjectable() != null) {
                            for (AssignmentType assignment : ((AbstractRoleType) object.asObjectable()).getInducement()) {
                                errors.addAll(validateInducement(assignment));
                            }
                        }
                        break;
                }
            }
        }
        return errors;
    }

    private Collection<SimpleValidationError> validateInducement(AssignmentType assignment) {
        List<SimpleValidationError> errors = new ArrayList<>();
        //TODO impelemnt findContainer(ItemPath)
        com.evolveum.midpoint.prism.Item<PrismContainerValue<ResourceObjectAssociationType>, PrismContainerDefinition<ResourceObjectAssociationType>> association =
                assignment.asPrismContainerValue().findItem(ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION));
        if (association != null && !association.getValues().isEmpty()) {
            for (PrismContainerValue<ResourceObjectAssociationType> associationValue : association.getValues()) {
                PrismContainer<MappingType> outbound = associationValue.findContainer(ResourceObjectAssociationType.F_OUTBOUND);
                if (outbound == null || outbound.getValues().isEmpty()) {
                    SimpleValidationError error = new SimpleValidationError();
                    error.setMessage(PageBase.createStringResourceStatic("InducedEntitlementsPanel.validator.message").getString());
                    ItemPathType path = new ItemPathType();
                    path.setItemPath(ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION, ResourceObjectAssociationType.F_OUTBOUND));
                    error.setAttribute(path);
                    errors.add(error);
                }
            }
        }
        return errors;
    }

    @Override
    public Collection<SimpleValidationError> validateAssignment(AssignmentType assignment) {
        return new ArrayList<>();
    }
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityPair;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.repo.sql.helpers.modify.DeltaUpdaterUtils.clearExtension;

/**
 * Deals with updating assignment extensions by item deltas.
 *
 * Static {@link #handleWholeContainerDelta(RObject, RAssignment, ItemDelta, UpdateContext)} method
 * translates add/replace container to a set of item deltas.
 */
class AssignmentExtensionUpdate extends ExtensionUpdate<RAssignmentExtension, RAssignmentExtensionType> {

    AssignmentExtensionUpdate(RObject object, RAssignment assignment, ItemDelta<?, ?> delta, UpdateContext ctx) {
        super(object, delta, ctx, getOrCreateExtension(assignment), RAssignmentExtensionType.EXTENSION);
    }

    private static RAssignmentExtension getOrCreateExtension(RAssignment assignment) {
        RAssignmentExtension extension;
        if (assignment.getExtension() != null) {
            extension = assignment.getExtension();
        } else {
            extension = new RAssignmentExtension();
            extension.setOwner(assignment);
            extension.setTransient(true);
            assignment.setExtension(extension);
        }
        return extension;
    }

    @Override
    void processExtensionValues(RAnyConverter.ValueType valueType,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta,
            RepositoryUpdater repositoryUpdater) {

        pairsFromDelta.forEach(item -> {
            RAExtValue<?> val = (RAExtValue<?>) item.getRepository();
            val.setAnyContainer(extension);
            val.setExtensionType(extensionType);
        });

        switch (valueType) {
            case BOOLEAN:
                repositoryUpdater.update(extension.getBooleans(), pairsFromDelta);
                break;
            case DATE:
                repositoryUpdater.update(extension.getDates(), pairsFromDelta);
                break;
            case LONG:
                repositoryUpdater.update(extension.getLongs(), pairsFromDelta);
                break;
            case REFERENCE:
                repositoryUpdater.update(extension.getReferences(), pairsFromDelta);
                break;
            case STRING:
                repositoryUpdater.update(extension.getStrings(), pairsFromDelta);
                break;
            case POLY_STRING:
                repositoryUpdater.update(extension.getPolys(), pairsFromDelta);
                break;
            default:
                throw new AssertionError("Wrong value type: " + valueType);
        }
    }

    @Override
    @NotNull RAnyValue<?> convertToRValue(Integer itemId,
            RAnyConverter converter, PrismValue prismValueFromDelta) throws SchemaException {
        return converter.convertToRValue(prismValueFromDelta, true, itemId);
    }

    static void handleWholeContainerDelta(RObject object, RAssignment assignment, ItemDelta<?, ?> delta, UpdateContext ctx)
            throws SchemaException {
        RAssignmentExtension existingExtension = assignment.getExtension();

        // Because ADD for single-valued container is the same as REPLACE (really?) we treat ADD as a REPLACE here.
        if (existingExtension != null) {
            clearExtension(existingExtension, ctx.entityManager);
        }

        if (delta.isAdd() || delta.isReplace()) {
            if (existingExtension == null) {
                RAssignmentExtension extension = new RAssignmentExtension();
                extension.setOwner(assignment);
                assignment.setExtension(extension);
            }
            PrismContainerValue<?> prismExtension = (PrismContainerValue<?>) delta.getAnyValue();
            if (prismExtension != null) {
                for (Item<?, ?> item : prismExtension.getItems()) {
                    ItemDelta itemDelta = item.createDelta();
                    //noinspection unchecked
                    itemDelta.addValuesToAdd(item.getClonedValues());

                    new AssignmentExtensionUpdate(object, assignment, itemDelta, ctx)
                            .handleItemDelta();
                }
            }
        }
    }
}

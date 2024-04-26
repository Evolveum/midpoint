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
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtValue;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityPair;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.repo.sql.helpers.delta.UpdateDispatcher.isObjectExtensionDelta;
import static com.evolveum.midpoint.repo.sql.helpers.delta.UpdateDispatcher.isShadowAttributesDelta;
import static com.evolveum.midpoint.repo.sql.helpers.modify.DeltaUpdaterUtils.clearExtension;

/**
 *
 */
class ObjectExtensionUpdate extends ExtensionUpdate<RObject, RObjectExtensionType> {

    ObjectExtensionUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx) {
        super(object, delta, ctx, object, computeExtensionType(delta));
    }

    private ObjectExtensionUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx, RObjectExtensionType type) {
        super(object, delta, ctx, object, type);
    }

    private static RObjectExtensionType computeExtensionType(ItemDelta<?, ?> delta) {
        if (isObjectExtensionDelta(delta.getPath())) {
            return RObjectExtensionType.EXTENSION;
        } else if (isShadowAttributesDelta(delta.getPath())) {
            return RObjectExtensionType.ATTRIBUTES;
        } else {
            throw new IllegalStateException("Unknown extension type, shouldn't happen");
        }
    }

    @Override
    void processExtensionValues(RAnyConverter.ValueType valueType,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta,
            RepositoryUpdater repositoryUpdater) {

        pairsFromDelta.forEach(item -> {
            ROExtValue val = (ROExtValue) item.getRepository();
            val.setOwner(object);
            val.setOwnerType(extensionType);
        });

        //noinspection Duplicates
        switch (valueType) {
            case BOOLEAN:
                repositoryUpdater.update(object.getBooleans(), pairsFromDelta);
                break;
            case DATE:
                repositoryUpdater.update(object.getDates(), pairsFromDelta);
                break;
            case LONG:
                repositoryUpdater.update(object.getLongs(), pairsFromDelta);
                break;
            case REFERENCE:
                repositoryUpdater.update(object.getReferences(), pairsFromDelta);
                break;
            case STRING:
                repositoryUpdater.update(object.getStrings(), pairsFromDelta);
                break;
            case POLY_STRING:
                repositoryUpdater.update(object.getPolys(), pairsFromDelta);
                break;
            default:
                throw new AssertionError("Wrong value type: " + valueType);
        }
    }

    @Override
    @NotNull RAnyValue<?> convertToRValue(Integer itemId,
            RAnyConverter converter, PrismValue prismValueFromDelta) throws SchemaException {
        return converter.convertToRValue(prismValueFromDelta, false, itemId);
    }

    static void handleWholeContainerDelta(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx) throws SchemaException {

        RObjectExtensionType extensionType = computeExtensionType(delta);

        // Because ADD for single-valued container is the same as REPLACE (really?) we treat ADD as a REPLACE here.
        clearExtension(object, extensionType, ctx.entityManager);

        if (delta.isAdd() || delta.isReplace()) {
            PrismContainerValue<?> extension = (PrismContainerValue<?>) delta.getAnyValue();
            if (extension != null) {
                for (Item<?, ?> item : extension.getItems()) {
                    ItemDelta<?, ?> itemDelta = item.createDelta();
                    //noinspection unchecked
                    itemDelta.addValuesToAdd((Collection) item.getClonedValues());

                    new ObjectExtensionUpdate(object, itemDelta, ctx, extensionType)
                            .handleItemDelta();
                }
            }
        }
    }

}

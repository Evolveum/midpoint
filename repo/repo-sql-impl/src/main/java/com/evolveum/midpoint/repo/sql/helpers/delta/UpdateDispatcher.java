/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RShadow;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.persistence.metamodel.ManagedType;

/**
 * Dispatches item delta processing to appropriate component (Update class).
 */
class UpdateDispatcher {

    private static final Trace LOGGER = TraceManager.getTrace(UpdateDispatcher.class);

    static <T extends ObjectType> void dispatchModification(PrismObject<T> prismObject, UpdateContext ctx, RObject object,
            ManagedType<T> mainEntityType, ItemDelta<?, ?> delta) throws SchemaException {
        ItemPath path = delta.getPath();

        LOGGER.trace("Processing delta with path '{}'", path);

        ctx.idGenerator.processModification(delta);

        // we'll apply delta to prism object so that we have ItemModifyResult available
        delta.applyTo(prismObject);

        if (isObjectExtensionDelta(path) || isShadowAttributesDelta(path)) {
            if (delta.getPath().size() == 1) {
                ObjectExtensionUpdate.handleWholeContainerDelta(object, delta, ctx);
            } else {
                new ObjectExtensionUpdate(object, delta, ctx)
                        .handleItemDelta();
            }
        } else if (isOperationResult(delta)) {
            new OperationResultUpdate(object, delta, ctx).handleItemDelta();
        } else if (ObjectType.F_METADATA.equivalent(delta.getPath())) {
            new MetadataUpdate(object, object, delta, ctx).handleWholeContainerDelta();
        } else if (FocusType.F_JPEG_PHOTO.equivalent(delta.getPath())) {
            new PhotoUpdate(object, delta, ctx).handlePropertyDelta();
        } else if (ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD,
                PasswordType.F_METADATA).equivalent(delta.getPath())) {
            new PasswordMetadataUpdate(object, delta, ctx).handlePropertyDelta();
        } else {
            if (object instanceof RShadow && ShadowType.F_PENDING_OPERATION.equivalent(delta.getPath())) {
                ctx.shadowPendingOperationModified = true;
            }
            new GeneralUpdate(object, delta, prismObject, mainEntityType, ctx).handleItemDelta();
        }
    }

    private static boolean isOperationResult(ItemDelta delta) throws SchemaException {
        ItemDefinition def = delta.getDefinition();
        if (def == null) {
            throw new SchemaException("No definition in delta for item " + delta.getPath());
        }
        return OperationResultType.COMPLEX_TYPE.equals(def.getTypeName());
    }

    static boolean isObjectExtensionDelta(ItemPath path) {
        return path.startsWithName(ObjectType.F_EXTENSION);
    }

    static boolean isShadowAttributesDelta(ItemPath path) {
        return path.startsWithName(ShadowType.F_ATTRIBUTES);
    }
}

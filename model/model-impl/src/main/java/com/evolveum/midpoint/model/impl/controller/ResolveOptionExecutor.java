/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

import static com.evolveum.midpoint.model.impl.controller.ModelController.RESOLVE_REFERENCE;

/**
 * Executes the "resolve" option at the model level.
 *
 * Uses {@link ModelObjectResolver} and {@link SchemaTransformer} to retrieve the objects and apply schema/security.
 */
class ResolveOptionExecutor {

    @NotNull private final Collection<SelectorOptions<GetOperationOptions>> options;
    @NotNull private final Task task;
    @NotNull private final ModelObjectResolver objectResolver;
    @NotNull private final SchemaTransformer schemaTransformer;

    ResolveOptionExecutor(
            @NotNull Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull ModelObjectResolver objectResolver,
            @NotNull SchemaTransformer schemaTransformer) {
        this.options = options;
        this.task = task;
        this.objectResolver = objectResolver;
        this.schemaTransformer = schemaTransformer;
    }

    public void execute(@NotNull Containerable base, OperationResult result) {
        for (SelectorOptions<GetOperationOptions> option : options) {
            if (GetOperationOptions.isResolve(option.getOptions())) {
                ObjectSelector selector = option.getSelector();
                if (selector != null) {
                    ItemPath path = selector.getPath();
                    ItemPath.checkNoSpecialSymbolsExceptParent(path);
                    resolvePathInContainerable(base, path, option, result);
                }
            }
        }
    }

    private void resolvePathInContainerable(
            @NotNull Containerable base, ItemPath path, SelectorOptions<GetOperationOptions> option, OperationResult result) {
        if (path == null || path.isEmpty()) {
            return;
        }
        PrismContainerValue<?> baseValue = base.asPrismContainerValue();
        Object first = path.first();
        ItemPath rest = path.rest();

        // Special case: we reference the parent (..)
        if (ItemPath.isParent(first)) {
            PrismContainerValue<?> parent = baseValue.getParentContainerValue();
            if (parent != null) {
                resolvePathInContainerable(parent.asContainerable(), rest, option, result);
            }
            return;
        }

        // Neither name nor parent - this should not occur. But let us handle that gracefully.
        if (!ItemPath.isName(first)) {
            return;
        }

        // We have a name: it can be either reference or a container.
        QName firstName = ItemPath.toName(first);
        PrismReference reference = findReference(baseValue, firstName);
        if (reference != null) {
            for (PrismReferenceValue referenceValue : reference.getValues()) {
                if (referenceValue.getObject() == null) {
                    resolveReferenceValue(referenceValue, option, base, result);
                }
                if (!rest.isEmpty()) {
                    PrismObject<?> refObject = referenceValue.getObject();
                    if (refObject != null) {
                        resolvePathInContainerable(refObject.asObjectable(), rest, option, result);
                    } else {
                        // Most probably unresolvable - silently ignore.
                    }
                }
            }
            return;
        }

        // The container case - interesting only if we have to step through it (i.e. the path does not end here).
        if (!rest.isEmpty()) {
            PrismContainer<?> container = baseValue.findContainer(firstName);
            if (container != null) {
                for (PrismContainerValue<?> containerValue : container.getValues()) {
                    resolvePathInContainerable(containerValue.asContainerable(), rest, option, result);
                }
            }
        }
    }

    private static PrismReference findReference(PrismContainerValue<?> baseValue, QName firstName) {
        PrismReference byCompositeName = baseValue.findReferenceByCompositeObjectElementName(firstName); // e.g. "link"
        if (byCompositeName != null) {
            return byCompositeName;
        } else {
            return baseValue.findReference(firstName); // e.g. "linkRef"
        }
    }

    /**
     * Does the actual reference value resolution.
     *
     * TODO are the options passed to "resolve" and "applySchemasAndSecurity" correct?
     */
    private <O extends ObjectType> void resolveReferenceValue(
            @NotNull PrismReferenceValue referenceValue,
            @NotNull SelectorOptions<GetOperationOptions> option,
            @NotNull Containerable context,
            @NotNull OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(RESOLVE_REFERENCE);
        try {
            PrismObject<O> resolved =
                    objectResolver.resolve(referenceValue, context.toString(), option.getOptions(), task, result);
            if (resolved != null) {
                PrismObject<O> cloned = resolved.cloneIfImmutable();
                schemaTransformer.applySchemasAndSecurity(
                        cloned,
                        option.getOptions(),
                        SelectorOptions.createCollection(option.getOptions()),
                        null,
                        task,
                        result);
                referenceValue.setObject(cloned);
            }
        } catch (CommonException e) {
            result.recordWarning(
                    String.format("Couldn't resolve reference to %s: %s",
                            ObjectTypeUtil.toShortString(referenceValue), e.getMessage()),
                    e);
        } finally {
            result.close();
        }
    }
}

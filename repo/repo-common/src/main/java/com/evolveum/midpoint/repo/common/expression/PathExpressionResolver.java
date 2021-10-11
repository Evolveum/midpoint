/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.DefinitionResolver;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.evolveum.midpoint.repo.common.expression.ExpressionUtil.resolveReference;

/**
 * Resolves expression paths (e.g. $immediateRole/jpegPhoto).
 *
 * Factored out from ExpressionUtil.
 */
class PathExpressionResolver {

    private static final Trace LOGGER = TraceManager.getTrace(PathExpressionResolver.class);

    /**
     * Path to be resolved, e.g. "$focus/name" or "assignment".
     */
    @NotNull private final ItemPath path;

    /**
     * Variable name, if present (e.g. focus, immediateRole, ...).
     */
    private final String variableName;

    /**
     * Path without the variable name, e.g. "name", "assignment", ...
     */
    @NotNull private final ItemPath relativePath;

    /**
     * Variables that can be pointed to by the variable name (e.g. focus, immediateRole).
     */
    @NotNull private final ExpressionVariables variables;

    /**
     * Default object to be used if the path does not start with a variable.
     */
    private final TypedValue<?> defaultContext;

    /**
     * Whether to normalize container values that are to be deleted, i.e. convert them from id-only to full data (MID-4863).
     */
    private final boolean normalizeValuesToDelete;

    @NotNull private final ObjectResolver objectResolver;
    @NotNull private final PrismContext prismContext;

    @NotNull private final String shortDesc;
    @NotNull private final Task task;

    PathExpressionResolver(@NotNull ItemPath path, @NotNull ExpressionVariables variables, boolean normalizeValuesToDelete,
            TypedValue<?> defaultContext, @NotNull ObjectResolver objectResolver, @NotNull PrismContext prismContext,
            @NotNull String shortDesc, @NotNull Task task) {
        this.path = path;
        if (path.startsWithVariable()) {
            this.variableName = path.firstToVariableNameOrNull().getLocalPart();
            this.relativePath = path.rest();
        } else {
            this.variableName = null;
            this.relativePath = path;
        }
        this.variables = variables;
        this.normalizeValuesToDelete = normalizeValuesToDelete;
        this.defaultContext = defaultContext;
        this.objectResolver = objectResolver;
        this.prismContext = prismContext;
        this.shortDesc = shortDesc;
        this.task = task;
    }

    /**
     * Main entry point.
     */
    TypedValue<?> resolve(OperationResult result) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        TypedValue<?> root;
        String topVarDesc;
        if (variableName != null) {
            topVarDesc = "variable " + variableName;
            if (variables.containsKey(variableName)) {
                root = variables.get(variableName);
            } else {
                throw new SchemaException("No variable with name " + variableName + " in " + shortDesc);
            }
        } else {
            topVarDesc = "default context";
            root = defaultContext;
        }

        if (root == null) {
            return null;
        }
        if (relativePath.isEmpty()) {
            return root;
        }

        if (normalizeValuesToDelete) {
            root = normalizeValuesToDelete(root);
        }

        if (root.getValue() instanceof ObjectReferenceType) {
            //noinspection unchecked
            root = resolveReference((TypedValue<ObjectReferenceType>) root, objectResolver, topVarDesc, shortDesc, task, result);
        }

        String lastPathSegmentName = relativePath.lastName().getLocalPart();

        Object rootValue = root.getValue();
        if (rootValue == null) {
            return determineNullTypedValue(root);
        } else if (rootValue instanceof Objectable) {
            return determineTypedValue(((Objectable) rootValue).asPrismObject(), false, result);
        } else if (rootValue instanceof PrismObject<?>) {
            return determineTypedValue((PrismObject<?>) rootValue, false, result);
        } else if (rootValue instanceof PrismContainer<?>) {
            return determineTypedValue((PrismContainer<?>) rootValue, false, result);
        } else if (rootValue instanceof PrismContainerValue<?>) {
            return determineTypedValue((PrismContainerValue<?>) rootValue);
        } else if (rootValue instanceof Item<?, ?>) {
            // Except for container (which is handled above)
            throw new SchemaException(
                    "Cannot apply path " + relativePath + " to " + root + " in " + shortDesc);
        } else if (rootValue instanceof ObjectDeltaObject<?>) {
            return determineTypedValueOdo(lastPathSegmentName, root);
        } else if (rootValue instanceof ItemDeltaItem<?, ?>) {
            return determineTypedValue((ItemDeltaItem<?,?>) rootValue);
        } else {
            throw new IllegalArgumentException(
                    "Unexpected root " + rootValue + " (relative path:" + relativePath + ") in " + shortDesc);
        }
    }

    @NotNull
    private TypedValue<?> determineNullTypedValue(TypedValue<?> root) {
        // Even if the result value is going to be null, we still need a definition. Here we try to determine that from root definition.
        if (root.getDefinition() == null) {
            throw new IllegalArgumentException("Root item has no definition for path "+path+". Root="+root);
        }
        ItemDefinition<?> resultDefinition;
        // Relative path is not empty here. Therefore the root must be a container.
        ItemDefinition<?> subItemDefinition = ((PrismContainerDefinition<?>)root.getDefinition()).findItemDefinition(relativePath);
        if (subItemDefinition != null) {
            resultDefinition = subItemDefinition;
        } else {
            // this must be something dynamic, e.g. assignment extension. Just assume string here. Not completely correct. But what can we do?
            resultDefinition = prismContext.definitionFactory()
                    .createPropertyDefinition(relativePath.lastName(), PrimitiveType.STRING.getQname());
        }
        return new TypedValue<>(null, resultDefinition);
    }

    @NotNull
    private TypedValue<?> determineTypedValue(PrismContainer<?> rootContainer, boolean objectAlreadyFetched,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        Object value;
        PartiallyResolvedItem<PrismValue, ItemDefinition<?>> partiallyResolvedItem = rootContainer.findPartial(relativePath);
        if (partiallyResolvedItem == null) {
            value = null;
        } else {
            if (partiallyResolvedItem.getResidualPath() == null) {
                value = partiallyResolvedItem.getItem();
            } else {
                Object parentValue = partiallyResolvedItem.getItem().getRealValue();
                if (parentValue instanceof Structured) {
                    value = ((Structured)parentValue).resolve(partiallyResolvedItem.getResidualPath());
                } else {
                    throw new SchemaException("No subpath "+partiallyResolvedItem.getResidualPath()+" in "+partiallyResolvedItem.getItem());
                }
            }
        }
        if (value instanceof Item && ((Item<?, ?>) value).isIncomplete()) {
            if (objectAlreadyFetched) {
                LOGGER.warn("Referencing incomplete item {} in {} but it is marked as incomplete even if the object was fully fetched", value, rootContainer);
            } else if (!(rootContainer instanceof PrismObject)) {
                LOGGER.warn("Unable to resolve incomplete item {} in {} because the root is not a prism object", value, rootContainer);
            } else {
                PrismObject<?> rootObject = (PrismObject<?>) rootContainer;
                LOGGER.info("Fetching {} because of incomplete item {}", rootObject, value);
                //noinspection unchecked
                Class<? extends ObjectType> type = (Class<? extends ObjectType>) rootObject.asObjectable().getClass();
                // Let's retrieve everything (at least for now). In the future we could ask just for the single item.
                Collection<SelectorOptions<GetOperationOptions>> options =
                        SelectorOptions.createCollection(GetOperationOptions.createRetrieve());
                ObjectType object = objectResolver.getObject(type, rootObject.getOid(), options, task, result);
                return determineTypedValue(object.asPrismObject(), true, result);
            }
        }
        ItemDefinition<?> def = determineItemDefinition(rootContainer.getDefinition(), relativePath);
        if (def == null) {
            throw new IllegalArgumentException("Cannot determine definition for '"+relativePath+"' from "+rootContainer+", value: "+value);
        }
        return new TypedValue<>(value, def);
    }

    private TypedValue<?> determineTypedValue(PrismContainerValue<?> rootContainerValue) {
        Item<PrismValue, ItemDefinition<?>> value = rootContainerValue.findItem(relativePath);
        ItemDefinition<?> def = determineItemDefinition(rootContainerValue.getDefinition(), relativePath);
        if (def == null) {
            throw new IllegalArgumentException("Cannot determine definition for '"+relativePath+"' from "+rootContainerValue+", value: "+value);
        }
        return new TypedValue<>(value, def);
    }

    private TypedValue<?> determineTypedValue(ItemDeltaItem<?, ?> rootIdi) throws SchemaException {
        ItemDeltaItem<PrismValue, ItemDefinition<?>> value = rootIdi.findIdi(relativePath);
        ItemDefinition<?> def = determineItemDefinition((PrismContainerDefinition<?>)rootIdi.getDefinition(), relativePath);
        if (def == null) {
            throw new IllegalArgumentException("Cannot determine definition for '"+relativePath+"' from "+rootIdi+", value: "+value);
        }
        return new TypedValue<>(value, def);
    }

    private <T,O extends ObjectType> TypedValue<T> determineTypedValueOdo(String name, TypedValue<?> root)
            throws SchemaException {
        //noinspection unchecked
        ObjectDeltaObject<O> rootOdo = (ObjectDeltaObject<O>) root.getValue();
        DefinitionResolver<PrismObjectDefinition<O>, ItemDefinition<?>> resolver = (rootDef, path) -> {
            // We are called just before failure. Therefore all normal ways of resolving of definition did not work.
            ItemDefinition<?> parentDef = rootDef.findItemDefinition(path.allExceptLast());
            if (parentDef != null && parentDef.isDynamic()) {
                // This is the case of dynamic schema extensions, such as assignment extension.
                // Those may not have a definition. In that case just assume strings.
                // In fact, this is a HACK. All such schemas should have a definition.
                // Otherwise there may be problems with parameter types for caching compiles scripts and so on.
                return prismContext.definitionFactory().createPropertyDefinition(path.firstName(), PrimitiveType.STRING.getQname());
            }
            return null;
        };
        ItemDeltaItem<PrismValue, ItemDefinition<?>> subValue = rootOdo.findIdi(relativePath, resolver);
        PrismObjectDefinition<O> rootDefinition;
        if (root.getDefinition() == null) {
            rootDefinition = rootOdo.getDefinition();
            if (rootDefinition == null) {
                throw new IllegalArgumentException("Found ODO without a definition while processing variable '"+name+"': "+rootOdo);
            }
        } else {
            rootDefinition = root.getDefinition();
        }
        ItemDefinition<?> itemDefinition = determineItemDefinition(rootDefinition, relativePath);
        if (itemDefinition == null) {
            throw new IllegalArgumentException("Cannot determine definition for '"+relativePath+"' from "+rootOdo+", value: "+subValue);
        }
        return new TypedValue<>(subValue, itemDefinition);
    }

    private ItemDefinition<?> determineItemDefinition(PrismContainerDefinition<?> containerDefinition, ItemPath relativePath) {
        ItemDefinition<?> def = containerDefinition.findItemDefinition(relativePath);
        if (def != null) {
            return def;
        }
        // This may be a wrong path. Or it may be a path to a "sub-property" of a structured property, such as PolyString/norm.
        // Let's find out by looking at the parent.
        ItemPath parentPath = relativePath.allExceptLast();
        ItemDefinition<?> parentDef = containerDefinition.findItemDefinition(parentPath);
        if (parentDef == null) {
            return null;
        }
        if (parentDef instanceof PrismContainerDefinition) {
            if (parentDef.isDynamic() && ((PrismContainerDefinition<?>)parentDef).isEmpty()) {
                // The case of dynamic schema for which there are no definitions
                // E.g. assignment extension just default to single-value strings. Better than nothing. At least for now.
                return parentDef.getPrismContext().definitionFactory().createPropertyDefinition(relativePath.lastName(), PrimitiveType.STRING.getQname());
            }
        } else if (parentDef instanceof PrismPropertyDefinition) {
            if (PrismUtil.isStructuredType(parentDef.getTypeName())) {
                // All "sub-properties" are hardcoded as single value strings
                return parentDef.getPrismContext().definitionFactory().createPropertyDefinition(relativePath.lastName(), PrimitiveType.STRING.getQname());
            }
        }
        return null;
    }

    private TypedValue<?> normalizeValuesToDelete(TypedValue<?> root) {
        Object rootValue = root.getValue();
        if (rootValue instanceof ObjectDeltaObject<?>) {
            return new TypedValue<>(((ObjectDeltaObject<?>) rootValue).normalizeValuesToDelete(true),
                    (ItemDefinition<?>) root.getDefinition());
        } else if (rootValue instanceof ItemDeltaItem<?, ?>) {
            // TODO normalize as well
            return root;
        } else {
            return root;
        }
    }
}

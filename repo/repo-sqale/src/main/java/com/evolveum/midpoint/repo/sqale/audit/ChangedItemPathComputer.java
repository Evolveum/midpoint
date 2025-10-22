/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.audit;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ChangedItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Utility class for computing changed item paths from object delta operations.
 *
 * @author Viliam Repan (lazyman)
 */
public class ChangedItemPathComputer {

    private final boolean indexAddObjectDeltaOperation;

    @NotNull private final Set<ChangedItemPath> indexAdditionalItemPath;

    @NotNull private final PrismContext prismContext;

    public ChangedItemPathComputer(
            boolean indexAddObjectDeltaOperation,
            @NotNull Set<ChangedItemPath> indexAdditionalItemPath,
            @NotNull PrismContext prismContext) {

        this.indexAddObjectDeltaOperation = indexAddObjectDeltaOperation;
        this.indexAdditionalItemPath = indexAdditionalItemPath;
        this.prismContext = prismContext;
    }

    private QName objectTypeQName(ObjectDeltaOperation<? extends ObjectType> delta) {
        return ObjectTypes.getObjectType(delta.getObjectDelta().getObjectTypeClass()).getTypeQName();
    }

    public Set<String> collectChangedItemPaths(
            Collection<ObjectDeltaOperation<? extends ObjectType>> deltas) {

        Set<String> changedItemPaths = new HashSet<>();
        for (var delta : deltas) {
            var objectType = objectTypeQName(delta);

            if (indexAddObjectDeltaOperation && delta.getObjectDelta().getObjectToAdd() != null) {
                PrismObject<?> object = delta.getObjectDelta().getObjectToAdd();

                collectAddDeltaOperationPaths(object, changedItemPaths);
            }

            for (var itemDelta : delta.getObjectDelta().getModifications()) {
                if (itemDelta.isEmpty()) {
                    // Skipping empty deltas (was normalized during serialization)
                    continue;
                }
                ItemPath path = itemDelta.getPath();

                collectDefaultItemDeltaPaths(path, objectType, changedItemPaths);

                collectAdditionalItemDeltaPaths(itemDelta, objectType, changedItemPaths);
            }
        }
        return changedItemPaths;
    }

    /**
     * Collect changed item paths from XML-based delta operations.
     *
     * TODO: is this OK? It's used in raw {@link AuditService#audit(AuditEventRecordType, OperationResult)} which doesn't
     * do any processing on record.
     *
     * Parses {@link ItemDeltaType}s, if parsing fails it's currently silently ignored.
     * No additional paths collected for such deltas.
     */
    public String[] collectChangedItemPaths(List<ObjectDeltaOperationType> deltaOperations) {
        Set<String> changedItemPaths = new HashSet<>();
        for (ObjectDeltaOperationType deltaOperation : deltaOperations) {
            ObjectDeltaType delta = deltaOperation.getObjectDelta();

            if (indexAddObjectDeltaOperation && delta.getObjectToAdd() != null) {
                PrismObject<?> object = delta.getObjectToAdd().asPrismObject();

                collectAddDeltaOperationPaths(object, changedItemPaths);
            }

            for (ItemDeltaType itemDelta : delta.getItemDelta()) {
                ItemPath path = itemDelta.getPath().getItemPath();

                collectDefaultItemDeltaPaths(path, delta.getObjectType(), changedItemPaths);

                collectAdditionalItemDeltaTypePaths(itemDelta, delta.getObjectType(), changedItemPaths);
            }
        }

        return changedItemPaths.isEmpty() ? null : changedItemPaths.toArray(String[]::new);
    }

    private void collectAddDeltaOperationPaths(PrismObject<?> object, Set<String> changedItemPaths) {
        collectDefaultAddDeltaOperationPaths(object, changedItemPaths);

        collectAdditionalAddDeltaPaths(object, changedItemPaths);
    }

    private void collectDefaultAddDeltaOperationPaths(PrismObject<?> object, Set<String> changedItemPaths) {
        QName type = object.getDefinition().getTypeName();
        object.getValue().getItems().stream()
                .map(Item::getElementName)
                .map(i -> createCanonicalItemPath(i, type))
                .forEach(changedItemPaths::add);
    }

    private void collectAdditionalAddDeltaPaths(PrismObject<?> object, Set<String> changedItemPaths) {
        for (ChangedItemPath additionalPath : indexAdditionalItemPath) {
            ItemPath path = additionalPath.path();

            ItemPathVisitor visitor = new ItemPathVisitor(path);
            object.acceptVisitor(visitor);

            collectAdditionalPathsFromFoundItems(
                    additionalPath, object.getDefinition().getTypeName(), visitor.result, changedItemPaths);
        }
    }

    private void collectAdditionalPathsFromFoundItems(
            ChangedItemPath additionalPath, QName type, List<Item<?, ?>> result, Set<String> changedItemPaths) {

        if (result.isEmpty()) {
            return;
        }

        ItemPath path = additionalPath.path();

        changedItemPaths.add(createCanonicalItemPath(path, type));

        if (!additionalPath.all()) {
            return;
        }

        for (Item<?, ?> item : result) {
            if (!(item instanceof PrismContainer<?> pc)) {
                continue;
            }

            for (PrismContainerValue<?> pcv : pc.getValues()) {
                for (Item<?, ?> subItem : pcv.getItems()) {
                    ItemPath subItemPath = path.append(subItem.getElementName());

                    changedItemPaths.add(createCanonicalItemPath(subItemPath, type));
                }
            }
        }
    }

    private String createCanonicalItemPath(ItemPath path, QName objectType) {
        CanonicalItemPath canonical = prismContext.createCanonicalItemPath(path, objectType);
        return canonical.asString();
    }

    private void collectDefaultItemDeltaPaths(ItemPath path, QName objectType, Set<String> changedItemPaths) {
        CanonicalItemPath canonical = prismContext.createCanonicalItemPath(path, objectType);
        for (int i = 0; i < canonical.size(); i++) {
            changedItemPaths.add(canonical.allUpToIncluding(i).asString());
        }
    }

    private void collectAdditionalItemDeltaPaths(ItemDelta<?, ?> itemDelta, QName objectType, Set<String> changedItemPaths) {
        for (ChangedItemPath additionalPath : indexAdditionalItemPath) {
            ItemPath path = additionalPath.path();

            ItemPath deltaPath = itemDelta.getPath().namedSegmentsOnly();

            if (!path.isSubPathOrEquivalent(deltaPath)) {
                continue;
            }

            ItemPath remainderPath = path.remainder(deltaPath);
            ItemPathVisitor visitor = new ItemPathVisitor(remainderPath);

            collectAdditionalItemDeltaValuePaths(itemDelta, objectType, additionalPath, visitor, changedItemPaths);
        }
    }

    private void collectAdditionalItemDeltaValuePaths(
            ItemDelta<?, ?> itemDelta,
            QName objectType,
            ChangedItemPath additionalPath,
            ItemPathVisitor visitor,
            Set<String> changedItemPaths) {

        for (ModificationType modificationType : ModificationType.values()) {
            Collection<?> values = itemDelta.getValues(modificationType);
            if (values == null) {
                continue;
            }

            for (Object value : values) {
                if (!(value instanceof PrismValue v)) {
                    continue;
                }

                v.acceptVisitor(visitor);
            }
        }

        collectAdditionalPathsFromFoundItems(
                additionalPath, objectType, visitor.result, changedItemPaths);
    }

    private void collectAdditionalItemDeltaTypePaths(ItemDeltaType itemDelta, QName objectType, Set<String> changedItemPaths) {
        for (ChangedItemPath additionalPath : indexAdditionalItemPath) {
            ItemPath path = additionalPath.path();

            ItemPath deltaPath = itemDelta.getPath().getItemPath().namedSegmentsOnly();

            if (!path.isSubPathOrEquivalent(deltaPath)) {
                continue;
            }

            ItemPath remainderPath = path.remainder(deltaPath);
            ItemPathVisitor visitor = new ItemPathVisitor(remainderPath);

            try {
                ItemDelta<?, ?> delta = DeltaConvertor.createItemDelta(itemDelta, ObjectTypes.getObjectTypeClass(objectType));

                collectAdditionalItemDeltaValuePaths(delta, objectType, additionalPath, visitor, changedItemPaths);
            } catch (SchemaException ex) {

            }
        }
    }

    /**
     * Public visibility only for testing purposes.
     */
    public static class ItemPathVisitor implements PrismVisitor {

        private final ItemPath path;

        private final List<Item<?, ?>> result;

        public ItemPathVisitor(ItemPath path) {
            this(path, new ArrayList<>());
        }

        public ItemPathVisitor(ItemPath path, List<Item<?, ?>> result) {
            this.path = path;
            this.result = result;
        }

        public List<Item<?, ?>> getResult() {
            return result;
        }

        @Override
        public boolean visit(PrismVisitable visitable) {
            if (path.isEmpty() && visitable instanceof Item<?, ?> i) {
                // we've found item
                result.add(i);
                return false;
            }

            if (visitable instanceof PrismProperty<?> || visitable instanceof PrismReference) {
                // don't go deeper if it's property or reference
                return false;
            }

            if (!(visitable instanceof PrismContainer<?> pc)) {
                return true;
            }

            for (PrismContainerValue<?> pcv : pc.getValues()) {
                ItemName first = path.firstName();
                ItemPath rest = path.rest();

                Item<?, ?> item = pcv.findItem(first);
                if (item == null) {
                    return false;
                }

                item.acceptVisitor(new ItemPathVisitor(rest, result));
            }

            return false;
        }
    }
}

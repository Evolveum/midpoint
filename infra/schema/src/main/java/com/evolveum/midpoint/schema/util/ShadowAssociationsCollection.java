/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.prism.ModificationType.*;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents iterable collection of association values either in an item delta, or in a shadow.
 * For delta, it can concern related either individual associations (i.e., items), or the associations container as a whole.
 *
 * LIMITATIONS: at the level of the whole associations container, only ADD deltas are supported. No REPLACE nor DELETE.
 *
 * @see ShadowReferenceAttributesCollection
 */
public abstract class ShadowAssociationsCollection implements DebugDumpable {

    public static @NotNull ShadowAssociationsCollection empty() {
        return Empty.INSTANCE;
    }

    public static ShadowAssociationsCollection ofDelta(@NotNull ItemDelta<?, ?> itemDelta) throws SchemaException {
        if (!(itemDelta instanceof ContainerDelta<?> containerDelta)) {
            return empty();
        }
        if (ShadowType.F_ASSOCIATIONS.equivalent(containerDelta.getParentPath())) {
            //noinspection unchecked
            return new ItemDeltaBased((ContainerDelta<ShadowAssociationValueType>) containerDelta);
        } else if (ShadowType.F_ASSOCIATIONS.equivalent(containerDelta.getPath())) {
            return AssociationsContainerValueBased.fromDelta(containerDelta);
        } else {
            return empty();
        }
    }

    public static ShadowAssociationsCollection ofShadow(@NotNull ShadowType shadow) {
        PrismContainer<ShadowAssociationsType> container = shadow.asPrismObject().findContainer(ShadowType.F_ASSOCIATIONS);
        if (container != null && container.hasAnyValue()) {
            return AssociationsContainerValueBased.fromValues(container.getValues());
        } else {
            return empty();
        }
    }

    @SuppressWarnings("unused") // maybe in the future
    public static ShadowAssociationsCollection ofAssociations(@NotNull ShadowAssociationsType associations) {
        //noinspection unchecked
        return AssociationsContainerValueBased.fromValues(
                List.of((PrismContainerValue<ShadowAssociationsType>) associations.asPrismContainerValue()));
    }

    public abstract Iterator<IterableAssociationValue> iterator();

    public boolean isEmpty() {
        return !iterator().hasNext(); // TODO we can optimize this
    }

    public List<IterableAssociationValue> getAllIterableValues() {
        List<IterableAssociationValue> allValues = new ArrayList<>();
        var iterator = iterator();
        while (iterator.hasNext()) {
            allValues.add(iterator.next());
        }
        return allValues;
    }

    @SuppressWarnings("unused") // maybe in the future
    public List<ShadowAssociationValue> getAllAssociationValues() {
        List<ShadowAssociationValue> allValues = new ArrayList<>();
        var iterator = iterator();
        while (iterator.hasNext()) {
            allValues.add(iterator.next().associationValue());
        }
        return allValues;
    }

    public int getNumberOfValues() {
        var iterator = iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    public abstract boolean hasReplace();

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder("Association delta(s)", indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "content", getDebugContent(), indent + 1);
        return sb.toString();
    }

    abstract DebugDumpable getDebugContent();

    // TODO
    public abstract void cleanup();

    /**
     * Represents a {@link ShadowAssociationValue} present either in an {@link ItemDelta} or in a {@link ShadowType},
     * accessed via {@link #iterator()} method.
     *
     * @param name Name of the containing association
     * @param value The value bean (refined, i.e. not a raw bean)
     * @param modificationType type of the modification, if the value is present in {@link ItemDelta} (although sometimes only
     * add/delete mods are expected, see {@link #isAddNotDelete()}. For static data the {@link ModificationType#ADD} is used.
     */
    public record IterableAssociationValue(
            @NotNull ItemName name, @NotNull ShadowAssociationValueType value, @NotNull ModificationType modificationType)
            implements Serializable {

        @SuppressWarnings("unused") // maybe in the future
        public @NotNull PrismContainerValue<ShadowAssociationValueType> associationPcv() {
            //noinspection unchecked
            return value.asPrismContainerValue();
        }

        public @NotNull ShadowAssociationValue associationValue() {
            return (ShadowAssociationValue) value.asPrismContainerValue();
        }

        public boolean isAdd() {
            return modificationType == ADD;
        }

        public boolean isDelete() {
            return modificationType == DELETE;
        }

        public boolean isAddNotDelete() {
            if (isAdd()) {
                return true;
            } else if (isDelete()) {
                return false;
            } else {
                throw new AssertionError("No REPLACE delta is expected here");
            }
        }
    }

    private static class Empty extends ShadowAssociationsCollection {

        private static final Empty INSTANCE = new Empty();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public List<IterableAssociationValue> getAllIterableValues() {
            return List.of();
        }

        @Override
        public int getNumberOfValues() {
            return 0;
        }

        @Override
        public Iterator<IterableAssociationValue> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean hasReplace() {
            return false;
        }

        @Override
        DebugDumpable getDebugContent() {
            return null;
        }

        @Override
        public void cleanup() {
            // Nothing to do
        }
    }

    private static class ItemDeltaBased extends ShadowAssociationsCollection {

        @NotNull private final ContainerDelta<ShadowAssociationValueType> delta;
        @NotNull private final ItemName associationName;

        private class MyIterator implements Iterator<IterableAssociationValue> {
            @NotNull private final Iterator<ModificationType> setIterator;
            private ModificationType currentSet;
            private Iterator<PrismContainerValue<ShadowAssociationValueType>> valueIterator;

            private MyIterator() {
                this.setIterator = List.of(ADD, DELETE, REPLACE).iterator();
            }

            @Override
            public boolean hasNext() {
                for (;;) {
                    if (valueIterator != null && valueIterator.hasNext()) {
                        return true;
                    }
                    if (!setIterator.hasNext()) {
                        return false;
                    }
                    currentSet = setIterator.next();
                    valueIterator = emptyIfNull(delta.getValues(currentSet)).iterator();
                }
            }

            @Override
            public IterableAssociationValue next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                assert currentSet != null;
                assert valueIterator != null;
                return new IterableAssociationValue(associationName, valueIterator.next().asContainerable(), currentSet);
            }

            @Override
            public void remove() {
                assert valueIterator != null;
                valueIterator.remove();
            }
        }

        ItemDeltaBased(@NotNull ContainerDelta<ShadowAssociationValueType> delta) {
            this.delta = delta;
            this.associationName = delta.getElementName();
        }

        @Override
        public Iterator<IterableAssociationValue> iterator() {
            return new MyIterator();
        }

        @Override
        public boolean hasReplace() {
            return delta.isReplace();
        }

        @Override
        DebugDumpable getDebugContent() {
            return delta;
        }

        @Override
        public void cleanup() {
            // basically, nothing to do here
        }
    }

    private static class AssociationsContainerValueBased extends ShadowAssociationsCollection {

        /** The `associations` container value. Its content is checked when iterating. */
        @NotNull private final PrismContainerValue<?> associations;

        private AssociationsContainerValueBased(@NotNull PrismContainerValue<?> associations) {
            this.associations = associations;
        }

        private static AssociationsContainerValueBased fromDelta(ContainerDelta<?> containerDelta) throws SchemaException {
            schemaCheck(!containerDelta.isReplace(),
                    "REPLACE associations delta is not supported: %s", containerDelta);
            schemaCheck(!containerDelta.isDelete(),
                    "DELETE associations delta is not supported: %s", containerDelta);
            return fromValues(containerDelta.getValuesToAdd());
        }

        private static AssociationsContainerValueBased fromValues(Collection<? extends PrismContainerValue<?>> containerValues) {
            PrismContainerValue<?> pcv = MiscUtil.extractSingleton(
                    containerValues,
                    () -> new IllegalStateException("Multiple associations containers to add? %s" + containerValues));
            return new AssociationsContainerValueBased(
                    Objects.requireNonNullElseGet(pcv, PrismContainerValueImpl::new));
        }

        private class MyIterator implements Iterator<IterableAssociationValue> {

            @NotNull private final Iterator<Item<?, ?>> itemIterator;
            private Item<?, ?> currentItem;
            private Iterator<? extends PrismContainerValue<?>> valueIterator;

            private MyIterator() {
                this.itemIterator = associations.getItems().iterator();
            }

            @Override
            public boolean hasNext() {
                for (;;) {
                    if (valueIterator != null && valueIterator.hasNext()) {
                        return true;
                    }
                    if (!itemIterator.hasNext()) {
                        return false;
                    }
                    currentItem = itemIterator.next();
                    if (currentItem instanceof PrismContainer<?> container) {
                        valueIterator = container.getValues().iterator();
                    } else {
                        throw new IllegalStateException("Not a container in the associations delta: " + currentItem);
                    }
                }
            }

            @Override
            public IterableAssociationValue next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                assert valueIterator != null;
                assert currentItem != null;
                PrismContainerValue<?> associationPcv = valueIterator.next();
                try {
                    return new IterableAssociationValue(
                            currentItem.getElementName(),
                            MiscUtil.castSafely(associationPcv.asContainerable(), ShadowAssociationValueType.class),
                            ADD);
                } catch (SchemaException e) {
                    throw new IllegalStateException("Not an association value: " + associationPcv, e);
                }
            }

            @Override
            public void remove() {
                assert valueIterator != null;
                valueIterator.remove();
            }
        }

        @Override
        public Iterator<IterableAssociationValue> iterator() {
            return new MyIterator();
        }

        @Override
        public boolean hasReplace() {
            return false;
        }

        @Override
        public void cleanup() {
            for (Item<?, ?> item : List.copyOf(associations.getItems())) {
                if (item.hasNoValues()) {
                    associations.remove(item);
                }
            }
        }

        @Override
        DebugDumpable getDebugContent() {
            return associations;
        }
    }
}

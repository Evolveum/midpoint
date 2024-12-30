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

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SystemException;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttribute;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents iterable collection of attribute values either in an item delta, or in a shadow.
 * For delta, it can concern related either individual attributes (i.e., items), or the attributes container as a whole.
 *
 * LIMITATIONS: at the level of the whole attributes container, only ADD deltas are supported. No REPLACE nor DELETE.
 *
 * @see ShadowAssociationsCollection
 */
public abstract class ShadowReferenceAttributesCollection implements DebugDumpable {

    public static @NotNull ShadowReferenceAttributesCollection empty() {
        return Empty.INSTANCE;
    }

    public static ShadowReferenceAttributesCollection ofDelta(@NotNull ItemDelta<?, ?> itemDelta) {
        if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath())
                && itemDelta instanceof ReferenceDelta referenceDelta) {
            return new ItemDeltaBased(referenceDelta);
        } else if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getPath())) {
            return AttributesContainerValueBased.fromDelta((ContainerDelta<?>) itemDelta);
        } else {
            return empty();
        }
    }

    public static ShadowReferenceAttributesCollection ofObjectDelta(@Nullable ObjectDelta<ShadowType> objectDelta) {
        if (objectDelta == null || objectDelta.isDelete()) {
            return empty();
        } else if (objectDelta.isAdd()) {
            return ofShadow(objectDelta.getObjectableToAdd());
        } else {
            assert objectDelta.isModify();
            return new ModifyDeltaBased(objectDelta);
        }
    }

    /** Returns all reference attributes of given shadow. */
    public static ShadowReferenceAttributesCollection ofShadow(@NotNull ShadowType shadow) {
        return ofAttributesContainer(
                shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES));
    }

    /** Returns all reference attributes of given association value - i.e. everything in "objects" container. */
    public static ShadowReferenceAttributesCollection ofAssociationValue(@NotNull ShadowAssociationValue assocValue) {
        return ofAttributesContainer(assocValue.getObjectsContainer());
    }

    private static ShadowReferenceAttributesCollection ofAttributesContainer(
            @Nullable PrismContainer<? extends ShadowAttributesType> container) {
        if (container != null && container.hasAnyValue()) {
            return AttributesContainerValueBased.fromValues(container.getValues());
        } else {
            return empty();
        }
    }

    public abstract Iterator<IterableReferenceAttributeValue> iterator();

    public Iterator<ShadowReferenceAttributeValue> valuesIterator() {
        return new Iterator<>() {

            private final Iterator<IterableReferenceAttributeValue> iterator =
                    ShadowReferenceAttributesCollection.this.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ShadowReferenceAttributeValue next() {
                return iterator.next().value();
            }
        };
    }

    public Iterable<ShadowReferenceAttributeValue> valuesIterable() {
        return new Iterable<>() {
            @Override
            public @NotNull Iterator<ShadowReferenceAttributeValue> iterator() {
                return valuesIterator();
            }
        };
    }

    public boolean isEmpty() {
        return !iterator().hasNext(); // TODO we can optimize this
    }

    public List<IterableReferenceAttributeValue> getAllIterableValues() {
        List<IterableReferenceAttributeValue> allValues = new ArrayList<>();
        var iterator = iterator();
        while (iterator.hasNext()) {
            allValues.add(iterator.next());
        }
        return allValues;
    }

    public List<ShadowReferenceAttributeValue> getAllReferenceValues() {
        List<ShadowReferenceAttributeValue> allValues = new ArrayList<>();
        var iterator = iterator();
        while (iterator.hasNext()) {
            allValues.add(iterator.next().value());
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

    public record IterableReferenceAttributeValue(
            @NotNull ItemName name, @NotNull ShadowReferenceAttributeValue value, @NotNull ModificationType modificationType)
            implements Serializable {

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

    private static class Empty extends ShadowReferenceAttributesCollection {

        private static final Empty INSTANCE = new Empty();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public List<IterableReferenceAttributeValue> getAllIterableValues() {
            return List.of();
        }

        @Override
        public int getNumberOfValues() {
            return 0;
        }

        @Override
        public Iterator<IterableReferenceAttributeValue> iterator() {
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

    private static class ModifyDeltaBased extends ShadowReferenceAttributesCollection {

        @NotNull private final ObjectDelta<ShadowType> shadowModifyDelta;

        private ModifyDeltaBased(@NotNull ObjectDelta<ShadowType> shadowModifyDelta) {
            Preconditions.checkArgument(shadowModifyDelta.isModify());
            this.shadowModifyDelta = shadowModifyDelta;
        }

        @Override
        public Iterator<IterableReferenceAttributeValue> iterator() {
            return new ItemDeltaListBasedIterator(shadowModifyDelta.getModifications().iterator());
        }

        @Override
        public boolean hasReplace() {
            return false;
        }

        @Override
        DebugDumpable getDebugContent() {
            return shadowModifyDelta;
        }

        @Override
        public void cleanup() {
            // Nothing to do (even if we remove all item deltas, the empty shadow modify delta is still valid)
        }

        private static class ItemDeltaListBasedIterator implements Iterator<IterableReferenceAttributeValue> {

            @NotNull private final Iterator<? extends ItemDelta<?, ?>> itemDeltaIterator;
            private Iterator<IterableReferenceAttributeValue> valueIterator;

            ItemDeltaListBasedIterator(@NotNull Iterator<? extends ItemDelta<?, ?>> iterator) {
                this.itemDeltaIterator = iterator;
            }

            @Override
            public boolean hasNext() {
                for (;;) {
                    if (valueIterator != null && valueIterator.hasNext()) {
                        return true;
                    }
                    if (!itemDeltaIterator.hasNext()) {
                        return false;
                    }
                    valueIterator = ShadowReferenceAttributesCollection
                            .ofDelta(itemDeltaIterator.next())
                            .iterator();
                }
            }

            @Override
            public IterableReferenceAttributeValue next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                assert valueIterator != null && valueIterator.hasNext();
                return valueIterator.next();
            }

            @Override
            public void remove() {
                assert valueIterator != null;
                valueIterator.remove();
            }
        }
    }

    private static class ItemDeltaBased extends ShadowReferenceAttributesCollection {

        @NotNull private final ReferenceDelta delta;
        @NotNull private final ItemName attributeName;

        private class ItemDeltaBasedIterator implements Iterator<IterableReferenceAttributeValue> {
            @NotNull private final Iterator<ModificationType> setIterator;
            private ModificationType currentSet;
            private Iterator<PrismReferenceValue> valueIterator;

            private ItemDeltaBasedIterator() {
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
            public IterableReferenceAttributeValue next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                assert currentSet != null;
                assert valueIterator != null;
                try {
                    return new IterableReferenceAttributeValue(
                            attributeName,
                            ShadowReferenceAttributeValue.fromRefValue(valueIterator.next()),
                            currentSet);
                } catch (SchemaException e) {
                    throw new SystemException(e); // FIXME
                }
            }

            @Override
            public void remove() {
                assert valueIterator != null;
                valueIterator.remove();
            }
        }

        ItemDeltaBased(@NotNull ReferenceDelta delta) {
            this.delta = delta;
            this.attributeName = delta.getElementName();
        }

        @Override
        public Iterator<IterableReferenceAttributeValue> iterator() {
            return new ItemDeltaBasedIterator();
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

    private static class AttributesContainerValueBased extends ShadowReferenceAttributesCollection {

        /** The `attributes` container value. Its content is checked when iterating. */
        @NotNull private final PrismContainerValue<?> attributes;

        private AttributesContainerValueBased(@NotNull PrismContainerValue<?> attributes) {
            this.attributes = attributes;
        }

        private static AttributesContainerValueBased fromDelta(ContainerDelta<?> containerDelta) {
            stateCheck(containerDelta.getValuesToReplace() == null,
                    "REPLACE attributes delta is not supported: %s", containerDelta);
            stateCheck(containerDelta.isDelete(),
                    "DELETE attributes delta is not supported: %s", containerDelta);
            return fromValues(containerDelta.getValuesToAdd());
        }

        private static AttributesContainerValueBased fromValues(Collection<? extends PrismContainerValue<?>> containerValues) {
            PrismContainerValue<?> pcv = MiscUtil.extractSingleton(
                    containerValues,
                    () -> new IllegalStateException("Multiple attributes containers to add? %s" + containerValues));
            return new AttributesContainerValueBased(
                    Objects.requireNonNullElseGet(pcv, PrismContainerValueImpl::new));
        }

        private class MyIterator implements Iterator<IterableReferenceAttributeValue> {

            @NotNull private final Iterator<ShadowReferenceAttribute> refAttrIterator;
            private ShadowReferenceAttribute currentRefAttr;
            private Iterator<ShadowReferenceAttributeValue> valueIterator;

            private MyIterator() {
                this.refAttrIterator = attributes.getItems().stream()
                        .filter(item -> item instanceof ShadowReferenceAttribute)
                        .map(item -> (ShadowReferenceAttribute) item)
                        .iterator();
            }

            @Override
            public boolean hasNext() {
                for (;;) {
                    if (valueIterator != null && valueIterator.hasNext()) {
                        return true;
                    }
                    if (!refAttrIterator.hasNext()) {
                        return false;
                    }
                    currentRefAttr = refAttrIterator.next();
                    valueIterator = currentRefAttr.getAttributeValues().iterator();
                }
            }

            @Override
            public IterableReferenceAttributeValue next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                assert currentRefAttr != null;
                assert valueIterator != null;
                return new IterableReferenceAttributeValue(
                        currentRefAttr.getElementName(),
                        valueIterator.next(),
                        ADD);
            }

            @Override
            public void remove() {
                assert valueIterator != null;
                valueIterator.remove();
            }
        }

        @Override
        public Iterator<IterableReferenceAttributeValue> iterator() {
            return new MyIterator();
        }

        @Override
        public boolean hasReplace() {
            return false;
        }

        @Override
        public void cleanup() {
            for (Item<?, ?> item : List.copyOf(attributes.getItems())) {
                if (item.hasNoValues()) {
                    attributes.remove(item);
                }
            }
        }

        @Override
        DebugDumpable getDebugContent() {
            return attributes;
        }
    }
}

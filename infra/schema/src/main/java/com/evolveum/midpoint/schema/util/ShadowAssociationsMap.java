/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ShadowReferenceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.NameKeyedMap;
import com.evolveum.midpoint.schema.processor.ShadowAssociationsContainer;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.TestOnly;

/**
 * Provides a nicer API for working with the shadow associations. Currently, it provides a read-only access:
 * the content of the `associations` container is parsed at the object construction time.
 *
 * Not thread-safe.
 *
 * NOTE: Use only when the shadow may be "raw". Otherwise, use the {@link ShadowAssociationsContainer} instead.
 *
 * TODO Decide on the fate of this class. Maybe it's not that useful as it originally seemed to be.
 *
 * @see ShadowAttributesContainer
 */
@TestOnly
@Experimental
public class ShadowAssociationsMap implements Map<QName, ShadowAssociationsMap.RawAssociation>, Serializable {

    @NotNull private final NameKeyedMap<QName, RawAssociation> map;

    /** We assume that the bean is well-formed. */
    private ShadowAssociationsMap(@Nullable ShadowAssociationsType associationsBean) {
        this.map = parse(associationsBean);
    }

    public static @NotNull ShadowAssociationsMap of(@Nullable ShadowAssociationsType associationsBean) {
        return new ShadowAssociationsMap(associationsBean);
    }

    public static @NotNull ShadowAssociationsMap of(@NotNull ShadowType shadow) {
        return of(shadow.getAssociations());
    }

    public static @NotNull ShadowAssociationsMap of(@NotNull PrismObject<ShadowType> shadow) {
        return of(shadow.asObjectable());
    }

    private NameKeyedMap<QName, RawAssociation> parse(ShadowAssociationsType bean) {
        if (bean != null) {
            return RawAssociation.collectionOf(bean).stream()
                    .collect(Collectors.toMap(
                            shadowAssociation -> shadowAssociation.name,
                            association -> association,
                            (assoc1, assoc2) -> {
                                throw new IllegalStateException("Duplicate association: " + assoc1);
                            },
                            NameKeyedMap::new));
        } else {
            return new NameKeyedMap<>();
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @NotNull
    public Collection<ShadowAssociationValueType> getValues(@Nullable QName key) {
        var association = map.get(key);
        return association != null ? association.getValues() : List.of();
    }

    @Override
    public RawAssociation get(@Nullable Object key) {
        return map.get(key);
    }

    @Nullable
    @Override
    public RawAssociation put(QName key, RawAssociation value) {
        throw immutableObjectException();
    }

    @NotNull
    private static UnsupportedOperationException immutableObjectException() {
        return new UnsupportedOperationException("This object is immutable");
    }

    @Override
    public RawAssociation remove(Object key) {
        throw immutableObjectException();
    }

    @Override
    public void putAll(@NotNull Map<? extends QName, ? extends RawAssociation> m) {
        throw immutableObjectException();
    }

    @Override
    public void clear() {
        throw immutableObjectException();
    }

    @NotNull
    @Override
    public Set<QName> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<RawAssociation> values() {
        return map.values();
    }

    @NotNull
    @Override
    public Set<Entry<QName, RawAssociation>> entrySet() {
        return map.entrySet();
    }

    /** TODO better name. */
    public int valuesCount() {
        return map.values().stream()
                .mapToInt(v -> v.size())
                .sum();
    }

    /** As {@link ShadowReferenceAttribute} but potentially raw, i.e. without definitions. */
    public static class RawAssociation {
        @NotNull private final QName name;
        @NotNull private final Collection<ShadowAssociationValueType> values;

        private RawAssociation(@NotNull QName name, @NotNull Collection<ShadowAssociationValueType> values) {
            this.name = name;
            this.values = values;
        }

        public @NotNull QName getName() {
            return name;
        }

        public @NotNull Collection<ShadowAssociationValueType> getValues() {
            return values;
        }

        public int size() {
            return values.size();
        }

        /**
         * The returned values are "live", connected to the original values in the associations bean.
         * However, the returned _collection_ is detached from the bean. That is, the collection itself is immutable.
         *
         * The bean must be well-formed.
         */
        public static Collection<RawAssociation> collectionOf(@Nullable ShadowAssociationsType associationsBean) {
            if (associationsBean != null) {
                //noinspection unchecked
                return ((PrismContainerValue<ShadowAssociationsType>) associationsBean.asPrismContainerValue()).getItems().stream()
                        .map(item -> of(item))
                        .toList();
            } else {
                return List.of();
            }
        }

        public static RawAssociation of(@NotNull Item<?, ?> item) {
            return new RawAssociation(item.getElementName(), item.getRealValues(ShadowAssociationValueType.class));
        }
    }
}

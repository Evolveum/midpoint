/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.key;

import static java.util.Map.entry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class NaturalKeyImpl implements NaturalKey {

    private static final Function<QName, NaturalKey> DEFAULT_CONSTITUENT_HANDLER = (constituent) -> DefaultNaturalKeyImpl.of(constituent);

    private static final @NotNull Map<Class<?>, Function<QName, NaturalKey>> CONSTITUENT_HANDLERS = Map.ofEntries(
            entry(ItemPathType.class, (constituent) -> ItemPathNaturalKeyImpl.of(new ItemName(constituent)))
    );

    @NotNull private final Collection<QName> constituents;

    private NaturalKeyImpl(@NotNull Collection<QName> constituents) {
        this.constituents = constituents;
    }

    public static NaturalKeyImpl of(QName... constituents) {
        return new NaturalKeyImpl(List.of(constituents));
    }

    @Override
    public boolean valuesMatch(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue)
            throws ConfigurationException {

        PrismContainerDefinition<?> targetDef = targetValue.getDefinition();

        for (QName constituent : constituents) {
            ItemDefinition<?> itemDefinition = targetDef.findItemDefinition(ItemName.fromQName(constituent));
            Class<?> itemType = itemDefinition.getTypeClass();

            NaturalKey handler = CONSTITUENT_HANDLERS
                    .getOrDefault(itemType, DEFAULT_CONSTITUENT_HANDLER)
                    .apply(constituent);

            if (!handler.valuesMatch(targetValue, sourceValue)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void mergeMatchingKeys(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue)
            throws ConfigurationException {

        PrismContainerDefinition<?> targetDef = targetValue.getDefinition();

        for (QName constituent : constituents) {
            ItemDefinition<?> itemDefinition = targetDef.findItemDefinition(ItemName.fromQName(constituent));
            Class<?> itemType = itemDefinition.getTypeClass();

            NaturalKey handler = CONSTITUENT_HANDLERS
                    .getOrDefault(itemType, DEFAULT_CONSTITUENT_HANDLER)
                    .apply(constituent);

            handler.mergeMatchingKeys(targetValue, sourceValue);
        }
    }
}

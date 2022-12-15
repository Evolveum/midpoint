/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.key;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Natural key consisting of an item path.
 *
 * Assumptions:
 *
 * 1. the type of the key item is {@link ItemPathType}
 * 2. each container value to be merged must have the key value specified
 */
public class ItemPathNaturalKeyImpl implements NaturalKey {

    @NotNull private final ItemName keyItemName;

    private ItemPathNaturalKeyImpl(@NotNull ItemName keyItemName) {
        this.keyItemName = keyItemName;
    }

    public static ItemPathNaturalKeyImpl of(@NotNull ItemName itemName) {
        return new ItemPathNaturalKeyImpl(itemName);
    }

    @Override
    public boolean valuesMatch(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue)
            throws ConfigurationException {
        return getItemPath(targetValue)
                .equivalent(
                        getItemPath(sourceValue));
    }

    /**
     * Source and target paths have to be equivalent here.
     *
     * We assume the source and target paths are purely name-based.
     * We go through them and construct the resulting path from the "most qualified" segments of these.
     *
     * We intentionally ignore some exotic cases here. (We are not obliged to create a "ideal" merged key, anyway.)
     */
    @Override
    public void mergeMatchingKeys(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue)
            throws ConfigurationException {
        ItemPath targetPath = getItemPath(targetValue);
        ItemPath sourcePath = getItemPath(sourceValue);

        assert targetPath.equivalent(sourcePath);

        if (targetPath.size() != sourcePath.size()) {
            return; // Paths are equivalent but differ in size -> ignore this case
        }

        List<Object> combinedPathSegments = new ArrayList<>(targetPath.size());
        for (int i = 0; i < targetPath.size(); i++) {
            Object targetSegment = targetPath.getSegment(i);
            Object sourceSegment = sourcePath.getSegment(i);
            if (!ItemPath.isName(targetSegment) || !ItemPath.isName(sourceSegment)) {
                return; // Exotic case -> ignore
            }
            ItemName targetSegmentName = ItemPath.toName(targetSegment);
            ItemName sourceSegmentName = ItemPath.toName(sourceSegment);
            if (!QNameUtil.match(targetSegmentName, sourceSegmentName)) {
                return; // Shouldn't occur!
            }
            if (QNameUtil.isQualified(targetSegmentName)) {
                combinedPathSegments.add(targetSegment);
            } else {
                combinedPathSegments.add(sourceSegment); // qualified or not
            }
        }

        try {
            targetValue.findProperty(keyItemName)
                    .replace(
                            PrismContext.get().itemFactory().createPropertyValue(
                                    new ItemPathType(
                                            ItemPath.create(combinedPathSegments))));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when updating '" + keyItemName + "'");
        }
    }

    private @NotNull ItemPath getItemPath(PrismContainerValue<?> containerValue) throws ConfigurationException {
        PrismProperty<ItemPathType> property =
                MiscUtil.requireNonNull(
                        containerValue.findProperty(keyItemName),
                        () -> new ConfigurationException("No '" + keyItemName + "' in " + containerValue));
        try {
            ItemPathType itemPathType = MiscUtil.requireNonNull(
                    property.getRealValue(ItemPathType.class),
                    () -> new ConfigurationException("No '" + keyItemName + "' in " + containerValue));
            return itemPathType.getItemPath();
        } catch (RuntimeException e) {
            throw new ConfigurationException("Couldn't get '" + keyItemName + "' from " + containerValue, e);
        }
    }
}

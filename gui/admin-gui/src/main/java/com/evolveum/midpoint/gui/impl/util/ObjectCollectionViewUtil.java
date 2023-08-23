/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ObjectCollectionViewUtil {

    public static @NotNull List<ObjectReferenceType> getArchetypeReferencesList(CompiledObjectCollectionView collectionView) {
        if (collectionView == null) {
            return new ArrayList<>();
        }
        var archetypeRef = collectionView.getArchetypeRef();
        return archetypeRef != null ? Arrays.asList(archetypeRef) : new ArrayList<>();
    }
}

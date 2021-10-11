/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.MiscUtil;

import java.util.Collection;

/**
 *
 */
public class ItemCollectionsUtil {
    public static boolean compareCollectionRealValues(Collection<? extends PrismProperty> col1, Collection<? extends PrismProperty> col2) {
        return MiscUtil.unorderedCollectionEquals(col1, col2,
                (p1, p2) -> {
                    if (!p1.getElementName().equals(p2.getElementName())) {
                        return false;
                    }
                    Collection p1RealVals = p1.getRealValues();
                    Collection p2RealVals = p2.getRealValues();
                    return MiscUtil.unorderedCollectionEquals(p1RealVals, p2RealVals);
                });
    }
}

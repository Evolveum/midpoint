/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.transformation;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.SourceTriple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.singleton;

/**
 * No functionality added to HashSet; just to make the code more readable.
 */
class SourceValues extends HashSet<PrismValue> {

    private SourceValues(Collection<? extends PrismValue> allValues) {
        super(allValues);
    }

    static List<SourceValues> fromSourceTripleList(List<SourceTriple<?, ?>> sourceTripleList) {
        List<SourceValues> sourceValuesList = new ArrayList<>(sourceTripleList.size());
        for (SourceTriple<?,?> sourceTriple: sourceTripleList) {
            SourceValues sourceValues;
            Collection<? extends PrismValue> values = sourceTriple.union();
            if (!values.isEmpty()) {
                sourceValues = new SourceValues(values);
            } else {
                // No values for this source. Use single null instead. It will make sure that the expression will
                // be evaluate at least once. (Otherwise no tuple of values could be constructed.)
                sourceValues = new SourceValues(singleton(null));
            }
            sourceValuesList.add(sourceValues);
        }
        return sourceValuesList;
    }
}

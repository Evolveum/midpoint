/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;

import java.text.Collator;
import java.util.Comparator;

/**
 * @author katka
 *
 */
public class ItemWrapperComparator<IW extends ItemWrapper> implements Comparator<IW> {

    private Collator collator;
    private boolean sorted;

    public ItemWrapperComparator(Collator collator, boolean sorted) {
        this.collator = collator;
        this.sorted = sorted;
    }

    @Override
    public int compare(IW id1, IW id2) {
        if (sorted) {
            return compareByDisplayNames(id1, id2, collator);
        }
        int displayOrder1 = (id1 == null || id1.getDisplayOrder() == null) ? Integer.MAX_VALUE : id1.getDisplayOrder();
        int displayOrder2 = (id2 == null || id2.getDisplayOrder() == null) ? Integer.MAX_VALUE : id2.getDisplayOrder();
        if (displayOrder1 == displayOrder2) {
            return compareByDisplayNames(id1, id1, collator);
        } else {
            return Integer.compare(displayOrder1, displayOrder2);
        }
    }

    private int compareByDisplayNames(IW pw1, IW pw2, Collator collator) {
        return collator.compare(pw1.getDisplayName(), pw2.getDisplayName());
    }
}

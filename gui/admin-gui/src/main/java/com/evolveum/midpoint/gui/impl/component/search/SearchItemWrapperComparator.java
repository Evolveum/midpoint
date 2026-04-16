/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.Comparator;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;

import org.apache.commons.lang3.StringUtils;

/**
 * @author matisovaa
 *
 */
public class SearchItemWrapperComparator<IW extends FilterableSearchItemWrapper> implements Comparator<IW> {

    @Override
    public int compare(IW i1, IW i2) {
        int displayOrder1 = getDisplayOrderFromSearchItemWrapper(i1);
        int displayOrder2 = getDisplayOrderFromSearchItemWrapper(i2);
        if (displayOrder1 == displayOrder2) {
            return String.CASE_INSENSITIVE_ORDER.compare(getNameFromSearchItemWrapper(i1), getNameFromSearchItemWrapper(i2));
        } else {
            return Integer.compare(displayOrder1, displayOrder2);
        }
    }

    private int getDisplayOrderFromSearchItemWrapper(FilterableSearchItemWrapper<?> searchItemWrapper) {
        return (searchItemWrapper == null || searchItemWrapper.getDisplayOrder() == null) ?
                Integer.MAX_VALUE :
                searchItemWrapper.getDisplayOrder();
    }

    private String getNameFromSearchItemWrapper(FilterableSearchItemWrapper<?> searchItemWrapper) {
        if (searchItemWrapper == null) {
            return "";
        }
        return StringUtils.isEmpty(searchItemWrapper.getName().getObject()) ?
                "" :
                PageBase.createStringResourceStatic(searchItemWrapper.getName().getObject()).getString();
    }
}

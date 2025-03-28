/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.DateSearchItemWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import javax.xml.datatype.XMLGregorianCalendar;

public class DateSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<XMLGregorianCalendar, DateSearchItemWrapper> {
    @Override
    protected DateSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        DateSearchItemWrapper wrapper = new DateSearchItemWrapper(ctx.getPath());
        //ticket 9828; using date search item as non-interval to be used as a report parameter value
        if (ctx.isReportCollectionSearch()) {
            wrapper.setInterval(false);
        }

        if (ctx.getIntervalPresets() != null) {
            wrapper.setIntervalPresets(ctx.getIntervalPresets());
            wrapper.setSelectedIntervalPreset(ctx.getSelectedIntervalPreset());
        }

        return wrapper;
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return QNameUtil.match(ctx.getValueTypeName(), DOMUtil.XSD_DATETIME);
    }
}

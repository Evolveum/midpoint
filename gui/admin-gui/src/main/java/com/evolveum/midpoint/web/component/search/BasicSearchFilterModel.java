/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.filter.BasicSearchFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import java.io.Serial;

/**
 * @author honchar
 */
public class BasicSearchFilterModel<O extends ObjectType> implements IModel<BasicSearchFilter<O>> {

    private static final Trace LOGGER = TraceManager.getTrace(BasicSearchFilterModel.class);

    @Serial private static final long serialVersionUID = 1L;

    private IModel<SearchFilterType> baseModel;
    private PageBase pageBase;
    private Class<O> type;
    private BasicSearchFilter<O> basicSearchFilter;

    public BasicSearchFilterModel(IModel<SearchFilterType> valueWrapper, Class<O> type, PageBase pageBase) {
        this.baseModel = valueWrapper;
        this.pageBase = pageBase;
        this.type = type;
    }

    @Override
    public void detach() {
        // TODO Auto-generated method stub

    }

    @Override
    public BasicSearchFilter<O> getObject() {
        if (basicSearchFilter == null) {
            basicSearchFilter = loadBasicSearchFilter();
        }
        return basicSearchFilter;
    }

    private BasicSearchFilter<O> loadBasicSearchFilter() {
        ObjectQuery objectFilter;
        try {
            objectFilter = pageBase.getPrismContext().getQueryConverter().createObjectQuery(type, baseModel.getObject());
        } catch (SchemaException | IllegalStateException | IllegalArgumentException e) {
            objectFilter = pageBase.getPrismContext().queryFor(type).build();
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
        }
        return new BasicSearchFilter<O>(pageBase, objectFilter.getFilter(), type);
    }

    @Override
    public void setObject(BasicSearchFilter<O> object) {
    }
}

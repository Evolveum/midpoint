/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;

public class QueryTypeModel implements IModel<String> {

    private static final transient Trace LOGGER = TraceManager.getTrace(QueryTypeModel.class);

    private IModel<QueryType> baseModel;
    private ItemName itemName;
    private PrismContext prismContext;

    public QueryTypeModel(IModel<QueryType> baseModel, ItemName itemName, PrismContext prismContext) {
        this.baseModel = baseModel;
        this.itemName = itemName;
        this.prismContext = prismContext;
    }

    @Override
    public String getObject() {
        QueryType query = baseModel.getObject();
        if (query == null) {
            return null;
        }

        try {
            return prismContext.xmlSerializer().serializeAnyData(query, itemName);
        } catch (SchemaException e) {
            LOGGER.error("Cannot serialize query, {}\n{}", e.getMessage(), query, e);
            Session.get().error("Cannot serialize query: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void setObject(String object) {

        if (StringUtils.isBlank(object)) {
            return;
        }

        try {
            QueryType query = prismContext.parserFor(object).parseRealValue();
            baseModel.setObject(query);
        } catch (SchemaException e) {
            LOGGER.error("Cannot parse query, {}\n{}", e.getMessage(), object, e);
            Session.get().error("Cannot parse query: " + e.getMessage());
        }


    }
}

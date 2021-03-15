/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

public class SearchFilterTypeForXmlModel extends SearchFilterTypeModel {

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterTypeForXmlModel.class);

    private static final long serialVersionUID = 1L;

    public SearchFilterTypeForXmlModel(IModel<SearchFilterType> valueWrapper, PageBase pageBase) {
        super(valueWrapper, pageBase);
    }

    @Override
    public String getObject() {
        try {
            SearchFilterType value = getBaseModel().getObject();
            if (value == null) {
                return null;
            }

            return getPageBase().getPrismContext().xmlSerializer().serializeRealValue(value);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
            getPageBase().error("Cannot parse filter: " + e.getMessage() + ". For more details, please, see midpoint log");
        }
        return null;
    }

    @Override
    public void setObject(String object) {
        if (StringUtils.isBlank(object)) {
            return;
        }

        try {
            SearchFilterType filter = getPageBase().getPrismContext().parserFor(object).parseRealValue(SearchFilterType.class);
            getBaseModel().setObject(filter);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse filter", e);
            getPageBase().error("Cannot parse filter: " + e.getMessage() + ". For more details, please, see midpoint log");
        }
    }
}

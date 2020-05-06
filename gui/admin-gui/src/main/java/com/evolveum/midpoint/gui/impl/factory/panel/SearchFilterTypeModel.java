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

public class SearchFilterTypeModel implements IModel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterTypeModel.class);

        private static final long serialVersionUID = 1L;

        private IModel<SearchFilterType> baseModel;
        private PageBase pageBase;

        public SearchFilterTypeModel(IModel<SearchFilterType> valueWrapper, PageBase pageBase) {
            this.baseModel = valueWrapper;
            this.pageBase = pageBase;
        }

        @Override
        public void detach() {
            // TODO Auto-generated method stub

        }

        @Override
        public String getObject() {
            try {
                SearchFilterType value = baseModel.getObject();
                if (value == null) {
                    return null;
                }

                return pageBase.getPrismContext().xmlSerializer().serializeRealValue(value);
            } catch (SchemaException e) {
                // TODO handle!!!!
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
//                getSession().error("Cannot serialize filter");
            }
            return null;
        }

        @Override
        public void setObject(String object) {
            if (StringUtils.isBlank(object)) {
                return;
            }

            try {
                SearchFilterType filter = pageBase.getPrismContext().parserFor(object).parseRealValue(SearchFilterType.class);
                baseModel.setObject(filter);
            } catch (SchemaException e) {
                // TODO handle!!!!
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse filter", e);
//                getSession().error("Cannot parse filter");
            }

        }
}

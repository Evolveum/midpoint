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
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;

public class SearchFilterTypeModel implements IComponentAssignedModel<String> {

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
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
                throw new IllegalStateException("Cannot serialize filter: " + e.getMessage() + ". For more details, please, see midpoint log");
            }
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
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse filter", e);
                throw new IllegalStateException("Cannot parse filter: " + e.getMessage() + ". For more details, please, see midpoint log");
            }

        }

    @Override
    public IWrapModel<String> wrapOnAssignment(Component component) {
        return new SearchFilterWrapperModel(component);
    }

    class SearchFilterWrapperModel implements IWrapModel<String> {

            private Component component;

            public SearchFilterWrapperModel(Component component) {
                this.component = component;
            }

        @Override
        public IModel<?> getWrappedModel() {
            return SearchFilterTypeModel.this;
        }

        @Override
        public String getObject() {
            try {
                return SearchFilterTypeModel.this.getObject();
            } catch (Throwable e) {
                component.error(e.getMessage());
            }
            return null;
        }

        @Override
        public void setObject(String object) {
            try {
                SearchFilterTypeModel.this.setObject(object);
            } catch (Throwable e) {
                component.error(e.getMessage());
            }
        }
    }
}

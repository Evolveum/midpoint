/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author skublik
 */

public class ExportingFilterTabPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(ExportingFilterTabPanel.class);

    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_CHECK = "check";
    private static final String ID_FILTER_FIELD = "filter";

    private LoadableModel<Search> search;

    private IModel<Boolean> check = new IModel<Boolean>() {
        private boolean check = false;

        @Override
        public Boolean getObject() {
            return check;
        }

        @Override
        public void setObject(Boolean object) {
            check = Boolean.TRUE.equals(object);
        }
    };

    private IModel<String> searchFilter = new IModel<String>() {
        private String value = null;
        @Override
        public String getObject() {
            if (value == null) {
                return null;
            }
            return value;
        }

        @Override
        public void setObject(String object) {
            if (StringUtils.isBlank(object)) {
                return;
            }

            value = object;
        }
    };

    public ExportingFilterTabPanel(String id, LoadableModel<Search> search) {
        super(id);
        this.search = search;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayer();
    }

    private void initLayer() {

        StringResourceModel messageModel = getPageBase().createStringResource("ExportingFilterTabPanel.message.useFilter");
        MessagePanel warningMessage = new MessagePanel(ID_WARNING_MESSAGE, MessagePanel.MessagePanelType.WARN, messageModel);
        warningMessage.setOutputMarkupId(true);
        add(warningMessage);

        AceEditor filterField = new AceEditor(ID_FILTER_FIELD, searchFilter);
        filterField.setMinHeight(200);
        filterField.setResizeToMaxHeight(false);
        filterField.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        filterField.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isEnabled() {
                return !check.getObject();
            }
        });
        add(filterField);

        CheckBoxPanel checkPanel = new CheckBoxPanel(ID_CHECK, check,
                getPageBase().createStringResource("ExportingFilterTabPanel.searchFromList"), Model.of(""));
        add(checkPanel);
    }

    public SearchFilterType getFilter() throws Exception {
        if (check.getObject()) {
            ObjectQuery query = search.getObject().createObjectQuery(getPageBase().getPrismContext());
            if (query == null) {
                return null;
            }
            ObjectFilter filter = query.getFilter();
            SearchFilterType origSearchFilter = getPageBase().getPrismContext().getQueryConverter().createSearchFilterType(filter);
            return origSearchFilter;
        }
        if (!StringUtils.isEmpty(searchFilter.getObject())) {
            SearchFilterType filter = getPageBase().getPrismContext().parserFor(searchFilter.getObject()).parseRealValue(SearchFilterType.class);
            return filter;
        }
        return null;
    }

    public boolean useFilterFromSearchPanel() {
        return Boolean.TRUE.equals(check.getObject());
    }
}

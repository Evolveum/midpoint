/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

/**
 * @author honchar
 */
public class FilterSearchItemPanel<T extends Serializable> extends BasePanel<SearchItem<T>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(FilterSearchItemPanel.class);

    private static final String ID_FILTER_SELECT = "filterSelect";
    private static final String ID_FILTER_NAME = "filterName";
    private static final String ID_FILTER_SEARCH_ITEM_CONTAINER = "filterSearchItemContainer";

    public FilterSearchItemPanel(String id, IModel<SearchItem<T>> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer filterContainer = new WebMarkupContainer(ID_FILTER_SEARCH_ITEM_CONTAINER);
        filterContainer.setOutputMarkupId(true);
        add(filterContainer);

        CheckBox filterSelect = new CheckBox(ID_FILTER_SELECT, Model.of(Boolean.FALSE));
        filterSelect.setOutputMarkupId(true);
        filterContainer.add(filterSelect);

        Label filterName = new Label(ID_FILTER_NAME, getFilterNameModel());
        filterName.setOutputMarkupId(true);
        filterName.add(AttributeAppender.append("title", getFilterTitleModel()));
        filterContainer.add(filterName);
    }

    private IModel<String> getFilterNameModel(){
        return Model.of(WebComponentUtil.getTranslatedPolyString(getModelObject().getPredefinedFilter().getDisplayName()));
    }

    private IModel<String> getFilterTitleModel(){
        return Model.of(""); //todo get string presentation of the filter?
    }

}

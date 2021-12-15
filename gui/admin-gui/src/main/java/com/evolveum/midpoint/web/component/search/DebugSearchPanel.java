/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.web.page.admin.configuration.component.DebugSearchFragment;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
/**
 * @author honchar
 */
public class DebugSearchPanel<O extends ObjectType> extends AbstractSearchPanel<O> {

    IModel<Boolean> showAllItemsModel;
    private static final String ID_DEBUG_SEARCH_FRAGMENT = "debugSearchFragment";

    public DebugSearchPanel(String id, IModel<Search<O>> model, IModel<Boolean> showAllItemsModel) {
        super(id, model);
        this.showAllItemsModel = showAllItemsModel;
    }

    protected void initSearchItemsPanel(RepeatingView searchItemsRepeatingView) {
        DebugSearchFragment debugSearchFragment = new DebugSearchFragment(searchItemsRepeatingView.newChildId(), ID_DEBUG_SEARCH_FRAGMENT,
                DebugSearchPanel.this, getModel(), showAllItemsModel);
        debugSearchFragment.setOutputMarkupId(true);
        searchItemsRepeatingView.add(debugSearchFragment);

        super.initSearchItemsPanel(searchItemsRepeatingView);
    }

    protected void searchPerformed(AjaxRequestTarget target) {

    }

}

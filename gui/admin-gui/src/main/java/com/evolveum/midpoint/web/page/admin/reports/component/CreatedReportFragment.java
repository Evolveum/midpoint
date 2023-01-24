/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.StringChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CreatedReportFragment extends Fragment {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH = "search";
    private static final String ID_REPORT_TYPE = "reportType";

    private IModel<Search<ReportDataType>> search;

    private IModel<String> reportType;

    private List<String> values;

    public CreatedReportFragment(String id, String markupId, MarkupContainer markupProvider,
            IModel<Search<ReportDataType>> model, IModel<String> reportType, List<String> values) {
        super(id, markupId, markupProvider);

        this.search = model;
        this.reportType = reportType;
        this.values = values;

        initLayout();
    }

    private void initLayout() {
        DropDownChoicePanel<String> reportType = new DropDownChoicePanel<>
                (ID_REPORT_TYPE, this.reportType,
                        Model.ofList(values),
                        StringChoiceRenderer.simple(), true);

        reportType.getBaseFormComponent().add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                CreatedReportFragment.this.onReportTypeUpdate(target);
            }
        });
        reportType.setOutputMarkupId(true);
        add(reportType);

        SearchPanel search = new SearchPanel<>(ID_SEARCH, CreatedReportFragment.this.search) {

            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                CreatedReportFragment.this.searchPerformed(target);
            }
        };
        search.setOutputMarkupId(true);
        add(search);
    }

    protected void onReportTypeUpdate(AjaxRequestTarget target) {

    }

    protected void searchPerformed(AjaxRequestTarget target) {

    }
}

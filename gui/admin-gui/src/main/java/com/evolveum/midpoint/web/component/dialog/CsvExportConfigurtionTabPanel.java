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
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author skublik
 */

public class CsvExportConfigurtionTabPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(ExportingFilterTabPanel.class);

    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_CSV_FIELD = "csv";

    private LoadableModel<Search> search;
    private FeedbackAlerts feedbackList;
    private IModel<PrismObjectWrapper<ReportType>> report;

    public CsvExportConfigurtionTabPanel(String id, IModel<PrismObjectWrapper<ReportType>> report) {
        super(id);
        this.report = report;
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

        Panel csv = null;
        try {
            csv = getPageBase().initItemPanel(ID_CSV_FIELD, CsvExportType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(report,
                            ItemPath.create(ReportType.F_EXPORT, ExportConfigurationType.F_CSV)), new ItemPanelSettingsBuilder().build());
        } catch (SchemaException e) {
            LOGGER.error("Could not create panel for filter. Reason: {}", e.getMessage(), e);
        }
        add(csv);
    }

    public CsvExportType getFilter() throws Exception {
        PrismContainerWrapper<CsvExportType> csv = report.getObject().findContainer(ItemPath.create(ReportType.F_EXPORT, ExportConfigurationType.F_CSV));
        return csv.getValue().getRealValue();
    }
}

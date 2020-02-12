/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ReportCreateHandlerDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.io.InputStream;

/**
 * @author mederly
 */
public class ReportCreateHandlerPanel extends DefaultHandlerPanel<ReportCreateHandlerDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_DOWNLOAD_CONTAINER = "downloadContainer";
    private static final String ID_DOWNLOAD = "download";
    private static final String ID_REPORT_PARAMETERS_CONTAINER = "reportParametersContainer";
    private static final String ID_REPORT_PARAMETERS = "reportParameters";

    private static final String OPERATION_LOAD_REPORT_OUTPUT = ReportCreateHandlerPanel.class.getName() +"." + "loadReportOutput";

    public ReportCreateHandlerPanel(String id, IModel<ReportCreateHandlerDto> model, PageBase parentPage) {
        super(id, model, parentPage);
        initLayout(parentPage);
    }

    private void initLayout(final PageBase parentPage) {

        final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {
            private static final long serialVersionUID = 1L;

            @Override
            protected InputStream initStream() {
                ReportOutputType reportObject = getReportOutput(parentPage);
                if (reportObject != null) {
                    return PageCreatedReports.createReport(reportObject, this, parentPage);
                } else {
                    return null;
                }
            }


            @Override
            public String getFileName() {
                ReportOutputType reportObject = getReportOutput(parentPage);
                return PageCreatedReports.getReportFileName(reportObject);
            }
        };
//        parentPage.getForm().add(ajaxDownloadBehavior);

        WebMarkupContainer reportParametersContainer = new WebMarkupContainer(ID_REPORT_PARAMETERS_CONTAINER);
        TextArea reportParameters = new TextArea<>(ID_REPORT_PARAMETERS, new PropertyModel<>(getModel(), ReportCreateHandlerDto.F_REPORT_PARAMS));
        reportParameters.setEnabled(false);
        reportParametersContainer.add(reportParameters);
        add(reportParametersContainer);

        WebMarkupContainer downloadContainer = new WebMarkupContainer(ID_DOWNLOAD_CONTAINER);
        AjaxButton download = new AjaxButton(ID_DOWNLOAD) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ajaxDownloadBehavior.initiate(target);
            }
        };
        downloadContainer.add(download);
//        downloadContainer.add(new VisibleEnableBehaviour() {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                ReportOutputType reportObject = getReportOutput(parentPage);
//                return getModelObject().getReportOutputOid() != null && reportObject != null;
//            }
//        });
        add(downloadContainer);
    }

    private ReportOutputType getReportOutput(PageBase parentPage) {
        String outputOid = null;//getModelObject().getReportOutputOid();

        if (outputOid == null) {
            return null;
        }
        Task task = parentPage.createSimpleTask(OPERATION_LOAD_REPORT_OUTPUT);
        PrismObject<ReportOutputType> reportOutput = WebModelServiceUtils.loadObject(ReportOutputType.class, outputOid, parentPage, task, task.getResult());

        if (reportOutput == null) {
            return null;
        }
        return reportOutput.asObjectable();
    }

}

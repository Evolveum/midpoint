/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
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

	private static final String ID_DOWNLOAD_CONTAINER = "downloadContainer";
	private static final String ID_DOWNLOAD = "download";
	private static final String ID_REPORT_PARAMETERS_CONTAINER = "reportParametersContainer";
	private static final String ID_REPORT_PARAMETERS = "reportParameters";

	private static final String OPERATION_LOAD_REPORT_OUTPUT = PageTaskEdit.DOT_CLASS + "loadReportOutput";

	public ReportCreateHandlerPanel(String id, IModel<ReportCreateHandlerDto> model, PageTaskEdit parentPage) {
		super(id, model, parentPage);
		initLayout(parentPage);
	}

	private void initLayout(final PageTaskEdit parentPage) {
		final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {

			@Override
			protected InputStream initStream() {
				String outputOid = getModelObject().getReportOutputOid();
				if (outputOid == null) {
					return null;
				}
				Task task = parentPage.createSimpleTask(OPERATION_LOAD_REPORT_OUTPUT);
				PrismObject<ReportOutputType> reportObject = WebModelServiceUtils.loadObject(ReportOutputType.class, outputOid, parentPage, task, task.getResult());
				if (reportObject != null) {
					return PageCreatedReports.createReport(reportObject.asObjectable(), this, parentPage);
				} else {
					return null;
				}
			}
		};
		parentPage.getForm().add(ajaxDownloadBehavior);

		WebMarkupContainer reportParametersContainer = new WebMarkupContainer(ID_REPORT_PARAMETERS_CONTAINER);
		TextArea reportParameters = new TextArea<>(ID_REPORT_PARAMETERS, new PropertyModel<>(getModel(), ReportCreateHandlerDto.F_REPORT_PARAMS));
		reportParameters.setEnabled(false);
		reportParametersContainer.add(reportParameters);
		add(reportParametersContainer);

		WebMarkupContainer downloadContainer = new WebMarkupContainer(ID_DOWNLOAD_CONTAINER);
		AjaxButton download = new AjaxButton(ID_DOWNLOAD) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				ajaxDownloadBehavior.initiate(target);
			}
		};
		downloadContainer.add(download);
		downloadContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().getReportOutputOid() != null;
			}
		});
		add(downloadContainer);
	}
}

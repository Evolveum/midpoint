/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.reports;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ajaxDownload.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.ThreeStateCheckPanel;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.reports.dto.UserFilterDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author mserbak
 */
public class PageReports extends PageAdminReports {

	private static final Trace LOGGER = TraceManager.getTrace(PageReports.class);
	private static final String DOT_CLASS = PageReports.class.getName() + ".";
	private static final String OPERATION_CREATE_RESOURCE_LIST = DOT_CLASS + "createResourceList";
	private LoadableModel<UserFilterDto> userFilterModel;
	private List listObject;

	public PageReports() {
		userFilterModel = new LoadableModel<UserFilterDto>(false) {

			@Override
			protected UserFilterDto load() {
				return new UserFilterDto();
			}
		};
		initLayout();
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		OptionPanel option = new OptionPanel("option", createStringResource("pageReports.optionsTitle"),
				getPage(), false);
		option.setOutputMarkupId(true);
		mainForm.add(option);

		OptionItem diagnostics = new OptionItem("objectsType",
				createStringResource("pageReports.objectsType"));
		option.getBodyContainer().add(diagnostics);

		DropDownChoice objectTypeChoice = new DropDownChoice("objectTypeChoice",
				new AbstractReadOnlyModel<Class>() {

					@Override
					public Class getObject() {
						return UserType.class;
					}
				}, createObjectListModel(), new IChoiceRenderer<Class>() {

					@Override
					public Object getDisplayValue(Class clazz) {
						return clazz.getSimpleName();
					}

					@Override
					public String getIdValue(Class clazz, int index) {
						return Integer.toString(index);
					}
				}) {

			@Override
			protected void onSelectionChanged(Object newSelection) {
				super.onSelectionChanged(newSelection);
			}
		};
		diagnostics.add(objectTypeChoice);

		OptionItem usersFilter = new OptionItem("usersFilter",
				createStringResource("pageReports.usersFilter"), true);
		option.getBodyContainer().add(usersFilter);
		initSearch(usersFilter);

		OptionContent content = new OptionContent("optionContent");
		mainForm.add(content);
		initTable(content, mainForm);
	}

	private IModel<List<Class>> createObjectListModel() {
		return new AbstractReadOnlyModel<List<Class>>() {

			@Override
			public List<Class> getObject() {
				List<Class> list = new ArrayList<Class>();
				list.add(UserType.class);
				return list;
			}
		};
	}

	private IModel<List<PrismObject<ResourceType>>> createResourceListModel() {
		return new AbstractReadOnlyModel<List<PrismObject<ResourceType>>>() {

			@Override
			public List<PrismObject<ResourceType>> getObject() {
				List<PrismObject<ResourceType>> list = new ArrayList<PrismObject<ResourceType>>();
				OperationResult result = new OperationResult(OPERATION_CREATE_RESOURCE_LIST);
				Task task = createSimpleTask(OPERATION_CREATE_RESOURCE_LIST);
				try {
					list = getModelService().searchObjects(ResourceType.class, new ObjectQuery(), null, task,
							result);
					result.recordSuccess();
				} catch (Exception ex) {
					result.recordFatalError("Couldn't list resources.", ex);
				}
				if (!result.isSuccess()) {
					showResult(result);
				}
				return list;
			}
		};
	}

	private void initSearch(OptionItem item) {
		TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(
				userFilterModel, "searchText"));
		item.add(search);

		ThreeStateCheckPanel activationCheck = new ThreeStateCheckPanel("activationEnabled",
				new PropertyModel<Boolean>(userFilterModel, "activated"));
		activationCheck.setStyle("margin: 1px 1px 1px 6px;");
		item.add(activationCheck);

		DropDownChoice resourceChoice = new DropDownChoice("resourceChoice",
				new PropertyModel<PrismObject<ResourceType>>(userFilterModel, "resource"),
				createResourceListModel(), new IChoiceRenderer<PrismObject<ResourceType>>() {

					@Override
					public Object getDisplayValue(PrismObject<ResourceType> resource) {
						return WebMiscUtil.getName(resource);
					}

					@Override
					public String getIdValue(PrismObject<ResourceType> resource, int index) {
						return Integer.toString(index);
					}
				});
		resourceChoice.setNullValid(true);
		item.add(resourceChoice);

		AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
				createStringResource("pageReports.button.clearButton")) {

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}

			@Override
			public void onSubmit(AjaxRequestTarget target, Form<?> form) {
				clearButtonPerformed(target);
			}
		};
		item.add(clearButton);

		AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
				createStringResource("pageReports.button.searchButton")) {

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				searchPerformed(target);
			}
		};
		item.add(searchButton);
	}

	private void initTable(OptionContent content, Form mainForm) {
		List<IColumn<SelectableBean>> columns = initColumns(mainForm);
		TablePanel table = new TablePanel<SelectableBean>("reportsTable", new ObjectDataProvider(
				PageReports.this, UserType.class), columns);
		table.setOutputMarkupId(true);
		content.getBodyContainer().add(table);
	}

	private List<IColumn<SelectableBean>> initColumns(Form mainForm) {
		List<IColumn<SelectableBean>> columns = new ArrayList<IColumn<SelectableBean>>();

		IColumn column = new CheckBoxHeaderColumn();
		columns.add(column);

		final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream(true) {

			@Override
			protected byte[] initStream() {
				return createReport();
			}
		};
		ajaxDownloadBehavior.setContentType("application/pdf; charset=UTF-8");
		mainForm.add(ajaxDownloadBehavior);

		column = new LinkColumn<SelectableBean>(createStringResource("pageReports.reportLink"), "fullName",
				"value.fullName.orig") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean> rowModel) {
				Serializable value = rowModel.getObject().getValue();
				if (value instanceof UserType) {
					List list = new ArrayList();
					UserType user = (UserType) value;
					list.add(user);
					listObject = list;
					ajaxDownloadBehavior.initiate(target);
				}
			}
		};
		columns.add(column);

		return columns;
	}

	private ObjectQuery createQuery() {
		UserFilterDto dto = userFilterModel.getObject();
		ObjectQuery query = null;

		try {
			List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

			PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
			if (normalizer == null) {
				normalizer = new PrismDefaultPolyStringNormalizer();
			}
			if (!StringUtils.isEmpty(dto.getSearchText())) {
				String normalizedString = normalizer.normalize(dto.getSearchText());
				filters.add(SubstringFilter.createSubstring(UserType.class, getPrismContext(),
						UserType.F_NAME, normalizedString));
			}

			if (dto.isActivated() != null) {
				filters.add(EqualsFilter.createEqual(UserType.class, getPrismContext(),
						UserType.F_ACTIVATION, dto.isActivated()));

			}

			if (dto.getResource() != null) {
				filters.add(RefFilter.createReferenceEqual(UserType.class, UserType.F_ACCOUNT_REF,
						getPrismContext(), dto.getResource().getOid()));
			}

			if (filters.size() == 1) {
				query = ObjectQuery.createObjectQuery(filters.get(0));
			} else if (filters.size() > 1) {
				query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));
			}

		} catch (Exception ex) {
			error(getString("pageReports.message.queryError") + " " + ex.getMessage());
			LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
		}

		return query;
	}

	private TablePanel getTable() {
		OptionContent content = (OptionContent) get("mainForm:optionContent");
		return (TablePanel) content.getBodyContainer().get("reportsTable");
	}

	private void searchPerformed(AjaxRequestTarget target) {
		ObjectQuery query = createQuery();
		target.add(getFeedbackPanel());

		TablePanel panel = getTable();
		DataTable table = panel.getDataTable();
		ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
		provider.setQuery(query);
		table.setCurrentPage(0);
		target.add(panel);
	}

	private void clearButtonPerformed(AjaxRequestTarget target) {
		userFilterModel.reset();
		target.appendJavaScript("init()");
		target.add(get("mainForm:option"));
		searchPerformed(target);
	}

	private byte[] createReport() {
		if(listObject == null || listObject.isEmpty()) {
			//return null;
		}
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("paramName", "Janko Hraško");

		JasperDesign design;
		JasperReport report = null;
		JasperPrint jasperPrint = null;

		try {
            ServletContext servletContext = ((WebApplication) getApplication()).getServletContext();
			// Loading template
			design = JRXmlLoader.load(servletContext.getRealPath("/reports/Report.jrxml"));
			report = JasperCompileManager.compileReport(design);
			jasperPrint = JasperFillManager.fillReport(report, parameterMap, new JREmptyDataSource());

		} catch (JRException ex) {
			error(getString("pageReports.message.jasperError") + " " + ex.getMessage());
			LoggingUtils.logException(LOGGER, "Couldn't create jasper report.", ex);
		}
		return JasperReports.getData(jasperPrint);
	}
}

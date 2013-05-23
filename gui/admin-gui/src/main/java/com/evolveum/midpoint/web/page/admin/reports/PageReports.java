/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.reports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.sql.Timestamp;
import javax.xml.namespace.QName;

import javax.servlet.ServletContext;

import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.query.JRHibernateQueryExecuterFactory;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.joda.time.DateTime;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ajaxDownload.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.reports.dto.UserFilterDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author mserbak
 */
public class PageReports extends PageAdminReports {

	private static final Trace LOGGER = TraceManager.getTrace(PageReports.class);
	private static final String DOT_CLASS = PageReports.class.getName() + ".";
	private static final String OPERATION_CREATE_RESOURCE_LIST = DOT_CLASS + "createResourceList";
	private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "getObjects";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_OPTION = "option";
    private static final String ID_OBJECTS_TYPE = "objectsType";
    private static final String ID_OBJECTS_TYPE_CHOICE = "objectTypeChoice";
    private static final String ID_USERS_FILTER = "usersFilter";
    private static final String ID_OPTION_CONTENT = "optionContent";

	private LoadableModel<UserFilterDto> userFilterDto;
	private List<DebugObjectItem> listObject;
	private AjaxDownloadBehaviorFromStream ajaxDownloadBehavior;
	@SpringBean(name = "sessionFactory")
    private SessionFactory sessionFactory;

	public PageReports() {
		userFilterDto = new LoadableModel<UserFilterDto>(false) {

			@Override
			protected UserFilterDto load() {
				UserFilterDto dto = new UserFilterDto();
				List<PrismObject<ResourceType>> resourceList = createResourceListModel().getObject();
				if(!resourceList.isEmpty()) {
					dto.setResource(resourceList.get(0));
				}
				return dto;
			}
		};
		initLayout();
	}

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);

		ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream(true) {
			@Override
			protected byte[] initStream() {
				return createReport();
			}
		};
		ajaxDownloadBehavior.setContentType("application/pdf; charset=UTF-8");
		mainForm.add(ajaxDownloadBehavior);

		OptionPanel option = new OptionPanel(ID_OPTION, createStringResource("pageReports.optionsTitle"), false);
		option.setOutputMarkupId(true);
		mainForm.add(option);

		OptionItem objectsType = new OptionItem(ID_OBJECTS_TYPE, createStringResource("pageReports.objectsType"));
		option.getBodyContainer().add(objectsType);
		objectsType.setVisible(false);

		DropDownChoice objectTypeChoice = new DropDownChoice(ID_OBJECTS_TYPE_CHOICE,
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
		objectsType.add(objectTypeChoice);

		OptionItem usersFilter = new OptionItem(ID_USERS_FILTER, createStringResource("pageReports.usersFilter"), true);
		option.getBodyContainer().add(usersFilter);
		initSearch(usersFilter);

		OptionContent content = new OptionContent(ID_OPTION_CONTENT);
		mainForm.add(content);
		initTable(content, mainForm);

		initButtons(mainForm);
	}

	private void initButtons(Form mainForm) {
		AjaxLinkButton selectedReport = new AjaxLinkButton("selectedReport",
				createStringResource("pageReports.button.selectedReport")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				List<DebugObjectItem> list = new ArrayList<DebugObjectItem>();
				List<DebugObjectItem> beans = WebMiscUtil.getSelectedData(getTable());
                list.addAll(beans);

				if(!list.isEmpty()) {
					listObject = list;
					ajaxDownloadBehavior.initiate(target);
				} else {
					warn(getString("pageReports.message.nothingSelected"));
			        target.add(getFeedbackPanel());
			        target.add(getTable());
				}

			}
		};
		mainForm.add(selectedReport);

		AjaxLinkButton allReport = new AjaxLinkButton("allReport",
				createStringResource("pageReports.button.allReport")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				listObject = getObjects();
				ajaxDownloadBehavior.initiate(target);
			}
		};
		mainForm.add(allReport);
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
					list = getModelService().searchObjects(ResourceType.class, new ObjectQuery(), SelectorOptions.createCollection(new ItemPath(), GetOperationOptions.createRaw()), task,
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
				userFilterDto, "searchText"));
		item.add(search);

		TriStateComboPanel activationCheck = new TriStateComboPanel("activationEnabled",
				new PropertyModel<Boolean>(userFilterDto, "activated"));
//		activationCheck.setStyle("margin: 1px 1px 1px 6px;");
		item.add(activationCheck);
		activationCheck.setVisible(false);

		final Image resourceHelp = new Image("resourceHelp", new PackageResourceReference(ImgResources.class,
				ImgResources.TOOLTIP_INFO));
		resourceHelp.setOutputMarkupId(true);
		resourceHelp.add(new AttributeAppender("original-title", getString("pageReports.resourceHelp")));
		resourceHelp.add(new AbstractDefaultAjaxBehavior() {
			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				String js = "$('#"+ resourceHelp.getMarkupId() +"').tipsy()";
				response.render(OnDomReadyHeaderItem.forScript(js));
				super.renderHead(component, response);
			}

			@Override
			protected void respond(AjaxRequestTarget target) {
			}
		});
		item.add(resourceHelp);

		DropDownChoice resourceChoice = new DropDownChoice("resourceChoice",
				new PropertyModel<PrismObject<ResourceType>>(userFilterDto, "resource"),
                createResourceListModel(), new IChoiceRenderer<PrismObject<ResourceType>>() {

					@Override
					public Object getDisplayValue(PrismObject<ResourceType> resource) {
						return WebMiscUtil.getName(resource);
					}

					@Override
					public String getIdValue(PrismObject<ResourceType> resource, int index) {
						return Integer.toString(index);
					}
				}){

			@Override
			protected boolean wantOnSelectionChangedNotifications() {
				return true;
			}

			@Override
			protected void onSelectionChanged(Object newSelection) {
				userFilterDto.getObject().setResource((PrismObject<ResourceType>) newSelection);
			}

		};
		resourceChoice.setNullValid(false);
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
		List<IColumn<DebugObjectItem, String>> columns = initColumns(mainForm);
		TablePanel table = new TablePanel<DebugObjectItem>("reportsTable", new RepositoryObjectDataProvider(
				PageReports.this, UserType.class), columns);
		table.setOutputMarkupId(true);
		content.getBodyContainer().add(table);
	}

	private List<IColumn<DebugObjectItem, String>> initColumns(Form mainForm) {
		List<IColumn<DebugObjectItem, String>> columns = new ArrayList<IColumn<DebugObjectItem, String>>();

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

		column = new LinkColumn<DebugObjectItem>(createStringResource("pageReports.reportLink"), "fullName") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<DebugObjectItem> rowModel) {
				DebugObjectItem value = rowModel.getObject();
                listObject = new ArrayList<DebugObjectItem>();
                listObject.add(value);

                ajaxDownloadBehavior.initiate(target);
			}
		};
		columns.add(column);

		return columns;
	}

	private ObjectQuery createQuery() {
		UserFilterDto dto = userFilterDto.getObject();
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
						UserType.F_FULL_NAME, normalizedString));
			}

//			if (dto.isActivated() != null) {
//				filters.add(EqualsFilter.createEqual(UserType.class, getPrismContext(),
//						UserType.F_ACTIVATION, dto.isActivated()));
//
//			}

//			if (dto.getResource() != null) {
//				filters.add(RefFilter.createReferenceEqual(UserType.class, UserType.F_ACCOUNT_REF,
//						getPrismContext(), dto.getResource().getOid()));
//			}

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

	private DropDownChoice getResourceChoiceComponent() {
		OptionPanel panel = (OptionPanel) get("mainForm:option");
		OptionItem item = (OptionItem) panel.getBodyContainer().get("usersFilter");
		return (DropDownChoice) item.getBodyContainer().get("resourceChoice");
	}

	 private RepositoryObjectDataProvider getTableDataProvider() {
	        TablePanel tablePanel = getTable();
	        DataTable table = tablePanel.getDataTable();
	        return (RepositoryObjectDataProvider) table.getDataProvider();
	    }

	private void searchPerformed(AjaxRequestTarget target) {
		ObjectQuery query = createQuery();
		target.add(getFeedbackPanel());

		TablePanel panel = getTable();
		DataTable table = panel.getDataTable();
		getTableDataProvider().setQuery(query);
		table.setCurrentPage(0);
		target.add(panel);
	}

	private void clearButtonPerformed(AjaxRequestTarget target) {
		userFilterDto.reset();
		target.appendJavaScript("init()");
		target.add(get("mainForm:option"));
		searchPerformed(target);
	}

	private byte[] createReport() {
		UserFilterDto dto = userFilterDto.getObject();
		if (dto.getResource() == null) {
			getSession().error(getString("pageReports.message.noResourceSelected"));
			throw new RestartResponseException(PageReports.class);
		}


		JasperDesign design;
		JasperReport report = null;
		JasperPrint jasperPrint = null;

		byte[] generatedReport = null;
		Session session = null;
	/*	
		try {
            ServletContext servletContext = ((WebApplication) getApplication()).getServletContext();
			// Loading template

			design = JRXmlLoader.load(servletContext.getRealPath("/reports/reportUserAccountsMy.jrxml"));
			report = JasperCompileManager.compileReport(design);
			Map params = new HashMap();

			List userNames = new ArrayList();
			for (DebugObjectItem obj : listObject){
                userNames.add(obj.getName());
			}

			params.put("LOGO_PATH", servletContext.getRealPath("/reports/logo.jpg"));
			params.put("USER_NAME", userNames);
			params.put("RESOURCE_OID", dto.getResource().getOid());

			session = sessionFactory.openSession();
			session.beginTransaction();
			params.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, session);
			jasperPrint = JasperFillManager.fillReport(report, params);
			generatedReport = JasperExportManager.exportReportToPdf(jasperPrint);
		} catch (JRException ex) {
            getSession().error(getString("pageReports.message.jasperError") + " " + ex.getMessage());
			LoggingUtils.logException(LOGGER, "Couldn't create jasper report.", ex);
			throw new RestartResponseException(PageReports.class);
		} finally{
			session.close();
		}
		*/
/*	
		//User Report
		try {
            ServletContext servletContext = ((WebApplication) getApplication()).getServletContext();
			// Loading template

			design = JRXmlLoader.load(servletContext.getRealPath("/reports/reportUserList.jrxml"));
			report = JasperCompileManager.compileReport(design);
			
			Map params = new HashMap();
			params.put("LOGO_PATH", servletContext.getRealPath("/reports/logo.jpg"));
			
			session = sessionFactory.openSession();
			session.beginTransaction();
			
			params.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, session);
			jasperPrint = JasperFillManager.fillReport(report, params);
			generatedReport = JasperExportManager.exportReportToPdf(jasperPrint);
			
		} catch (JRException ex) {
            getSession().error(getString("pageReports.message.jasperError") + " " + ex.getMessage());
			LoggingUtils.logException(LOGGER, "Couldn't create jasper report.", ex);
			throw new RestartResponseException(PageReports.class);
		} finally{
			session.close();
		}
*/
/*

		// Audit Log report 
		
		// parameters 
		// LOGO_PATH = "/reports/logo.jpg"
		// DATE_FROM = Timestamp
		// DATE_TO = Timestamp
		
		// output data - audit logs
		// Timestamp, Initiator name, Event Type, Event Stage, Target type, Target name, Outcome, Message
		try {
            ServletContext servletContext = ((WebApplication) getApplication()).getServletContext();
			// Loading template
			design = JRXmlLoader.load(servletContext.getRealPath("/reports/reportAuditLogs.jrxml"));
			report = JasperCompileManager.compileReport(design);
			
			Timestamp dateTo = new Timestamp(new Date().getTime());
			Long time = (dateTo.getTime() - 3*3600000);
			Timestamp dateFrom = new Timestamp(time);
			
			Map params = new HashMap();
			params.put("LOGO_PATH", servletContext.getRealPath("/reports/logo.jpg"));
			params.put("DATE_FROM", dateFrom) ;
			params.put("DATE_TO", dateTo);

			session = sessionFactory.openSession();
			session.beginTransaction();
			
			params.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, session);
			jasperPrint = JasperFillManager.fillReport(report, params);
			generatedReport = JasperExportManager.exportReportToPdf(jasperPrint);
		} catch (JRException ex) {
            getSession().error(getString("pageReports.message.jasperError") + " " + ex.getMessage());
			LoggingUtils.logException(LOGGER, "Couldn't create jasper report.", ex);
			throw new RestartResponseException(PageReports.class);
		} finally{
			session.close();
		}
*/

		// Reconciliation Report
		
		// parameters 
		// LOGO_PATH = "/reports/logo.jpg"
		// RESOURCE_OID = String
		// CLASS = QName "http://midpoint.evolveum.com/xml/ns/public/resource/instance-2","AccountObjectClass"
		// INTENT = STRING "default"
				
		// output data - audit logs
		// Name, Situation, Owner, Situation description
		
		try {
			ServletContext servletContext = ((WebApplication) getApplication()).getServletContext();
			// Loading template

			design = JRXmlLoader.load(servletContext.getRealPath("/reports/reportReconciliation.jrxml"));
			report = JasperCompileManager.compileReport(design);
			
			Map params = new HashMap();
			params.put("LOGO_PATH",	servletContext.getRealPath("/reports/logo.jpg"));
			params.put("RESOURCE_OID", dto.getResource().getOid());
			QName objectClass = new QName("http://midpoint.evolveum.com/xml/ns/public/resource/instance-2","AccountObjectClass");
			params.put("CLASS", objectClass);
			params.put("INTENT", "default");
			
			session = sessionFactory.openSession();
			session.beginTransaction();

			params.put(
					JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION,
					session);
			jasperPrint = JasperFillManager.fillReport(report, params);
			generatedReport = JasperExportManager.exportReportToPdf(jasperPrint);

		} catch (JRException ex) {
			getSession().error(getString("pageReports.message.jasperError") + " " + ex.getMessage());
			LoggingUtils.logException(LOGGER, "Couldn't create jasper report.",	ex);
			throw new RestartResponseException(PageReports.class);
		} finally {
			session.close();
		}
	
		return generatedReport;

	}

	private List<DebugObjectItem> getObjects() {
        ObjectQuery query = getTableDataProvider().getQuery();

        ObjectQuery clonedQuery = null;
        if (query != null) {
            clonedQuery = new ObjectQuery();
            clonedQuery.setFilter(query.getFilter());
        }
        Class<? extends ObjectType> type = getTableDataProvider().getType();
        if (type == null) {
            type = ObjectType.class;
        }

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECT);
        List<DebugObjectItem> objects = new ArrayList<DebugObjectItem>();
        try {
			List<? extends PrismObject> resList = getModelService().searchObjects(type, clonedQuery,
					SelectorOptions.createCollection(new ItemPath(), GetOperationOptions.createRaw()),
					createSimpleTask(OPERATION_SEARCH_OBJECT), result);
            if (resList != null) {
                for (PrismObject prism : resList) {
                    objects.add(DebugObjectItem.createDebugObjectItem(prism));
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load objects", ex);
        } finally {
            result.recomputeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
        }

        
        return objects;
    }

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

}

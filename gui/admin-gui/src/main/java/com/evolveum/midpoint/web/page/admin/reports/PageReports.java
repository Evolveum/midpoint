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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageLogging;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AppenderConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggingDto;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author mserbak
 */
public class PageReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageReports.class);
    private static final String DOT_CLASS = PageReports.class.getName() + ".";
    private static final String OPERATION_CREATE_RESOURCE_LIST = DOT_CLASS + "createResourceList";
    private LoadableModel<UsersDto> userModel;

    public PageReports() {
    	userModel = new LoadableModel<UsersDto>(false) {

			@Override
			protected UsersDto load() {
				return new UsersDto();
			}
		};
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);
        
        OptionPanel option = new OptionPanel("option", createStringResource("pageReports.optionsTitle"),
        		getPage(), false);
        mainForm.add(option);
        
        OptionItem diagnostics = new OptionItem("objectsType", createStringResource("pageReports.objectsType"));
        option.getBodyContainer().add(diagnostics);
        
        DropDownChoice objectTypeChoice = new DropDownChoice("objectTypeChoice",
        		new AbstractReadOnlyModel<Class>() {

					@Override
					public Class getObject() {
						return UserType.class;
					}
				}, createObjectListModel(),new IChoiceRenderer<Class>() {

					@Override
					public Object getDisplayValue(Class clazz) {
						return clazz.getSimpleName();
					}

					@Override
					public String getIdValue(Class clazz, int index) {
						return Integer.toString(index);
					}
				}){

			@Override
			protected void onSelectionChanged(Object newSelection) {
				super.onSelectionChanged(newSelection);
			}
        };
        diagnostics.add(objectTypeChoice);
        
        OptionItem usersFilter = new OptionItem("usersFilter", createStringResource("pageReports.usersFilter"), true);
        option.getBodyContainer().add(usersFilter);
        initSearch(usersFilter);
		
		OptionContent content = new OptionContent("optionContent");
		mainForm.add(content);
		initTable(content);
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
    
    private IModel<List<ResourceType>> createResourceListModel() {
		return new AbstractReadOnlyModel<List<ResourceType>>() {

			@Override
			public List<ResourceType> getObject() {
				List<ResourceType> list = new ArrayList<ResourceType>();
				OperationResult result = new OperationResult(OPERATION_CREATE_RESOURCE_LIST);
				Task task = createSimpleTask(OPERATION_CREATE_RESOURCE_LIST);
				try {
					List<PrismObject<ResourceType>> resources = getModelService().searchObjects(ResourceType.class, new ObjectQuery(), task, result);
					list.addAll(new ArrayList(resources));
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
		TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(userModel,
				"searchText"));
		item.add(search);
		
		AjaxCheckBox activationEnabled = new AjaxCheckBox("activationEnabled", new Model<Boolean>(true)) {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				
			}
		};
		item.add(activationEnabled);
		
		DropDownChoice resourceChoice = new DropDownChoice("resourceChoice",
				new AbstractReadOnlyModel<PrismObject<ResourceType>>() {

					@Override
					public PrismObject<ResourceType> getObject() {
						return null;
					}
				}, createResourceListModel(), new IChoiceRenderer<PrismObject<ResourceType>>() {

					@Override
					public Object getDisplayValue(PrismObject<ResourceType> resource) {
						return WebMiscUtil.getName(resource);
					}

					@Override
					public String getIdValue(PrismObject<ResourceType> resource, int index) {
						return Integer.toString(index);
					}
		});
		item.add(resourceChoice);

		AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
				createStringResource("pageReports.button.clearButton")) {

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}

			@Override
			public void onSubmit(AjaxRequestTarget target, Form<?> form) {
				//clearButtonPerformed(target);
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
				//searchPerformed(target);
			}
		};
		item.add(searchButton);
	}
    
    private void initTable(OptionContent content) {
		List<IColumn<SelectableBean>> columns = initColumns();
		TablePanel table = new TablePanel<SelectableBean>("reportsTable", new ObjectDataProvider(
				PageReports.this, UserType.class), columns);
		table.setOutputMarkupId(true);
		content.getBodyContainer().add(table);
	}
    
    private List<IColumn<SelectableBean>> initColumns() {
    	List<IColumn<SelectableBean>> columns = new ArrayList<IColumn<SelectableBean>>();
    	
    	IColumn column = new CheckBoxHeaderColumn();
		columns.add(column);
		
		column = new PropertyColumn(createStringResource("pageReports.name"), "givenName",
				"value.givenName");
		
		column = new LinkColumn<SelectableBean>(createStringResource("pageReports.reportLink"), "name",
				"value.name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean> rowModel) {
				//TODO
			}
		};
		return columns;
    }
}

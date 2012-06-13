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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ButtonColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author lazyman
 * @author mserbak
 */

public class PageDebugList extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugList.class);
    private static final String DOT_CLASS = PageDebugList.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    private boolean deleteSelected;
    private IModel<ObjectTypes> choice = null;
    private ObjectType object = null;

    public PageDebugList() {
        initLayout();
    }

    private void initLayout() {
    	//confirm delete
    	add(new ConfirmationDialog("confirmDeletePopup", createStringResource("pageDebugList.dialog.title.confirmDelete"),
                createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                if(deleteSelected){
                	deleteSelected = false;
                	deleteSelectedConfirmedPerformed(target);
                } else {
                	deleteObjectConfirmedPerformed(target);
                }
                
            }
        });
    	
        //listed type
    	IModel<ObjectTypes> sessionChoice = null;
    	PrismObjectDefinition selectedCategory = (PrismObjectDefinition)getSession().getAttribute("category");
    	if(selectedCategory != null) {
    		sessionChoice = new Model<ObjectTypes>(ObjectTypes.getObjectTypeFromTypeQName(selectedCategory.getTypeName()));
    	} else {
    		sessionChoice = new Model<ObjectTypes>(ObjectTypes.SYSTEM_CONFIGURATION);
    	}
    	final IModel<ObjectTypes> choice = sessionChoice;

        List<IColumn<? extends ObjectType>> columns = new ArrayList<IColumn<? extends ObjectType>>();

        IColumn column = new CheckBoxHeaderColumn<ObjectType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<? extends ObjectType>>(createStringResource("pageDebugList.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                objectEditPerformed(target, object.getOid());
            }
        };
        columns.add(column);

        column = new ButtonColumn<SelectableBean<? extends ObjectType>>(createStringResource("pageDebugList.operation"),
                createStringResource("pageDebugList.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                deleteObjectPerformed(target, choice, object);
            }
        };
        columns.add(column);

        Form main = new Form("mainForm");
        add(main);
        
        OptionPanel option = new OptionPanel("option", createStringResource("pageDebugList.optionsTitle"));
        option.setOutputMarkupId(true);
        main.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageDebugList.search"));
        option.getBodyContainer().add(item);
        IModel<String> searchNameModel = initSearch(item, choice);

        item = new OptionItem("category", createStringResource("pageDebugList.selectType"));
        option.getBodyContainer().add(item);
        initCategory(item, choice, searchNameModel);

        OptionContent content = new OptionContent("optionContent");
        main.add(content);
        
        Class provider = selectedCategory == null ? SystemConfigurationType.class : selectedCategory.getCompileTimeClass();
        TablePanel table = new TablePanel("table", new RepositoryObjectDataProvider(PageDebugList.this,
        		provider), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);

        AjaxLinkButton button = new AjaxLinkButton("deleteSelected", ButtonType.NEGATIVE,
                createStringResource("pageDebugList.button.deleteSelected")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteSelectedPerformed(target, choice);
            }
        };
        main.add(button);
    }

    private IModel<String> initSearch(OptionItem item, final IModel<ObjectTypes> choice) {
        final IModel<String> model = new Model<String>();
        TextField<String> search = new TextField<String>("searchText", model);
        item.add(search);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                new StringResourceModel("pageDebugList.button.clear", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                model.setObject(null);
                target.add(PageDebugList.this.get("mainForm:option"));
                listObjectsPerformed(target, model.getObject(), choice.getObject());
            }
        };
        item.add(clearButton);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                new StringResourceModel("pageDebugList.button.search", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                listObjectsPerformed(target, model.getObject(), choice.getObject());
            }
        };
        item.add(searchButton);

        return model;
    }

    private void initCategory(OptionItem item, final IModel<ObjectTypes> choice, final IModel<String> searchNameModel) {
        IChoiceRenderer<ObjectTypes> renderer = new IChoiceRenderer<ObjectTypes>() {

            @Override
            public Object getDisplayValue(ObjectTypes object) {
                return new StringResourceModel(object.getLocalizationKey(),
                        (PageBase) PageDebugList.this, null).getString();
            }

            @Override
            public String getIdValue(ObjectTypes object, int index) {
                return object.getClassDefinition().getSimpleName();
            }
        };

        IModel<List<ObjectTypes>> choiceModel = createChoiceModel(renderer);
        final ListChoice listChoice = new ListChoice("choice", choice, choiceModel, renderer, choiceModel.getObject().size()) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
        listChoice.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(listChoice);
                listObjectsPerformed(target, searchNameModel.getObject(), choice.getObject());
            }
        });
        item.getBodyContainer().add(listChoice);
    }

    private IModel<List<ObjectTypes>> createChoiceModel(final IChoiceRenderer<ObjectTypes> renderer) {
        return new LoadableModel<List<ObjectTypes>>(false) {

            @Override
            protected List<ObjectTypes> load() {
                List<ObjectTypes> choices = new ArrayList<ObjectTypes>();
                Collections.addAll(choices, ObjectTypes.values());
                Collections.sort(choices, new Comparator<ObjectTypes>() {

                    @Override
                    public int compare(ObjectTypes o1, ObjectTypes o2) {
                        String str1 = (String) renderer.getDisplayValue(o1);
                        String str2 = (String) renderer.getDisplayValue(o2);
                        return String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
                    }
                });

                return choices;
            }
        };
    }

    private TablePanel getListTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("table");
    }

    private void listObjectsPerformed(AjaxRequestTarget target, String nameText, ObjectTypes selected) {
        RepositoryObjectDataProvider provider = getTableDataProvider();
        if (StringUtils.isNotEmpty(nameText)) {
            try {
                Document document = DOMUtil.getDocument();
                Element substring = QueryUtil.createSubstringFilter(document, null, ObjectType.F_NAME, nameText);
                QueryType query = new QueryType();
                query.setFilter(substring);
                provider.setQuery(query);
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Couldn't create substring filter", ex);
                error(getString("pageDebugList.message.queryException", ex.getMessage()));
                target.add(getFeedbackPanel());
            }
        } else {
            provider.setQuery(null);
        }

        if (selected != null) {
            provider.setType(selected.getClassDefinition());
        }
        TablePanel table = getListTable();
        target.add(table);
    }

    private void objectEditPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, oid);
        setResponsePage(PageDebugView.class, parameters);
    }

    private RepositoryObjectDataProvider getTableDataProvider() {
        TablePanel tablePanel = getListTable();
        DataTable table = tablePanel.getDataTable();
        return (RepositoryObjectDataProvider<ObjectType>) table.getDataProvider();
    }

    private List<ObjectType> getSelectedObjects() {
        RepositoryObjectDataProvider<ObjectType> provider = getTableDataProvider();

        List<ObjectType> selected = new ArrayList<ObjectType>();
        for (SelectableBean<ObjectType> row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row.getValue());
            }
        }

        return selected;
    }
    
    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
            	if(deleteSelected){
            		return createStringResource("pageDebugList.message.deleteSelectedConfirm", 
            				getSelectedObjects().size()).getString();
            	}
            	return createStringResource("pageDebugList.message.deleteObjectConfirm").getString();
            }
        };
    }
    
    private void deleteSelectedConfirmedPerformed(AjaxRequestTarget target){
    	MidPointApplication application = getMidpointApplication();
        RepositoryService repository = application.getRepository();
        ObjectTypes type = choice.getObject();

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
        for (ObjectType object : getSelectedObjects()) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
            try {
                repository.deleteObject(type.getClassDefinition(), object.getOid(), subResult);
                subResult.recordSuccess();
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't delete objects.", ex);
            }
        }
        result.recomputeStatus();

        showResult(result);
        target.add(getListTable());
        target.add(getFeedbackPanel());
    }
    
    private void deleteObjectConfirmedPerformed(AjaxRequestTarget target){
    	MidPointApplication application = getMidpointApplication();
        RepositoryService repository = application.getRepository();

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
        try {
            ObjectTypes type = choice.getObject();
            repository.deleteObject(type.getClassDefinition(), object.getOid(), result);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't delete object '" + object.getName() + "'.", ex);
        }

        showResult(result);
        target.add(getListTable());
        target.add(getFeedbackPanel());
    }
    
    private void deleteSelectedPerformed(AjaxRequestTarget target, IModel<ObjectTypes> choice) {
    	List<ObjectType> selected = getSelectedObjects();
        if (selected.isEmpty()) {
            warn(getString("pageDebugList.message.nothingSelected"));
            target.add(getFeedbackPanel());
            return;
        }
        
    	ModalWindow dialog = (ModalWindow) get("confirmDeletePopup");
    	deleteSelected = true;
    	this.choice = choice;
        dialog.show(target);
    }

    private void deleteObjectPerformed(AjaxRequestTarget target, IModel<ObjectTypes> choice, ObjectType object) {
    	ModalWindow dialog = (ModalWindow) get("confirmDeletePopup");
    	this.choice = choice;
    	this.object = object;
        dialog.show(target);
    }
}

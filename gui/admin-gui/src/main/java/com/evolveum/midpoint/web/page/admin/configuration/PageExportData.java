package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;


public class PageExportData extends PageAdminConfiguration {

    private static final String DOT_CLASS = PageExportData.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "searchObjects";
    private static final Trace LOGGER = TraceManager.getTrace(PageExportData.class);

    public PageExportData() {
        
        initLayout();
    }    
    private void initLayout() {
    	Form mainForm = new Form("mainForm");
    	add(mainForm);
    	
    	final IModel<ObjectTypes> choice = new Model<ObjectTypes>(ObjectTypes.SYSTEM_CONFIGURATION);
    	
    	WebMarkupContainer searchPanel = new WebMarkupContainer("searchPanel");
    	searchPanel.setOutputMarkupId(true);
    	mainForm.add(searchPanel);
    	
    	WebMarkupContainer searchContainer = new WebMarkupContainer("searchContainer");
    	searchPanel.add(searchContainer);
    	IModel<String> searchNameModel = initSearch(searchContainer, choice);
    	
    	WebMarkupContainer categoryContainer = new WebMarkupContainer("categoryContainer");
    	searchPanel.add(categoryContainer);
    	initCategory(categoryContainer, choice, searchNameModel);
    	
    	Label exportedData = new Label("exportedData", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageExportData.noProvidedData");
			}
		});
    	exportedData.setOutputMarkupId(true);
    	mainForm.add(exportedData);

    }
    
    private IModel<String> initSearch(WebMarkupContainer container, final IModel<ObjectTypes> choice) {
        final IModel<String> model = new Model<String>();
        TextField<String> searchText = new TextField<String>("searchText", model);
        container.add(searchText);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                new StringResourceModel("pageExportData.button.clear", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                model.setObject(null);
                target.appendJavaScript("init()");
                target.add(PageExportData.this.get("mainForm:searchPanel"));
                listObjectsPerformed(target, model.getObject(), choice.getObject());
            }
        };
        container.add(clearButton);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                new StringResourceModel("pageExportData.button.search", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
            	listObjectsPerformed(target, model.getObject(), choice.getObject());
            	target.add(getListChoice());
            }
        };
        container.add(searchButton);
        return model;
    }
    
    private void initCategory(WebMarkupContainer container, final IModel<ObjectTypes> choice, final IModel<String> searchNameModel) {
        IChoiceRenderer<ObjectTypes> renderer = new IChoiceRenderer<ObjectTypes>() {

            @Override
            public Object getDisplayValue(ObjectTypes object) {
                return new StringResourceModel(object.getLocalizationKey(),
                        (PageBase) PageExportData.this, null).getString();
            }

            @Override
            public String getIdValue(ObjectTypes object, int index) {
                return object.getClassDefinition().getSimpleName();
            }
        };

        IModel<List<ObjectTypes>> choiceModel = createChoiceModel(renderer);
        ListChoice listChoice = new ListChoice("choice", choice, choiceModel, renderer, choiceModel.getObject().size()) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
        listChoice.setOutputMarkupId(true);
//        listChoice.add(new OnChangeAjaxBehavior() {
//
//            @Override
//            protected void onUpdate(AjaxRequestTarget target) {
//                //target.add(listChoice);
//                listObjectsPerformed(target, searchNameModel.getObject(), choice.getObject());
//            }
//        });
        container.add(listChoice);
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
    
    private void listObjectsPerformed(AjaxRequestTarget target, String nameText, ObjectTypes selected) {
    	ObjectQuery query = null;
    	List objects = null;
        if (StringUtils.isNotEmpty(nameText)) {
            try {
                ObjectFilter substring = SubstringFilter.createSubstring(ObjectType.class, getPrismContext(), ObjectType.F_NAME, nameText);
                query = new ObjectQuery();
                query.setFilter(substring);
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Couldn't create substring filter", ex);
                error(getString("pageExportData.message.queryException", ex.getMessage()));
                target.add(getFeedbackPanel());
            }
        }
        
        if (selected != null) {
        	Task task = createSimpleTask(OPERATION_SEARCH_OBJECT);
        	OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECT);
        	try {
        		objects = getModelService().searchObjects(selected.getClassDefinition(), query, task, result);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't search objects", ex);
                error(getString("pageExportData.message.searchException", ex.getMessage()));
                target.add(getFeedbackPanel());
			}
        }
        if(objects != null) {
        	//create download link
        }
    }
    
    private ListChoice getListChoice() {
    	WebMarkupContainer panel = (WebMarkupContainer) get("mainForm:searchPanel:categoryContainer");
        return (ListChoice) panel.get("choice");
    } 

}

/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration;

import java.io.Serializable;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.DataLanguagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@PageDescriptor(url = "/admin/config/debug", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUG_URL,
                label = "PageDebugView.auth.debug.label", description = "PageDebugView.auth.debug.description")})
public class PageDebugView extends PageAdminConfiguration {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageDebugView.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "initObjectViewObject";
    private static final String OPERATION_SAVE_OBJECT = DOT_CLASS + "saveObject";
    private static final String ID_PLAIN_TEXTAREA = "plain-textarea";
    private static final String ID_VIEW_BUTTON_PANEL = "viewButtonPanel";
    
    private static final String ID_FORM = "mainForm";

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugView.class);

    public static final String PARAM_OBJECT_ID = "objectId";
    public static final String PARAM_OBJECT_TYPE = "objectType";
    private String dataLanguage = null;
    
    private IModel<ObjectViewDto<?>> objectViewDtoModel;
    private DebugViewOptions debugViewConfiguration = new DebugViewOptions();
   
    public PageDebugView() {

    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        if (dataLanguage == null) {
            dataLanguage = determineDataLanguage();
        }
        if (objectViewDtoModel == null){
            objectViewDtoModel = initObjectViewObject();
        }
        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        if (objectViewDtoModel == null){
            objectViewDtoModel = initObjectViewObject();
        }
        if (dataLanguage == null) {
            dataLanguage = determineDataLanguage();
        }

        return createStringResource("PageDebugView.title", getName());
    }
    
    private String getName() {
    	if (objectViewDtoModel == null || objectViewDtoModel.getObject() == null) {
    		return "";
    	}
    	
    	return objectViewDtoModel.getObject().getName();
    }

    private LoadableModel<ObjectViewDto<?>> initObjectViewObject() {
        return new LoadableModel<ObjectViewDto<?>>(false) {
           
        	private static final long serialVersionUID = 1L;

			@Override
            protected ObjectViewDto<?> load() {
                ObjectViewDto<?> objectViewDto = new ObjectViewDto<>();
                StringValue objectOid = getPageParameters().get(PARAM_OBJECT_ID);
                if (objectOid == null || StringUtils.isEmpty(objectOid.toString())) {
                    getSession().error(getString("pageDebugView.message.oidNotDefined"));
                    throw new RestartResponseException(PageDebugList.class);
                }

                Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
                OperationResult result = task.getResult(); //todo is this result != null ?
                try {
                    MidPointApplication application = PageDebugView.this.getMidpointApplication();

                    // FIXME: ObjectType.class will not work well here. We need more specific type.
                    //todo on page debug list create page params, put there oid and class for object type and send that to this page....read it here
                    Class<? extends ObjectType> type = getTypeFromParameters();

	                GetOperationOptionsBuilder optionsBuilder = getSchemaHelper().getOperationOptionsBuilder()
			                .raw()
			                .resolveNames()
			                .tolerateRawData();
                    // TODO make this configurable (or at least do not show campaign cases in production)
                    optionsBuilder = WebModelServiceUtils.addIncludeOptionsForExportOrView(optionsBuilder, type);
                    PrismObject<? extends ObjectType> object = getModelService().getObject(type, objectOid.toString(), optionsBuilder.build(), task, result);

                    PrismContext context = application.getPrismContext();

                    String lex = context.serializerFor(dataLanguage).serialize(object);
                    objectViewDto = new ObjectViewDto<>(object.getOid(), WebComponentUtil.getName(object), object, lex);

                    result.recomputeStatus();
                } catch (Exception ex) {
                    result.recordFatalError("Couldn't load object.", ex);
                }

                showResult(result, false);

                if (!WebComponentUtil.isSuccessOrHandledErrorOrWarning(result)) {
                    showResult(result, false);
                    throw new RestartResponseException(PageDebugList.class);
                }
                return objectViewDto;
            }
        };
    }
    
    private Class<? extends ObjectType> getTypeFromParameters() {
    	StringValue objectType = getPageParameters().get(PARAM_OBJECT_TYPE);
        if (objectType != null && StringUtils.isNotBlank(objectType.toString())) {
            return getPrismContext().getSchemaRegistry().determineCompileTimeClass(new QName(SchemaConstantsGenerated.NS_COMMON, objectType.toString()));
        }
        
        return ObjectType.class;
    }

    private void initLayout() {
    	final Form<?> mainForm = new Form<>(ID_FORM);
        add(mainForm);
        mainForm.add(createOptionCheckbox(DebugViewOptions.ID_ENCRYPT, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_ENCRYPT), "pageDebugView.encrypt", "pageDebugView.encrypt.help"));
        mainForm.add(createOptionCheckbox(DebugViewOptions.ID_VALIDATE_SCHEMA, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_VALIDATE_SCHEMA), "pageDebugView.validateSchema", "pageDebugView.validateSchema.help"));
        mainForm.add(createOptionCheckbox(DebugViewOptions.ID_SAVE_AS_RAW, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_SAVE_AS_RAW), "pageDebugView.saveAsRaw", "pageDebugView.saveAsRaw.help"));
        mainForm.add(createOptionCheckbox(DebugViewOptions.ID_REEVALUATE_SEARCH_FILTERS, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_REEVALUATE_SEARCH_FILTERS), "pageDebugView.reevaluateSearchFilters", "pageDebugView.reevaluateSearchFilters.help"));
        mainForm.add(createOptionCheckbox(DebugViewOptions.ID_SWITCH_TO_PLAINTEXT, new PropertyModel<>(debugViewConfiguration, DebugViewOptions.ID_SWITCH_TO_PLAINTEXT), "pageDebugView.switchToPlainText", "pageDebugView.switchToPlainText.help"));

        TextArea<String> plainTextarea = new TextArea<>(ID_PLAIN_TEXTAREA, new PropertyModel<>(objectViewDtoModel, ObjectViewDto.F_XML));
        plainTextarea.add(new VisibleBehaviour(() -> isTrue(DebugViewOptions.ID_SWITCH_TO_PLAINTEXT)));
        mainForm.add(plainTextarea);

        initAceEditor(mainForm);

        initButtons(mainForm);
        initViewButton(mainForm);

    }
    
    private CheckBoxPanel createOptionCheckbox(String id, IModel<Boolean> model, String labelKey, String helpKey) {
    	
      	return new CheckBoxPanel(id, model, null, createStringResource(labelKey), createStringResource(helpKey)) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onUpdate(AjaxRequestTarget target) {
				if (DebugViewOptions.ID_SWITCH_TO_PLAINTEXT.equals(id)) {
					target.add(getMainForm());
				}
			}
        };
    }
    
    private boolean isTrue(String panelId) {
    	CheckBoxPanel panel = (CheckBoxPanel) get(createComponentPath(ID_FORM, panelId));
    	if (panel == null) {
    		LOGGER.error("Cannot find panel: {}", panelId);
    		 return false;
    	}
    	
    	return panel.getValue();
    }
    private Form<?> getMainForm() {
    	return (Form<?>) get(ID_FORM);
    }

    private void initAceEditor(Form<?> mainForm){
        AceEditor editor = new AceEditor("aceEditor", new PropertyModel<>(objectViewDtoModel, ObjectViewDto.F_XML));
        editor.setModeForDataLanguage(dataLanguage);
        editor.add(new VisibleBehaviour(() -> !isTrue(DebugViewOptions.ID_SWITCH_TO_PLAINTEXT)));
        mainForm.add(editor);
    }

    private void initViewButton(Form mainForm) {
        DataLanguagePanel<Objectable> dataLanguagePanel =
                new DataLanguagePanel<Objectable>(ID_VIEW_BUTTON_PANEL, dataLanguage, Objectable.class, PageDebugView.this) {
                    private static final long serialVersionUID = 1L;

	                @Override
	                protected void onLanguageSwitched(AjaxRequestTarget target, int updatedIndex, String updatedLanguage,
			                String objectString) {
		                objectViewDtoModel.getObject().setXml(objectString);
		                dataLanguage = updatedLanguage;
		                target.add(mainForm);
	                }
	                @Override
	                protected String getObjectStringRepresentation() {
		                return objectViewDtoModel.getObject().getXml();
	                }
	                @Override
	                protected boolean isValidateSchema() {
		                return isTrue(DebugViewOptions.ID_VALIDATE_SCHEMA);
	                }
                };
        dataLanguagePanel.setOutputMarkupId(true);
        mainForm.add(dataLanguagePanel);
    }

    private void initButtons(final Form<?> mainForm) {
        AjaxSubmitButton saveButton = new AjaxSubmitButton("saveButton",
                createStringResource("pageDebugView.button.save")) {
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(saveButton);

        AjaxButton backButton = new AjaxButton("backButton",
                createStringResource("pageDebugView.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }
        };
        mainForm.add(backButton);
    }
    
    public void savePerformed(AjaxRequestTarget target) {
        if (StringUtils.isEmpty(objectViewDtoModel.getObject().getXml())) {
            error(getString("pageDebugView.message.cantSaveEmpty"));
            target.add(getFeedbackPanel());
            return;
        }

        Task task = createSimpleTask(OPERATION_SAVE_OBJECT);
        OperationResult result = task.getResult();
        try {

            PrismObject<? extends ObjectType> oldObject = objectViewDtoModel.getObject().getObject();
            oldObject.revive(getPrismContext());

            Holder<? extends ObjectType> objectHolder = new Holder<>(null);
            validateObject(result, (Holder) objectHolder);

			if (result.isAcceptable()) {
                PrismObject<? extends ObjectType> newObject = objectHolder.getValue().asPrismObject();

				ObjectDelta<? extends ObjectType> delta = oldObject.diff((PrismObject) newObject, true, true);

                if (delta.getPrismContext() == null) {
                	LOGGER.warn("No prism context in delta {} after diff, adding it", delta);
                	delta.revive(getPrismContext());
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Delta to be applied:\n{}", delta.debugDump());
                }
                
                //quick fix for now (MID-1910), maybe it should be somewhere in objectViewModel..
//                if (isReport(oldObject)){
//                	ReportTypeUtil.applyConfigurationDefinition((PrismObject)newObject, delta, getPrismContext());
//                }

                Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(delta);
                ModelExecuteOptions options = new ModelExecuteOptions();
                if (isTrue(DebugViewOptions.ID_SAVE_AS_RAW)) {
                    options.setRaw(true);
                }
                if (isTrue(DebugViewOptions.ID_REEVALUATE_SEARCH_FILTERS)) {
                    options.setReevaluateSearchFilters(true);
                }
                if(!isTrue(DebugViewOptions.ID_ENCRYPT)) {
                	options.setNoCrypt(true);
                }

                getModelService().executeChanges(deltas, options, task, result);

                result.computeStatus();
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save object.", ex);
        }

        if (result.isError()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResult(result);
            //to handle returning back to list objects page instead of edit object page
            if (getBreadcrumbs().size() >= 3){
                redirectBack(3);
            } else {
                redirectBack();
            }
        }
    }

    private void validateObject(OperationResult result, Holder<Objectable> objectHolder) {
	    parseObject(objectViewDtoModel.getObject().getXml(), (Holder<Objectable>) objectHolder, dataLanguage, isTrue(DebugViewOptions.ID_VALIDATE_SCHEMA), false, Objectable.class, result);
    }
    
    
 class DebugViewOptions implements Serializable {
    	
	 	private static final long serialVersionUID = 1L;
	 	
		private final static String ID_ENCRYPT = "encrypt";
    	private final static String ID_SAVE_AS_RAW = "saveAsRaw";
    	private final static String ID_REEVALUATE_SEARCH_FILTERS = "reevaluateSearchFilters";
    	private final static String ID_VALIDATE_SCHEMA = "validateSchema";
    	private final static String ID_SWITCH_TO_PLAINTEXT = "switchToPlainText";
    	
    	private final boolean encrypt=true;
        private final boolean saveAsRaw=true;
        private final boolean reevaluateSearchFilters = false;
        private final boolean validateSchema = false;
        private final boolean switchToPlainText = false;
    	
    }

}

package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.ConfigurationStorage;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PageDebugView extends PageAdminConfiguration {

    private static final String DOT_CLASS = PageDebugView.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_SAVE_OBJECT = DOT_CLASS + "saveObject";
    
    private static final Trace LOGGER = TraceManager.getTrace(PageDebugView.class);

    public static final String PARAM_OBJECT_ID = "objectId";
    public static final String PARAM_OBJECT_TYPE = "objectType";
    private IModel<ObjectViewDto> model;
    private AceEditor<String> editor;
    private final IModel<Boolean> encrypt = new Model<Boolean>(true);

    public PageDebugView() {
        model = new LoadableModel<ObjectViewDto>(false) {

            @Override
            protected ObjectViewDto load() {
                return loadObject();
            }
        };
        initLayout();
    }

    private ObjectViewDto loadObject() {
        StringValue objectOid = getPageParameters().get(PARAM_OBJECT_ID);
        if (objectOid == null || StringUtils.isEmpty(objectOid.toString())) {
            getSession().error(getString("pageDebugView.message.oidNotDefined"));
            throw new RestartResponseException(PageDebugList.class);
        }

        Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
        OperationResult result = task.getResult(); //todo is this result != null ?
        ObjectViewDto dto = null;
        try {
            MidPointApplication application = PageDebugView.this.getMidpointApplication();
//            ModelService modelService = application.getModel();
            
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
			// FIXME: ObjectType.class will not work well here. We need more specific type.
            //todo on page debug list create page params, put there oid and class for object type and send that to this page....read it here
            Class type = ObjectType.class;
            StringValue objectType = getPageParameters().get(PARAM_OBJECT_TYPE);
            if (objectType != null && StringUtils.isNotBlank(objectType.toString())){
            	type = getPrismContext().getSchemaRegistry().determineCompileTimeClass(new QName(SchemaConstantsGenerated.NS_COMMON, objectType.toString()));
            }
            PrismObject<ObjectType> object = getModelService().getObject(type, objectOid.toString(), options, task, result);
            
            PrismContext context = application.getPrismContext();
            String xml = context.getPrismDomProcessor().serializeObjectToString(object);
            dto = new ObjectViewDto(object.getOid(), WebMiscUtil.getName(object), object, xml);

            //save object type category to session storage, used by back button
            ConfigurationStorage storage = getSessionStorage().getConfiguration();
            storage.setDebugListCategory(ObjectTypes.getObjectType(object.getCompileTimeClass()));

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load object.", ex);
        }

        if (dto == null) {
            showResultInSession(result);
            throw new RestartResponseException(PageDebugList.class);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        return dto;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        mainForm.add(new Label("oid", new PropertyModel(model, ObjectViewDto.F_OID)));
        mainForm.add(new Label("name", new PropertyModel(model, ObjectViewDto.F_NAME)));
        final IModel<Boolean> editable = new Model<Boolean>(false);
        
        mainForm.add(new AjaxCheckBox("encrypt", encrypt) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
			}
        });
        mainForm.add(new AjaxCheckBox("edit", editable) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                editPerformed(target, editable.getObject());
            }
        });
        editor = new AceEditor<String>("aceEditor", new PropertyModel<String>(model, ObjectViewDto.F_XML));
        editor.setReadonly(!editable.getObject());
        mainForm.add(editor);

        initButtons(mainForm);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton", ButtonType.POSITIVE,
                createStringResource("pageDebugView.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(saveButton);

        AjaxLinkButton backButton = new AjaxLinkButton("backButton",
                createStringResource("pageDebugView.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //target.appendJavaScript("history.go(-1)");
                //todo wtf????
                Page requestPage = (Page)getSession().getAttribute("requestPage");
                
                if(requestPage != null){
                	setResponsePage(requestPage);
                	getSession().setAttribute("requestPage", null);
                } else {
                	setResponsePage(PageDebugList.class);
                } 
            }
        };
        mainForm.add(backButton);
    }

    public void editPerformed(AjaxRequestTarget target, boolean editable) {
        editor.setReadonly(!editable);
        target.appendJavaScript(editor.createJavascriptEditableRefresh());
    }

    public void savePerformed(AjaxRequestTarget target) {
        ObjectViewDto dto = model.getObject();
        if (StringUtils.isEmpty(dto.getXml())) {
            error(getString("pageDebugView.message.cantSaveEmpty"));
            target.add(getFeedbackPanel());
            return;
        }

        Task task = createSimpleTask(OPERATION_SAVE_OBJECT);
        OperationResult result = task.getResult();
        try {

            PrismObject<ObjectType> oldObject = dto.getObject();
            if (oldObject.getPrismContext() == null) {
            	LOGGER.warn("No prism context in old object {}, adding it", oldObject);
            	oldObject.setPrismContext(getPrismContext());
            }
            
            final Holder<PrismObject<ObjectType>> objectHolder = new Holder<PrismObject<ObjectType>>(null);
            EventHandler handler = new EventHandler() {
				@Override
				public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
					return EventResult.cont();
				}				
				@Override
				public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
						OperationResult objectResult) {
					objectHolder.setValue((PrismObject<ObjectType>) object);
					return EventResult.cont();
				}
				
				@Override
				public void handleGlobalError(OperationResult currentResult) {
				}
			};
			Validator validator = new Validator(getPrismContext(), handler );
            validator.setVerbose(true);
            validator.setValidateSchema(true);
            String newXmlString = editor.getModel().getObject();
            validator.validateObject(newXmlString, result);
            
            result.computeStatus();
            
            if (result.isAcceptable()) {
                PrismObject<ObjectType> newObject = objectHolder.getValue();
                
                ObjectDelta<ObjectType> delta = oldObject.diff(newObject, true, true);
                
                if (delta.getPrismContext() == null) {
                	LOGGER.warn("No prism context in delta {} after diff, adding it", delta);
                	delta.setPrismContext(getPrismContext());
                }
                
                Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(delta);
                ModelExecuteOptions options = ModelExecuteOptions.createRaw();
                
                if(!encrypt.getObject()) {
                	options.setNoCrypt(true);
                }  

                getModelService().executeChanges(deltas, options, task, result);
                
                result.computeStatus();            	
            }
            
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save object.", ex);
        }

        if (!result.isSuccess() && !result.isHandledError()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResultInSession(result);
            setResponsePage(PageDebugList.class);
        }
    }
}

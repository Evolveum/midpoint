package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.web.page.admin.dto.DtoUtils;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

public class PageDebugView extends PageAdminConfiguration {

    private static final String DOT_CLASS = PageDebugView.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_SAVE_OBJECT = DOT_CLASS + "saveObject";

    public static final String PARAM_OBJECT_ID = "objectId";
    private IModel<ObjectViewDto> model;
    private AceEditor<String> editor;

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

        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT);
        ObjectViewDto dto = null;
        try {
            MidPointApplication application = PageDebugView.this.getMidpointApplication();
            RepositoryService repository = application.getRepository();

            PrismObject<ObjectType> object = repository.getObject(ObjectType.class, objectOid.toString(), result);
            PrismContext context = application.getPrismContext();
            String xml = context.getPrismDomProcessor().serializeObjectToString(object);

            dto = new ObjectViewDto(object.getOid(), DtoUtils.getName(object), object, xml);

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

        mainForm.add(new Label("oid", new PropertyModel<Object>(model, "oid")));
        mainForm.add(new Label("name", new PropertyModel<Object>(model, "name")));
        final IModel<Boolean> editable = new Model<Boolean>(false);
        mainForm.add(new AjaxCheckBox("edit", editable) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                editPerformed(target, editable.getObject());
            }
        });
        editor = new AceEditor<String>("aceEditor", new PropertyModel<String>(model, "xml"));
        editor.setReadonly(!editable.getObject());
        mainForm.add(editor);

        initButtons(mainForm);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
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
                target.appendJavaScript("history.go(-1)");

                //setResponsePage(PageDebugList.class);
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

        OperationResult result = new OperationResult(OPERATION_SAVE_OBJECT);
        try {
            PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();

            PrismObject<ObjectType> oldObject = dto.getObject();
            PrismObject<ObjectType> newObject = domProcessor.parseObject(editor.getModel().getObject());
            ObjectDelta<ObjectType> delta = oldObject.diff(newObject);

            MidPointApplication application = PageDebugView.this.getMidpointApplication();
            RepositoryService repository = application.getRepository();

            repository.modifyObject(ObjectType.class, delta.getOid(), delta.getModifications(), result);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save object.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResultInSession(result);
            setResponsePage(PageDebugList.class);
        }
    }
}

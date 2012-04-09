package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

public class PageDebugView extends PageAdminConfiguration {

    public static final String PARAM_OBJECT_ID = "objectId";
    private IModel<ObjectViewDto> model;

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
        if (objectOid == null) {
            error("some errorrrororor");//todo change
        }

        ObjectViewDto dto = null;
        try {
            MidPointApplication application = PageDebugView.this.getMidpointApplication();
            ModelService model = application.getModel();

            OperationResult result = new OperationResult("aaaaaaaaaaaaaaaa");
            PrismObject<ObjectType> object = model.getObject(ObjectType.class, objectOid.toString(),
                    null, result);

            PrismContext context = application.getPrismContext();
            String xml = context.getPrismDomProcessor().serializeObjectToString(object);
            String name = null;
            PrismProperty nameProperty = object.findProperty(ObjectType.F_NAME);
            if (nameProperty != null) {
                name = (String) nameProperty.getRealValue();
            }

            dto = new ObjectViewDto(object.getOid(), name, xml);
        } catch (Exception ex) {
            ex.printStackTrace();
            //todo implement and fix result
        }

        if (dto == null) {
            dto = new ObjectViewDto();
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

        AceEditor<String> editor = new AceEditor<String>("aceEditor", new PropertyModel<String>(model, "xml"));
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
                onSaveError(target, form);
            }
        };
        mainForm.add(saveButton);

        AjaxLinkButton backButton = new AjaxLinkButton("backButton",
                createStringResource("pageDebugView.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(PageDebugList.class);
            }
        };
        mainForm.add(backButton);
    }

    public void editPerformed(AjaxRequestTarget target, boolean editable) {
        //todo implement
    }

    public void onSaveError(AjaxRequestTarget target, Form form) {
        //todo implement
    }

    public void savePerformed(AjaxRequestTarget target) {
        //todo implement
    }
}

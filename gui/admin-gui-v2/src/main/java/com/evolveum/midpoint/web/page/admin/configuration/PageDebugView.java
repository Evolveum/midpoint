package com.evolveum.midpoint.web.page.admin.configuration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;

public class PageDebugView extends PageAdminConfiguration  {
private LoadableModel<ImportOptionsType> model;
    
    public PageDebugView() {
        initLayout();
    }
    
    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        AceEditor<String> editor = new AceEditor<String>("aceEditor", new Model<String>("aaa"));
        mainForm.add(editor);

        initButtons(mainForm);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
                createStringResource("pageDebugView.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                //todo implement

                model.reset();
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                //todo implement
            }
        };
        mainForm.add(saveButton);
    }
}

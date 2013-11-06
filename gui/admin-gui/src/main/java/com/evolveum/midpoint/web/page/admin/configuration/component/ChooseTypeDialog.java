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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.StringResourceModel;

/**
 *  @author shood
 * */
public class ChooseTypeDialog extends ModalWindow{

    private static final Trace LOGGER = TraceManager.getTrace(ChooseTypeDialog.class);
    Class objectType;
    private boolean initialized;

    public ChooseTypeDialog(String id, Class type){
        super(id);

        objectType = type;

        //TODO - when setting title, use type name to tell user, what he is choosing
        setTitle(createStringResource("chooseTypeDialog.title"));
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(ChooseTypeDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(500);
        setInitialHeight(500);
        setWidthUnit("px");

        //WebMarkupContainer content = new WebMarkupContainer(getContentId());
        //setContent(content);
    }

    @Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if(initialized)
            return;

        //initLayout((WebMarkupContainer) get(getContentId()));
        initLayout();
        initialized = true;
    }

    //public void initLayout(WebMarkupContainer content){
    public void initLayout(){
        Form mainForm = new Form("mainForm");
        add(mainForm);

        //TODO - add table here

        AjaxLinkButton cancelButton = new AjaxLinkButton("cancelButton",
                createStringResource("chooseTypeDialog.button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                cancelPerformed(ajaxRequestTarget);
            }
        };
        mainForm.add(cancelButton);
    }


    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }
}

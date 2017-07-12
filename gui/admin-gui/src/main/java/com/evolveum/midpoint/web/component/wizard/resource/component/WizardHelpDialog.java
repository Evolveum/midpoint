/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.wizard.resource.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.wizard.resource.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.StringResourceModel;

/**
 *  @author shood
 * */
public class WizardHelpDialog extends ModalWindow{

    private static final String ID_HELP = "helpLabel";
    private static final String ID_BUTTON_OK = "okButton";

    private boolean initialized;
    private IWizardStep step;

    public WizardHelpDialog(String id, final IWizardStep step){
        super(id);

        this.step = step;

        setOutputMarkupId(true);
        setTitle(createStringResource("WizardHelpDialog.label"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(WizardHelpDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(450);
        setInitialHeight(500);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        content.setOutputMarkupId(true);
        setContent(content);
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return PageBase.createStringResourceStatic(this, resourceKey, objects);
//        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    public void updateModal(AjaxRequestTarget target, IWizardStep step){
        this.step = step;

        if(target != null){
            target.add(getContent());
        }
    }

    @Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if(initialized){
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    public void initLayout(WebMarkupContainer content){
        Label helpLabel = new Label(ID_HELP, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getString(determineHelpKey());
            }
        });
        helpLabel.setEscapeModelStrings(false);
        content.add(helpLabel);

        AjaxLink ok = new AjaxLink(ID_BUTTON_OK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePerformed(target);
            }
        };
        content.add(ok);
    }

    private String determineHelpKey(){
        if(step != null){
            if(step instanceof NameStep){
                return "ResourceWizard.help.nameStep";
            } else if(step instanceof ConfigurationStep){
                return "ResourceWizard.help.configurationStep";
            } else if(step instanceof SchemaStep){
                return "ResourceWizard.help.schemaStep";
            } else if(step instanceof SchemaHandlingStep){
                return "ResourceWizard.help.schemaHandlingStep";
            } else if(step instanceof CapabilityStep){
                return "ResourceWizard.help.capabilityStep";
            } else if(step instanceof SynchronizationStep){
                return "ResourceWizard.help.synchronizationStep";
            }
        }

        return null;
    }

    private void closePerformed(AjaxRequestTarget target){
        close(target);
    }
}

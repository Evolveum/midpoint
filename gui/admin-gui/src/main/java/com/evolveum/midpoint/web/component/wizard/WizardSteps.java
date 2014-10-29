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

package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.component.WizardHelpDialog;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 * @author lazyman
 */
public class WizardSteps extends SimplePanel<List<WizardStepDto>> {

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTON_HELP = "help";
    private static final String ID_HELP_MODAL = "helpModal";

    public WizardSteps(String id, IModel<List<WizardStepDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        ListView<WizardStepDto> link = new ListView<WizardStepDto>(ID_LINK, getModel()) {

            @Override
            protected void populateItem(ListItem<WizardStepDto> item) {
                final WizardStepDto dto = item.getModelObject();
                Label label = new Label(ID_LABEL, createLabelModel(dto.getName()));
                item.add(label);

                item.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isEnabled() {
//                        return dto.isEnabled();
                        return true;
                    }

                    @Override
                    public boolean isVisible() {
                        return dto.isVisible();
                    }
                });


                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return dto.isActive() ? "current" : null;
                    }
                }));

                item.add(new AjaxEventBehavior("onclick") {

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        changeStepPerformed(target, dto);
                    }
                });
            }
        };
        add(link);

        AjaxLink help = new AjaxLink(ID_BUTTON_HELP) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showHelpPerformed(target);
            }
        };
        add(help);

        initModals();
    }

    private void initModals(){
        ModalWindow helpWindow = new WizardHelpDialog(ID_HELP_MODAL, getActiveStep());
        add(helpWindow);
    }

    public void updateModal(){
        WizardHelpDialog window = (WizardHelpDialog)get(ID_HELP_MODAL);

        if(window != null){
            AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class);
            window.updateModal(target ,getActiveStep());
        }
    }

    private IModel<String> createLabelModel(final String key) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return new StringResourceModel(key, getPage(), null, key).getString();
            }
        };
    }

    public void changeStepPerformed(AjaxRequestTarget target, WizardStepDto dto){}

    private void showHelpPerformed(AjaxRequestTarget target){
        WizardHelpDialog window = (WizardHelpDialog)get(ID_HELP_MODAL);
        window.show(target);
    }

    public IWizardStep getActiveStep(){
        return null;
    }
}

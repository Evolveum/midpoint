/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.component.WizardHelpDialog;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lazyman
 */
public class WizardSteps extends BasePanel<List<WizardStepDto>> {

    private static final String ID_LINK_REPEATER = "linkRepeater";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTON_HELP = "help";
    private static final String ID_HELP_MODAL = "helpModal";

    public WizardSteps(String id, IModel<List<WizardStepDto>> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
        ListView<WizardStepDto> linkContainer = new ListView<WizardStepDto>(ID_LINK_REPEATER, getModel()) {

            @Override
            protected void populateItem(ListItem<WizardStepDto> item) {
                final WizardStepDto dto = item.getModelObject();
                item.setRenderBodyOnly(true);

                AjaxSubmitLink button = new AjaxSubmitLink(ID_LINK) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        changeStepPerformed(target, dto);
                    }

					@Override
					protected void onError(AjaxRequestTarget target) {
						target.add(getPageBase().getFeedbackPanel());
					}
				};
                item.add(button);

                button.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isEnabled() {
						final boolean enabled = ((PageResourceWizard) getPageBase()).isCurrentStepComplete();
//						System.out.println(dto.getName() + " enabled = " + enabled);
						return enabled;
                    }

                    @Override
                    public boolean isVisible() {
                        return dto.isVisible();
                    }
                });

                button.add(AttributeModifier.replace("class", new IModel<String>() {
                    @Override
                    public String getObject() {
						return dto.getWizardStep() == getActiveStep() ? "current" : null;
                    }
                }));

				button.add(AttributeModifier.replace("style", new IModel<String>() {
					@Override
					public String getObject() {
						final boolean enabled = ((PageResourceWizard) getPageBase()).isCurrentStepComplete();
//						System.out.println(dto.getName() + " enabled2 = " + enabled);
						return enabled ? null : "color: #FFF;";		// TODO respect color scheme (and find a better style for disabled anyway...)
					}
				}));

                Label label = new Label(ID_LABEL, createLabelModel(dto.getName()));
                button.add(label);
            }
        };
        add(linkContainer);

        AjaxLink<Void> help = new AjaxLink<Void>(ID_BUTTON_HELP) {
        	private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                showHelpPerformed(target);
                updateModal();
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

        if(window != null && getRequestCycle().find(AjaxRequestTarget.class).isPresent()){
        	AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class).get();
            window.updateModal(target ,getActiveStep());
        }
    }

    private IModel<String> createLabelModel(final String key) {
        return new IModel<String>() {

            @Override
            public String getObject() {
            	return PageBase.createStringResourceStatic(getPage(), key).getString();
//                return new StringResourceModel(key, getPage(), null, key).getString();
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

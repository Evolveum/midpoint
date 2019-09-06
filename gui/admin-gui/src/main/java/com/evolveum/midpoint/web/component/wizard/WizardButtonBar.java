/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class WizardButtonBar extends Panel implements IDefaultButtonProvider {

	private static final String ID_PREVIOUS = "previous";
	private static final String ID_NEXT = "next";
	private static final String ID_LAST = "last";
	private static final String ID_FINISH = "finish";
	private static final String ID_CANCEL = "cancel";
	private static final String ID_VALIDATE = "validate";
	private static final String ID_SAVE = "save";
	private static final String ID_VISUALIZE = "visualize";
	private static final String ID_VISUALIZE_LABEL = "visualizeLabel";

    public WizardButtonBar(String id, final Wizard wizard) {
        super(id);

		VisibleEnableBehaviour showInFullWizardMode = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				PageResourceWizard wizardPage = (PageResourceWizard) getPage();
				return !wizardPage.isConfigurationOnly() && !wizardPage.isReadOnly();
			}
		};
		boolean moreSteps = wizard.hasMoreThanOneStep();

		PreviousButton previous = new PreviousButton(ID_PREVIOUS, wizard) {
			@Override
			public void onClick() {
				IWizardModel wizardModel = getWizardModel();
				IWizardStep step = wizardModel.getActiveStep();
				step.applyState();
				if (step.isComplete()) {
					wizardModel.previous();
				} else {
					couldntSave();
				}
			}
		};
		previous.setVisible(moreSteps);
		add(previous);

		final NextButton next = new NextButton(ID_NEXT, wizard) {
			@Override
			public void onClick() {
				IWizardModel wizardModel = getWizardModel();
				IWizardStep step = wizardModel.getActiveStep();
				step.applyState();
				if (step.isComplete()) {
					wizardModel.next();
				} else {
					couldntSave();
				}
			}
		};
		next.setVisible(moreSteps);
		add(next);

		final LastButton lastButton = new LastButton(ID_LAST, wizard);
		lastButton.setVisible(false);		// not used at all
		add(lastButton);

        add(new CancelButton(ID_CANCEL, wizard));
        add(new FinishButton(ID_FINISH, wizard){

			@Override
			public void onClick()
			{
				IWizardModel wizardModel = getWizardModel();
				IWizardStep step = wizardModel.getActiveStep();
				step.applyState();
				if (step.isComplete()) {
					getWizardModel().finish();
				} else {
					couldntSave();
				}
			}
			/*
             *   Finish button is always enabled, so user don't have to
             *   click through every step of wizard every time it is used
             */
            @Override
            public boolean isEnabled() {
				final IWizardStep activeStep = wizard.getModelObject().getActiveStep();
				return activeStep == null || activeStep.isComplete();
            }
        });

		AjaxSubmitButton validate = new AjaxSubmitButton(ID_VALIDATE) {
			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				Session.get().getFeedbackMessages().clear();			// TODO - ok?
				((PageResourceWizard) getPage()).refreshIssues(target);
			}
			@Override
			protected void onError(AjaxRequestTarget target) {
				target.add(((PageBase) getPage()).getFeedbackPanel());
			}
		};
		validate.add(showInFullWizardMode);
		add(validate);

		final AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE) {
			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				IWizardStep activeStep = wizard.getModelObject().getActiveStep();
				if (activeStep != null) {
					activeStep.applyState();
					target.add(getPage());
				}
			}

			@Override
			protected void onError(AjaxRequestTarget target) {
				target.add(((PageBase) getPage()).getFeedbackPanel());
			}
		};
		save.add(showInFullWizardMode);
		add(save);

		final AjaxSubmitButton visualize = new AjaxSubmitButton(ID_VISUALIZE) {
			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				IWizardStep activeStep = wizard.getModelObject().getActiveStep();
				PageResourceWizard wizardPage = (PageResourceWizard) getPage();
				if (!wizardPage.isReadOnly()) {
					if (activeStep != null) {
						activeStep.applyState();
						if (!activeStep.isComplete()) {
							return;
						}
					}
				}
				((PageResourceWizard) getPage()).visualize(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target) {
				target.add(((PageBase) getPage()).getFeedbackPanel());
			}
		};
		visualize.setVisible(moreSteps);
		add(visualize);

		Label visualizeLabel = new Label(ID_VISUALIZE_LABEL, new IModel<String>() {
			@Override
			public String getObject() {
				PageResourceWizard wizardPage = (PageResourceWizard) getPage();
				return wizardPage.isReadOnly() ? getString("ResourceWizard.visualize") : getString("ResourceWizard.saveAndVisualize");
			}
		});
		visualize.add(visualizeLabel);
	}

	private void couldntSave() {
		// we should't come here but ... might happen if 'issues' window is not up-to-date
		error(getString("Wizard.correctErrorsFirst"));
		getPage().setResponsePage(getPage());
	}

	@Override
    public IFormSubmittingComponent getDefaultButton(IWizardModel model) {

        if (model.isNextAvailable()){
            return (IFormSubmittingComponent)get("next");
        }

        else if (model.isLastAvailable()){
            return (IFormSubmittingComponent)get("last");
        }

        else if (model.isLastStep(model.getActiveStep())){
            return (IFormSubmittingComponent)get("finish");
        }

        return null;
    }
}

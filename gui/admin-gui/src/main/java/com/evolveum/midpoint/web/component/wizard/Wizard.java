package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import com.evolveum.midpoint.web.component.wizard.resource.dto.WizardIssuesDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.wizard.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class Wizard extends BasePanel<IWizardModel> implements IWizardModelListener, IWizard {

    private static final String ID_FORM = "form";
    private static final String ID_HEADER = "header";
    private static final String ID_STEPS = "steps";
    private static final String ID_VIEW = "view";
    private static final String ID_ISSUES = "issues";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_AUTO_SAVE_NOTE = "autoSaveNote";
    private static final String ID_READ_ONLY_NOTE = "readOnlyNote";
    private static final String ID_READ_ONLY_SWITCH = "readOnlySwitch";

	@NotNull private final NonEmptyModel<WizardIssuesDto> issuesModel;

    public Wizard(String id, IModel<IWizardModel> model, @NotNull NonEmptyModel<WizardIssuesDto> issuesModel) {
        super(id, model);
		this.issuesModel = issuesModel;
		initLayout();
    }

    protected void initLayout() {
        Form form = new com.evolveum.midpoint.web.component.form.Form(ID_FORM);
        add(form);

        IModel<List<WizardStepDto>> stepsModel = new LoadableModel<List<WizardStepDto>>() {

            @Override
            protected List<WizardStepDto> load() {
                return loadSteps();
            }
        };
        WizardSteps steps = new WizardSteps(ID_STEPS, stepsModel){

            @Override
            public IWizardStep getActiveStep() {
                if(Wizard.this.getModel() != null && Wizard.this.getModel().getObject() != null){
                    return Wizard.this.getModel().getObject().getActiveStep();
                }

                return null;
            }

            @Override
            public void changeStepPerformed(AjaxRequestTarget target, WizardStepDto dto) {
                changeStep(target, dto);
            }
        };
		steps.setOutputMarkupId(true);
		steps.setVisible(hasMoreThanOneStep());
        form.add(steps);

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        form.add(header);

        WebMarkupContainer view = new WebMarkupContainer(ID_VIEW);
        form.add(view);

		WizardIssuesPanel issuesPanel = new WizardIssuesPanel(ID_ISSUES, issuesModel);
		issuesPanel.setOutputMarkupId(true);
		form.add(issuesPanel);

        WizardButtonBar buttons = new WizardButtonBar(ID_BUTTONS, this);
		buttons.setOutputMarkupId(true);
        form.add(buttons);

		WebMarkupContainer autoSaveNote = new WebMarkupContainer(ID_AUTO_SAVE_NOTE);
		autoSaveNote.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				PageResourceWizard wizardPage = (PageResourceWizard) getPageBase();
				return !wizardPage.isConfigurationOnly() && !wizardPage.isReadOnly();
			}
		});
		form.add(autoSaveNote);

		WebMarkupContainer readOnlyNote = new WebMarkupContainer(ID_READ_ONLY_NOTE);
		readOnlyNote.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				PageResourceWizard wizardPage = (PageResourceWizard) getPageBase();
				return wizardPage.isReadOnly();
			}
		});
		form.add(readOnlyNote);

		readOnlyNote.add(new AjaxFallbackLink<String>(ID_READ_ONLY_SWITCH) {
			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				PageResourceWizard wizardPage = (PageResourceWizard) getPageBase();
				wizardPage.resetModels();			// e.g. to switch configuration models to read-write
				wizardPage.setReadOnly(false);
				ajaxRequestTarget.add(wizardPage);
			}
		});

        IWizardModel wizard = getWizardModel();
        wizard.addListener(this);
        wizard.reset();
    }

	public boolean hasMoreThanOneStep() {
		Iterator<IWizardStep> iter = getWizardModel().stepIterator();
		if (!iter.hasNext()) {
			return false;
		}
		iter.next();
		return iter.hasNext();
	}

	public WizardIssuesPanel getIssuesPanel() {
		return (WizardIssuesPanel) get(createComponentPath(ID_FORM, ID_ISSUES));
	}

	public Component getSteps() {
		return get(createComponentPath(ID_FORM, ID_STEPS));
	}

	public Component getButtons() {
		return get(createComponentPath(ID_FORM, ID_BUTTONS));
	}

	private List<WizardStepDto> loadSteps() {
        List<WizardStepDto> steps = new ArrayList<>();

        IWizardModel model = getWizardModel();
        Iterator<IWizardStep> iterator = model.stepIterator();
        while (iterator.hasNext()) {
            IWizardStep step = iterator.next();
            if (step instanceof WizardStep) {
                WizardStep wizStep = (WizardStep) step;
                steps.add(new WizardStepDto(wizStep.getTitle(), wizStep, false, true));
            } else {
                steps.add(new WizardStepDto("Wizard.unknownStep", null, false, true));
            }
        }

        return steps;
    }

    @Override
    public boolean isVersioned() {
        return false;
    }

    @Override
    public IWizardModel getWizardModel() {
        IModel<IWizardModel> model = getModel();
        return model.getObject();
    }

    @Override
    public void onActiveStepChanged(IWizardStep newStep) {
        if(newStep == null){
            return;
        }

        Form form = (Form) get(ID_FORM);
        form.replace(newStep.getView(ID_VIEW, this, this));
        form.replace(newStep.getHeader(ID_HEADER, this, this));

        //mark proper wizard step as current.
        int index = 0;
        IWizardModel model = getWizardModel();
        Iterator<IWizardStep> iterator = model.stepIterator();
        while (iterator.hasNext()) {
            IWizardStep step = iterator.next();

            if (step.equals(newStep)) {
                break;
            }
            index++;
        }

        WizardSteps steps = (WizardSteps) get(createComponentPath(ID_FORM, ID_STEPS));
        IModel<List<WizardStepDto>> stepsModel = steps.getModel();
        stepsModel.getObject().get(index).setActive(true);
        steps.updateModal();
    }

    @Override
    public void onCancel() {
		getPageBase().redirectBack();
        //setResponsePage(new PageResources(false));
        //warn(getString("Wizard.message.cancel"));
    }

    @Override
    public void onFinish() {
		// 'show result' is already done
//        if(getModel() != null && getModel().getObject() != null){
//            IWizardStep activeStep = getModel().getObject().getActiveStep();
//
//            if(activeStep != null){
//                OperationResult result = ((WizardStep)activeStep).getResult();
//				if (result != null) {
//					getPageBase().showResult(result);
//				}
//            }
//        }

		getPageBase().redirectBack();
    }

    private void changeStep(AjaxRequestTarget target, WizardStepDto dto){
        IWizardStep newStep = null;
        Iterator<IWizardStep> iterator = getWizardModel().stepIterator();

        if(dto != null){
            if(getString("NameStep.title").equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof NameStep){
                        newStep = step;
                    }
                }
            } else if(getString("ConfigurationStep.title").equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof ConfigurationStep){
                        newStep = step;
                    }
                }
            } else if(getString("SchemaStep.title").equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof SchemaStep){
                        newStep = step;
                    }
                }
            } else if(getString("SchemaHandlingStep.title").equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof SchemaHandlingStep){
                        newStep = step;
                    }
                }
            } else if(getString("CapabilityStep.title").equals(dto.getName())) {
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof CapabilityStep){
                        newStep = step;
                    }
                }
            } else if(getString("SynchronizationStep.title").equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof SynchronizationStep){
                        newStep = step;
                    }
                }
            }
        }

        WizardModel model = (WizardModel) getWizardModel();
        IWizardStep currentStep = model.getActiveStep();
        currentStep.applyState();

        if(currentStep.isComplete()){
            model.setActiveStep(newStep);
            onActiveStepChanged(newStep);
        }

        target.add(this, getPageBase().getFeedbackPanel());
    }
//
//	public boolean noErrors() {
//		return !issuesModel.getObject().hasErrors();
//	}
}

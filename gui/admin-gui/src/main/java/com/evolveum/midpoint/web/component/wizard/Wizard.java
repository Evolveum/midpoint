package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class Wizard extends SimplePanel<IWizardModel> implements IWizardModelListener, IWizard {

    private static final String ID_FORM = "form";
    private static final String ID_HEADER = "header";
    private static final String ID_STEPS = "steps";
    private static final String ID_VIEW = "view";
    private static final String ID_BUTTONS = "buttons";

    public Wizard(String id, IModel<IWizardModel> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        Form form = new Form(ID_FORM);
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
        form.add(steps);

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        form.add(header);

        WebMarkupContainer view = new WebMarkupContainer(ID_VIEW);
        form.add(view);

        WizardButtonBar buttons = new WizardButtonBar(ID_BUTTONS, this);
        form.add(buttons);

        IWizardModel wizard = getWizardModel();
        wizard.addListener(this);
        wizard.reset();
    }

    private List<WizardStepDto> loadSteps() {
        List<WizardStepDto> steps = new ArrayList<>();

        IWizardModel model = getWizardModel();
        Iterator<IWizardStep> iterator = model.stepIterator();
        while (iterator.hasNext()) {
            IWizardStep step = iterator.next();
            if (step instanceof WizardStep) {
                WizardStep wizStep = (WizardStep) step;
                steps.add(new WizardStepDto(wizStep.getTitle(), false, true));
            } else {
                steps.add(new WizardStepDto("Wizard.unknownStep", false, true));
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
        setResponsePage(new PageResources(false));
        warn(getString("Wizard.message.cancel"));
    }

    @Override
    public void onFinish() {
        if(getModel() != null && getModel().getObject() != null){
            IWizardStep activeStep = getModel().getObject().getActiveStep();

            if(activeStep != null){
                OperationResult result = ((WizardStep)activeStep).getResult();

                getPageBase().showResultInSession(result);
            }
        }

        setResponsePage(new PageResources(false));
    }

    private void changeStep(AjaxRequestTarget target, WizardStepDto dto){
        IWizardStep newStep = null;
        Iterator<IWizardStep> iterator = getWizardModel().stepIterator();

        if(dto != null){
            if("Resource basics".equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof NameStep){
                        newStep = step;
                    }
                }
            } else if("Configuration".equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof ConfigurationStep){
                        newStep = step;
                    }
                }
            } else if("Schema".equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof SchemaStep){
                        newStep = step;
                    }
                }
            } else if("Schema handling".equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof SchemaHandlingStep){
                        newStep = step;
                    }
                }
            } else if("Capabilities".equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof CapabilityStep){
                        newStep = step;
                    }
                }
            } else if("Synchronization".equals(dto.getName())){
                while(iterator.hasNext()){
                    IWizardStep step = iterator.next();
                    if(step instanceof SynchronizationStep){
                        newStep = step;
                    }
                }
            }
        }

        Wizard.this.onActiveStepChanged(newStep);
        WizardModel model = (WizardModel) getWizardModel();
        model.setActiveStep(newStep);
        getWizardModel().getActiveStep().applyState();

        target.add(this);
    }
}

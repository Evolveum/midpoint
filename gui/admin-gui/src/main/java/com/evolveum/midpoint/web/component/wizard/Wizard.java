package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardModelListener;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
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

        IModel<List<WizardStepDto>> stepsModel = new Model(new ArrayList());
        stepsModel.getObject().add(new WizardStepDto("asdf"));
        stepsModel.getObject().add(new WizardStepDto("krok 2"));
        WizardSteps steps = new WizardSteps(ID_STEPS, stepsModel);
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
        Form form = (Form) get(ID_FORM);
        form.replace(newStep.getView(ID_VIEW, this, this));
        form.replace(newStep.getHeader(ID_HEADER, this, this));
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onFinish() {
    }
}

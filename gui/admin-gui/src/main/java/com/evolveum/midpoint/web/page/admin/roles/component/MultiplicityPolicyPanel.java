package com.evolveum.midpoint.web.page.admin.roles.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintEnforcementType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

/**
 * Created by honchar the panel is to be used as a content component for main
 * popup window from the PageBase class
 */
public class MultiplicityPolicyPanel extends Panel implements Popupable {
	private static final Trace LOGGER = TraceManager.getTrace(MultiplicityPolicyPanel.class);

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_ENFORCEMENT = "enforcement";
	private static final String ID_MULTIPLICITY_CONTAINER = "multiplicityContainer";
	private static final String ID_MULTIPLICITY = "multiplicity";
	private static final String ID_MULTIPLICITY_UNBOUND = "multiplicityUnbounded";
	private static final String ID_BUTTON_SAVE = "saveButton";
	private static final String ID_BUTTON_CANCEL = "cancelButton";

	private static final String ID_LABEL_SIZE = "col-md-4";
	private static final String ID_INPUT_SIZE = "col-md-8";

	private static final String MULTIPLICITY_UNBOUNDED = "unbounded";

	private boolean unbounded = false;
	private IModel<MultiplicityPolicyConstraintType> model;

	public MultiplicityPolicyPanel(String id, final MultiplicityPolicyConstraintType policy) {
		super(id);

		model = new LoadableModel<MultiplicityPolicyConstraintType>(false) {

			@Override
			protected MultiplicityPolicyConstraintType load() {
				return loadModel(policy);
			}
		};

		setOutputMarkupId(true);
		initLayout();
	}

	public IModel<MultiplicityPolicyConstraintType> getModel() {
		return model;
	}

	private MultiplicityPolicyConstraintType loadModel(MultiplicityPolicyConstraintType policy) {
		return policy == null ? new MultiplicityPolicyConstraintType() : policy;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
	}

	public void updateModel(AjaxRequestTarget target, MultiplicityPolicyConstraintType policy) {
		if (policy == null) {
			warn("MultiplicityPolicyDialog.message.badUpdate");
			target.add(getPageBase().getFeedbackPanel());
		}

		model.setObject(policy);
		target.add(getMainForm());
	}

	private PageBase getPageBase() {
		return (PageBase) getPage();
	}

	private void initLayout() {
		Form form = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
		form.setOutputMarkupId(true);
		add(form);

		TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
            new PropertyModel<>(model,
                MultiplicityPolicyConstraintType.F_DESCRIPTION.getLocalPart()),
				createStringResource("multiplicityContainer.label.description"), ID_LABEL_SIZE,
				ID_INPUT_SIZE);
		form.add(description);

		DropDownFormGroup enforcement = new DropDownFormGroup<>(ID_ENFORCEMENT,
            new PropertyModel<>(model,
                MultiplicityPolicyConstraintType.F_ENFORCEMENT.getLocalPart()),
				WebComponentUtil.createReadonlyModelFromEnum(PolicyConstraintEnforcementType.class),
            new EnumChoiceRenderer<>(),
				createStringResource("multiplicityContainer.label.enforcement"), ID_LABEL_SIZE, ID_INPUT_SIZE,
				false);
		form.add(enforcement);

		WebMarkupContainer multiplicityContainer = new WebMarkupContainer(ID_MULTIPLICITY_CONTAINER);
		multiplicityContainer.setOutputMarkupId(true);
		form.add(multiplicityContainer);

		TextField multiplicity = new TextField<>(ID_MULTIPLICITY, new PropertyModel<String>(model,
				MultiplicityPolicyConstraintType.F_MULTIPLICITY.getLocalPart()));
		multiplicity.add(prepareMultiplicityValidator());
		multiplicity.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return !unbounded;
			}
		});
		multiplicityContainer.add(multiplicity);

		CheckBox multiplicityUnbounded = new CheckBox(ID_MULTIPLICITY_UNBOUND,
            new PropertyModel<>(this, MULTIPLICITY_UNBOUNDED));
		multiplicityUnbounded.add(new OnChangeAjaxBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				model.getObject().setMultiplicity(MULTIPLICITY_UNBOUNDED);
				target.add(getMultiplicityContainer());
			}
		});
		multiplicityContainer.add(multiplicityUnbounded);

		initButtons(form);
	}

	private void initButtons(Form mainForm) {
		AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL,
				createStringResource("PageBase.button.cancel")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				cancelPerformed(target);
			}
		};
		mainForm.add(cancel);

		AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE,
				createStringResource("PageBase.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}
		};
		mainForm.add(save);
	}

	private IValidator<String> prepareMultiplicityValidator() {
		return new IValidator<String>() {

			@Override
			public void validate(IValidatable<String> toValidate) {
				String multiplicity = toValidate.getValue();

				if (!StringUtils.isNumeric(multiplicity) && !multiplicity.equals(MULTIPLICITY_UNBOUNDED)) {
					error(getString("MultiplicityPolicyDialog.message.invalidMultiplicity"));
				}
			}
		};
	}

	private WebMarkupContainer getMultiplicityContainer() {
		return (WebMarkupContainer) get(
				StringUtils.join(new String[] { ID_MAIN_FORM, ID_MULTIPLICITY_CONTAINER }, ":"));
	}

	private Component getMainForm() {
		return get(ID_MAIN_FORM);
	}

	/**
	 * Override to provide call-back to situation when cancel button is clicked
	 */
	protected void cancelPerformed(AjaxRequestTarget target) {
		getPageBase().hideMainPopup(target);
	}

	/**
	 * Override to provide call-back to situation when save button is clicked
	 */
	protected void savePerformed(AjaxRequestTarget target) {
	}

	@Override
	public int getWidth() {
		return 625;
	}

	@Override
	public int getHeight() {
		return 400;
	}

	@Override
	public StringResourceModel getTitle() {
		return new StringResourceModel("MultiplicityPolicyDialog.label");
	}

	@Override
	public Component getComponent() {
		return this;
	}

}

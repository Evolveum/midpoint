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

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.*;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueDropDownPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.input.ObjectReferenceChoiceRenderer;
import com.evolveum.midpoint.web.component.input.StringChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.jetbrains.annotations.NotNull;

/**
 * @author shood
 */
public class MappingEditorDialog extends ModalWindow {

	private static final Trace LOGGER = TraceManager.getTrace(MappingEditorDialog.class);

	private static final String DOT_CLASS = MappingEditorDialog.class.getName() + ".";
	private static final String OPERATION_LOAD_PASSWORD_POLICIES = DOT_CLASS + "createPasswordPolicyList";

	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_NAME = "name";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_AUTHORITATIVE = "authoritative";
	private static final String ID_EXCLUSIVE = "exclusive";
	private static final String ID_STRENGTH = "strength";
	private static final String ID_CHANNEL = "channel";
	private static final String ID_EXCEPT_CHANNEL = "exceptChannel";
	private static final String ID_SOURCE = "source";
	private static final String ID_TARGET = "target";
	private static final String ID_EXPRESSION_TYPE = "expressionType";
	private static final String ID_EXPRESSION = "expression";
	private static final String ID_EXPRESSION_LANG = "expressionLanguage";
	private static final String ID_EXPRESSION_POLICY_REF = "expressionValuePolicyRef";
	private static final String ID_CONDITION_TYPE = "conditionType";
	private static final String ID_CONDITION = "condition";
	private static final String ID_CONDITION_LANG = "conditionLanguage";
	private static final String ID_CONDITION_POLICY_REF = "conditionValuePolicyRef";
	// private static final String ID_TIME_FROM = "timeFrom";
	// private static final String ID_TIME_TO = "timeTo";
	private static final String ID_BUTTON_SAVE = "saveButton";
	private static final String ID_BUTTON_CANCEL = "cancelButton";

	private static final String ID_T_CHANNEL = "channelTooltip";
	private static final String ID_T_EXCEPT_CHANNEL = "exceptChannelTooltip";
	private static final String ID_T_SOURCE = "sourceTooltip";

	private static final String ID_LABEL_SIZE = "col-md-4";
	private static final String ID_INPUT_SIZE = "col-md-8";

	private static final int CODE_ROW_COUNT = 5;

	private boolean initialized;
	private IModel<MappingTypeDto> model;
	private Map<String, String> policyMap = new HashMap<>();
	private IModel<MappingType> inputModel;
	private boolean isTargetRequired = false;
	@NotNull private final NonEmptyModel<Boolean> readOnlyModel;

	public MappingEditorDialog(String id, final IModel<MappingType> mapping, NonEmptyModel<Boolean> readOnlyModel) {
		super(id);

		inputModel = mapping;
		this.readOnlyModel = readOnlyModel;
		model = new LoadableModel<MappingTypeDto>(false) {

			@Override
			protected MappingTypeDto load() {
				if (mapping != null) {
					return new MappingTypeDto(mapping.getObject(), getPageBase().getPrismContext());
				} else {
					return new MappingTypeDto(new MappingType(), getPageBase().getPrismContext());
				}
			}
		};

		setOutputMarkupId(true);
		setTitle(createStringResource("MappingEditorDialog.label"));
		showUnloadConfirmation(false);
		setCssClassName(ModalWindow.CSS_CLASS_GRAY);
		setCookieName(MappingEditorDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
		setInitialWidth(700);
		setInitialHeight(700);
		setWidthUnit("px");

		WebMarkupContainer content = new WebMarkupContainer(getContentId());
		content.setOutputMarkupId(true);
		setContent(content);
	}

	public void updateModel(AjaxRequestTarget target, IModel<MappingType> mapping, boolean isTargetRequired) {
		this.isTargetRequired = isTargetRequired;
		model.setObject(new MappingTypeDto(mapping.getObject(), getPageBase().getPrismContext()));
		inputModel = mapping;
		target.add(getContent());

		if (initialized) {
			((WebMarkupContainer) get(getContentId())).removeAll();
			initialized = false;
		}
	}

	public void updateModel(AjaxRequestTarget target, MappingType mapping, boolean isTargetRequired) {
		this.isTargetRequired = isTargetRequired;
		model.setObject(new MappingTypeDto(mapping, getPageBase().getPrismContext()));

		if (inputModel != null) {
			inputModel.setObject(mapping);
		} else {
			inputModel = new Model<>(mapping);
		}

		target.add(getContent());

		if (initialized) {
			((WebMarkupContainer) get(getContentId())).removeAll();
			initialized = false;
		}
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
	}

	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();

		if (initialized) {
			return;
		}

		initLayout((WebMarkupContainer) get(getContentId()));
		initialized = true;
	}

	public void initLayout(WebMarkupContainer content) {
		Form form = new Form(ID_MAIN_FORM);
		form.setOutputMarkupId(true);
		content.add(form);

		TextFormGroup name = new TextFormGroup(ID_NAME,
				new PropertyModel<String>(model, MappingTypeDto.F_MAPPING + ".name"),
				createStringResource("MappingEditorDialog.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		name.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(name);

		TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
				new PropertyModel<String>(model, MappingTypeDto.F_MAPPING + ".description"),
				createStringResource("MappingEditorDialog.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		description.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(description);

		CheckFormGroup authoritative = new CheckFormGroup(ID_AUTHORITATIVE,
				new PropertyModel<Boolean>(model, MappingTypeDto.F_MAPPING + ".authoritative"),
				createStringResource("MappingEditorDialog.label.authoritative"),
				"SchemaHandlingStep.mapping.tooltip.authoritative", true, ID_LABEL_SIZE, ID_INPUT_SIZE);
		authoritative.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(authoritative);

		CheckFormGroup exclusive = new CheckFormGroup(ID_EXCLUSIVE,
				new PropertyModel<Boolean>(model, MappingTypeDto.F_MAPPING + ".exclusive"),
				createStringResource("MappingEditorDialog.label.exclusive"),
				"SchemaHandlingStep.mapping.tooltip.exclusive", true, ID_LABEL_SIZE, ID_INPUT_SIZE);
		exclusive.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(exclusive);

		DropDownFormGroup strength = new DropDownFormGroup<>(ID_STRENGTH,
				new PropertyModel<MappingStrengthType>(model, MappingTypeDto.F_MAPPING + ".strength"),
				WebComponentUtil.createReadonlyModelFromEnum(MappingStrengthType.class),
				new EnumChoiceRenderer<MappingStrengthType>(this),
				createStringResource("MappingEditorDialog.label.strength"),
				"SchemaHandlingStep.mapping.tooltip.strength", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		strength.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(strength);

		MultiValueDropDownPanel channel = new MultiValueDropDownPanel<String>(ID_CHANNEL,
				new PropertyModel<List<String>>(model, MappingTypeDto.F_MAPPING + ".channel"), true, readOnlyModel) {

			@Override
			protected String createNewEmptyItem() {
				return "";
			}

			@Override
			protected IModel<List<String>> createChoiceList() {
				return new AbstractReadOnlyModel<List<String>>() {

					@Override
					public List<String> getObject() {
						return WebComponentUtil.getChannelList();
					}
				};
			}

			@Override
			protected IChoiceRenderer<String> createRenderer() {
				return new StringChoiceRenderer("Channel.", "#");

			}
		};
		form.add(channel);

		MultiValueDropDownPanel exceptChannel = new MultiValueDropDownPanel<String>(ID_EXCEPT_CHANNEL,
				new PropertyModel<List<String>>(model, MappingTypeDto.F_MAPPING + ".exceptChannel"), true, readOnlyModel) {

			@Override
			protected String createNewEmptyItem() {
				return "";
			}

			@Override
			protected IModel<List<String>> createChoiceList() {
				return new AbstractReadOnlyModel<List<String>>() {

					@Override
					public List<String> getObject() {
						return WebComponentUtil.getChannelList();
					}
				};
			}

			@Override
			protected IChoiceRenderer<String> createRenderer() {
					return new StringChoiceRenderer("Channel.", "#");
			}
		};
		form.add(exceptChannel);

		// TODO - create some nice ItemPathType editor in near future
		MultiValueTextPanel source = new MultiValueTextPanel<>(ID_SOURCE,
				new PropertyModel<List<String>>(model, MappingTypeDto.F_SOURCE), readOnlyModel, true);
		form.add(source);

		// TODO - create some nice ItemPathType editor in near future
		TextFormGroup target = new TextFormGroup(ID_TARGET, new PropertyModel<String>(model, MappingTypeDto.F_TARGET),
				createStringResource("MappingEditorDialog.label.target"), "SchemaHandlingStep.mapping.tooltip.target",
				true, ID_LABEL_SIZE, ID_INPUT_SIZE, false, isTargetRequired);
		target.setOutputMarkupId(true);
		target.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(target);

		DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType> expressionType = new DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType>(
				ID_EXPRESSION_TYPE,
				new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(model, MappingTypeDto.F_EXPRESSION_TYPE),
				WebComponentUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
				new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this),
				createStringResource("MappingEditorDialog.label.expressionType"),
				"SchemaHandlingStep.mapping.tooltip.expressionType", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false) {

			@Override
			protected DropDownChoice createDropDown(String id,
					IModel<List<ExpressionUtil.ExpressionEvaluatorType>> choices,
					IChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType> renderer, boolean required) {
				return new DropDownChoice<>(id, getModel(), choices, renderer);
			}
		};
		expressionType.getInput().add(new AjaxFormComponentUpdatingBehavior("change") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				model.getObject().updateExpression();
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION));
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION_LANG));
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION_POLICY_REF));
			}
		});
		expressionType.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(expressionType);

		DropDownFormGroup expressionLanguage = new DropDownFormGroup<>(ID_EXPRESSION_LANG,
				new PropertyModel<ExpressionUtil.Language>(model, MappingTypeDto.F_EXPRESSION_LANG),
				WebComponentUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
				new EnumChoiceRenderer<ExpressionUtil.Language>(this),
				createStringResource("MappingEditorDialog.label.language"),
				"SchemaHandlingStep.mapping.tooltip.expressionLanguage", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		expressionLanguage.setOutputMarkupId(true);
		expressionLanguage.setOutputMarkupPlaceholderTag(true);
		expressionLanguage.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(model.getObject().getExpressionType());
			}
			@Override
			public boolean isEnabled() {
				return !readOnlyModel.getObject();
			}
		});
		form.add(expressionLanguage);
		expressionLanguage.getInput().add(new AjaxFormComponentUpdatingBehavior("change") {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				model.getObject().updateExpressionLanguage();
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION));
			}
		});

		DropDownFormGroup<ObjectReferenceType> expressionGeneratePolicy = new DropDownFormGroup<ObjectReferenceType>(
				ID_EXPRESSION_POLICY_REF,
				new PropertyModel<ObjectReferenceType>(model, MappingTypeDto.F_EXPRESSION_POLICY_REF),
				new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

					@Override
					public List<ObjectReferenceType> getObject() {
						return WebModelServiceUtils.createObjectReferenceList(ValuePolicyType.class, getPageBase(), policyMap);
					}
				}, new ObjectReferenceChoiceRenderer(policyMap),
				createStringResource("MappingEditorDialog.label.passPolicyRef"),
				"SchemaHandlingStep.mapping.tooltip.expressionValuePolicyRef", true, ID_LABEL_SIZE, ID_INPUT_SIZE,
				false) {

			@Override
			protected DropDownChoice createDropDown(String id, IModel<List<ObjectReferenceType>> choices,
					IChoiceRenderer<ObjectReferenceType> renderer, boolean required) {
				return new DropDownChoice<>(id, getModel(), choices, renderer);
			}
		};
		expressionGeneratePolicy.setOutputMarkupId(true);
		expressionGeneratePolicy.setOutputMarkupPlaceholderTag(true);
		expressionGeneratePolicy.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return ExpressionUtil.ExpressionEvaluatorType.GENERATE.equals(model.getObject().getExpressionType());
			}
			@Override
			public boolean isEnabled() {
				return !readOnlyModel.getObject();
			}
		});
		expressionGeneratePolicy.getInput().add(new AjaxFormComponentUpdatingBehavior("change") {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				model.getObject().updateExpressionGeneratePolicy();
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION));
			}
		});
		form.add(expressionGeneratePolicy);

		AceEditorFormGroup expression = new AceEditorFormGroup(ID_EXPRESSION,
				new PropertyModel<String>(model, MappingTypeDto.F_EXPRESSION),
				createStringResource("MappingEditorDialog.label.expression"),
				"SchemaHandlingStep.mapping.tooltip.expression", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false,
				CODE_ROW_COUNT);
		expression.setOutputMarkupId(true);
		expression.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(expression);

		DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType> conditionType = new DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType>(
				ID_CONDITION_TYPE,
				new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(model, MappingTypeDto.F_CONDITION_TYPE),
				WebComponentUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
				new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this),
				createStringResource("MappingEditorDialog.label.conditionType"),
				"SchemaHandlingStep.mapping.tooltip.conditionType", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false) {

			@Override
			protected DropDownChoice createDropDown(String id,
					IModel<List<ExpressionUtil.ExpressionEvaluatorType>> choices,
					IChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType> renderer, boolean required) {
				return new DropDownChoice<>(id, getModel(), choices, renderer);
			}
		};
		conditionType.getInput().add(new AjaxFormComponentUpdatingBehavior("change") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				model.getObject().updateCondition();
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION));
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION_LANG));
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION_POLICY_REF));
			}
		});
		form.add(conditionType);
		conditionType.add(WebComponentUtil.enabledIfFalse(readOnlyModel));

		DropDownFormGroup conditionLanguage = new DropDownFormGroup<>(ID_CONDITION_LANG,
				new PropertyModel<ExpressionUtil.Language>(model, MappingTypeDto.F_CONDITION_LANG),
				WebComponentUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
				new EnumChoiceRenderer<ExpressionUtil.Language>(this),
				createStringResource("MappingEditorDialog.label.language"),
				"SchemaHandlingStep.mapping.tooltip.conditionLanguage", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
		conditionLanguage.setOutputMarkupId(true);
		conditionLanguage.setOutputMarkupPlaceholderTag(true);
		conditionLanguage.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(model.getObject().getConditionType());
			}

			@Override
			public boolean isEnabled() {
				return !readOnlyModel.getObject();
			}
		});
		conditionLanguage.getInput().add(new AjaxFormComponentUpdatingBehavior("change") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				model.getObject().updateConditionLanguage();
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION));
			}
		});
		form.add(conditionLanguage);

		DropDownFormGroup<ObjectReferenceType> conditionGeneratePolicy = new DropDownFormGroup<ObjectReferenceType>(
				ID_CONDITION_POLICY_REF,
				new PropertyModel<ObjectReferenceType>(model, MappingTypeDto.F_CONDITION_POLICY_REF),
				new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

					@Override
					public List<ObjectReferenceType> getObject() {
						return WebModelServiceUtils.createObjectReferenceList(ValuePolicyType.class, getPageBase(), policyMap);
					}
				}, new ObjectReferenceChoiceRenderer(policyMap), createStringResource("MappingEditorDialog.label.passPolicyRef"),
				"SchemaHandlingStep.mapping.tooltip.conditionValuePolicyRef", true, ID_LABEL_SIZE, ID_INPUT_SIZE,
				false) {

			@Override
			protected DropDownChoice createDropDown(String id, IModel<List<ObjectReferenceType>> choices,
					IChoiceRenderer<ObjectReferenceType> renderer, boolean required) {
				return new DropDownChoice<>(id, getModel(), choices, renderer);
			}
		};
		conditionGeneratePolicy.setOutputMarkupId(true);
		conditionGeneratePolicy.setOutputMarkupPlaceholderTag(true);
		conditionGeneratePolicy.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return ExpressionUtil.ExpressionEvaluatorType.GENERATE.equals(model.getObject().getConditionType());
			}

			@Override
			public boolean isEnabled() {
				return !readOnlyModel.getObject();
			}
		});
		conditionGeneratePolicy.getInput().add(new AjaxFormComponentUpdatingBehavior("change") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				model.getObject().updateConditionGeneratePolicy();
				target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION));
			}
		});
		form.add(conditionGeneratePolicy);

		AceEditorFormGroup condition = new AceEditorFormGroup(ID_CONDITION,
				new PropertyModel<String>(model, MappingTypeDto.F_CONDITION),
				createStringResource("MappingEditorDialog.label.condition"),
				"SchemaHandlingStep.mapping.tooltip.condition", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false,
				CODE_ROW_COUNT);
		condition.setOutputMarkupId(true);
		condition.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
		form.add(condition);

		Label channelTooltip = new Label(ID_T_CHANNEL);
		channelTooltip.add(new InfoTooltipBehavior(true));
		form.add(channelTooltip);

		Label exceptChannelTooltip = new Label(ID_T_EXCEPT_CHANNEL);
		exceptChannelTooltip.add(new InfoTooltipBehavior(true));
		form.add(exceptChannelTooltip);

		Label sourceTooltip = new Label(ID_T_SOURCE);
		sourceTooltip.add(new InfoTooltipBehavior(true));
		form.add(sourceTooltip);

		AjaxButton cancel = new AjaxButton(ID_BUTTON_CANCEL,
				createStringResource("MappingEditorDialog.button.cancel")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		form.add(cancel);

		AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE,
				createStringResource("MappingEditorDialog.button.apply")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getPageBase().getFeedbackPanel(), getContent());
			}
		};
		save.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
		form.add(save);
	}

	private PageBase getPageBase() {
		return (PageBase) getPage();
	}

	private List<ObjectReferenceType> createPasswordPolicyList() {
		policyMap.clear();
		OperationResult result = new OperationResult(OPERATION_LOAD_PASSWORD_POLICIES);
		Task task = getPageBase().createSimpleTask(OPERATION_LOAD_PASSWORD_POLICIES);
		List<PrismObject<ValuePolicyType>> policies = null;
		List<ObjectReferenceType> references = new ArrayList<>();

		try {
			policies = getPageBase().getModelService().searchObjects(ValuePolicyType.class, new ObjectQuery(), null,
					task, result);
			result.recomputeStatus();
		} catch (CommonException|RuntimeException e) {
			result.recordFatalError("Couldn't load password policies.", e);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load password policies", e);
		}

		// TODO - show error somehow
		// if(!result.isSuccess()){
		// getPageBase().showResult(result);
		// }

		if (policies != null) {
			ObjectReferenceType ref;

			for (PrismObject<ValuePolicyType> policy : policies) {
				policyMap.put(policy.getOid(), WebComponentUtil.getName(policy));
				ref = new ObjectReferenceType();
				ref.setType(ValuePolicyType.COMPLEX_TYPE);
				ref.setOid(policy.getOid());
				references.add(ref);
			}
		}

		return references;
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		if (inputModel != null && model.getObject() != null) {
			model.getObject().cancelChanges();
		}

		updateComponents(target);
		target.add(getPageBase().getFeedbackPanel());
		close(target);
	}

	private void savePerformed(AjaxRequestTarget target) {
		try {
			if (inputModel != null) {
				inputModel.setObject(model.getObject().prepareDtoToSave(getPageBase().getPrismContext()));
			} else {
				model.getObject().prepareDtoToSave(getPageBase().getPrismContext());
				inputModel = new PropertyModel<>(model, MappingTypeDto.F_MAPPING);
			}

		} catch (CommonException|RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save mapping.", e, e.getStackTrace());
			error(getString("MappingEditorDialog.message.cantSave") + e);
		}

		updateComponents(target);
		target.add(getPageBase().getFeedbackPanel());
		((PageResourceWizard) getPageBase()).refreshIssues(target);
		close(target);
	}

	/**
	 * Override this if update of component(s) holding this modal window is
	 * needed
	 */
	public void updateComponents(AjaxRequestTarget target) {
	}
}

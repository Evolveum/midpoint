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

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueDropDownPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class MappingEditorDialog extends ModalWindow{

    private static final Trace LOGGER = TraceManager.getTrace(MappingEditorDialog.class);

    private static final String DOT_CLASS = MappingEditorDialog.class.getName() + ".";
    private static final String OPERATION_LOAD_PASSWORD_POLICIES = DOT_CLASS + "createPasswordPolicyList";

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
//    private static final String ID_TIME_FROM = "timeFrom";
//    private static final String ID_TIME_TO = "timeTo";
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

    public MappingEditorDialog(String id, final IModel<MappingType> mapping){
        super(id);

        inputModel = mapping;
        model = new LoadableModel<MappingTypeDto>(false) {

            @Override
            protected MappingTypeDto load() {
                if(mapping != null){
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

    public void updateModel(AjaxRequestTarget target, IModel<MappingType> mapping){
        model.setObject(new MappingTypeDto(mapping.getObject(), getPageBase().getPrismContext()));
        inputModel = mapping;
        target.add(getContent());
    }

    public void updateModel(AjaxRequestTarget target, MappingType mapping){
        model.setObject(new MappingTypeDto(mapping, getPageBase().getPrismContext()));

        if(inputModel != null){
            inputModel.setObject(mapping);
        } else {
            inputModel = new Model<>(mapping);
        }

        target.add(getContent());
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
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
        Form form = new Form(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        content.add(form);

        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<String>(model, MappingTypeDto.F_MAPPING + ".name"),
                createStringResource("MappingEditorDialog.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new PropertyModel<String>(model, MappingTypeDto.F_MAPPING + ".description"),
                createStringResource("MappingEditorDialog.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(description);

        CheckFormGroup authoritative = new CheckFormGroup(ID_AUTHORITATIVE, new PropertyModel<Boolean>(model, MappingTypeDto.F_MAPPING + ".authoritative"),
                createStringResource("MappingEditorDialog.label.authoritative"), "SchemaHandlingStep.mapping.tooltip.authoritative",
                true, ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(authoritative);

        CheckFormGroup exclusive = new CheckFormGroup(ID_EXCLUSIVE, new PropertyModel<Boolean>(model, MappingTypeDto.F_MAPPING + ".exclusive"),
                createStringResource("MappingEditorDialog.label.exclusive"), "SchemaHandlingStep.mapping.tooltip.exclusive", true, ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(exclusive);

        DropDownFormGroup strength = new DropDownFormGroup<>(ID_STRENGTH,
                new PropertyModel<MappingStrengthType>(model, MappingTypeDto.F_MAPPING + ".strength"),
                WebMiscUtil.createReadonlyModelFromEnum(MappingStrengthType.class),
                new EnumChoiceRenderer<MappingStrengthType>(this), createStringResource("MappingEditorDialog.label.strength"),
                "SchemaHandlingStep.mapping.tooltip.strength", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(strength);

        MultiValueDropDownPanel channel = new MultiValueDropDownPanel<String>(ID_CHANNEL,
                new PropertyModel<List<String>>(model, MappingTypeDto.F_MAPPING + ".channel"), true, true){

            @Override
            protected String createNewEmptyItem() {
                return "";
            }

            @Override
            protected IModel<List<String>> createChoiceList() {
                return new AbstractReadOnlyModel<List<String>>() {

                    @Override
                    public List<String> getObject() {
                        return WebMiscUtil.getChannelList();
                    }
                };
            }

            @Override
            protected IChoiceRenderer<String> createRenderer() {
                return new IChoiceRenderer<String>() {

                    @Override
                    public Object getDisplayValue(String object) {
                        String[] fields = object.split("#");
                        String label = fields[1];


                        return getString("Channel." + label);
                    }

                    @Override
                    public String getIdValue(String object, int index) {
                        return Integer.toString(index);
                    }
                };
            }
        };
        form.add(channel);

        MultiValueDropDownPanel exceptChannel = new MultiValueDropDownPanel<String>(ID_EXCEPT_CHANNEL,
                new PropertyModel<List<String>>(model, MappingTypeDto.F_MAPPING +  ".exceptChannel"), true, true){

            @Override
            protected String createNewEmptyItem() {
                return "";
            }

            @Override
            protected IModel<List<String>> createChoiceList() {
                return new AbstractReadOnlyModel<List<String>>() {

                    @Override
                    public List<String> getObject() {
                        return WebMiscUtil.getChannelList();
                    }
                };
            }

            @Override
            protected IChoiceRenderer<String> createRenderer() {
                return new IChoiceRenderer<String>() {

                    @Override
                    public Object getDisplayValue(String object) {
                        String[] fields = object.split("#");
                        String label = fields[1];


                        return getString("Channel." + label);
                    }

                    @Override
                    public String getIdValue(String object, int index) {
                        return Integer.toString(index);
                    }
                };
            }
        };
        form.add(exceptChannel);

        //TODO - create some nice ItemPathType editor in near future
        MultiValueTextPanel source = new MultiValueTextPanel<>(ID_SOURCE,
                new PropertyModel<List<String>>(model, MappingTypeDto.F_SOURCE));
        form.add(source);

        //TODO - create some nice ItemPathType editor in near future
        TextFormGroup target = new TextFormGroup(ID_TARGET, new PropertyModel<String>(model, MappingTypeDto.F_TARGET),
                createStringResource("MappingEditorDialog.label.target"), "SchemaHandlingStep.mapping.tooltip.target", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(target);

        DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType> expressionType = new DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType>(ID_EXPRESSION_TYPE,
                new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(model, MappingTypeDto.F_EXPRESSION_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this),
                createStringResource("MappingEditorDialog.label.expressionType"), "SchemaHandlingStep.mapping.tooltip.expressionType", true,
                ID_LABEL_SIZE, ID_INPUT_SIZE, false){

            @Override
            protected DropDownChoice createDropDown(String id, IModel<List<ExpressionUtil.ExpressionEvaluatorType>> choices,
                                                    IChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType> renderer, boolean required) {
                return new DropDownChoice<>(id, getModel(), choices, renderer);
            }
        };
        expressionType.getInput().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpression();
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION));
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION_LANG));
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION_POLICY_REF));
            }
        });
        form.add(expressionType);

        DropDownFormGroup expressionLanguage = new DropDownFormGroup<>(ID_EXPRESSION_LANG,
                new PropertyModel<ExpressionUtil.Language>(model, MappingTypeDto.F_EXPRESSION_LANG),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
                new EnumChoiceRenderer<ExpressionUtil.Language>(this),
                createStringResource("MappingEditorDialog.label.language"), "SchemaHandlingStep.mapping.tooltip.expressionLanguage", true,
                ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        expressionLanguage.setOutputMarkupId(true);
        expressionLanguage.setOutputMarkupPlaceholderTag(true);
        expressionLanguage.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(model.getObject().getExpressionType());

            }
        });
        form.add(expressionLanguage);
        expressionLanguage.getInput().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpressionLanguage();
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION));
            }
        });

        DropDownFormGroup<ObjectReferenceType> expressionGeneratePolicy = new DropDownFormGroup<ObjectReferenceType>(ID_EXPRESSION_POLICY_REF,
                new PropertyModel<ObjectReferenceType>(model, MappingTypeDto.F_EXPRESSION_POLICY_REF),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                        return createPasswordPolicyList();
                    }
                },
        new IChoiceRenderer<ObjectReferenceType>() {

            @Override
            public Object getDisplayValue(ObjectReferenceType object) {
                return policyMap.get(object.getOid());
            }

            @Override
            public String getIdValue(ObjectReferenceType object, int index) {
                return Integer.toString(index);
            }
        }, createStringResource("MappingEditorDialog.label.passPolicyRef"), "SchemaHandlingStep.mapping.tooltip.expressionValuePolicyRef",
                true, ID_LABEL_SIZE, ID_INPUT_SIZE, false){

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
        });
        expressionGeneratePolicy.getInput().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpressionGeneratePolicy();
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_EXPRESSION));
            }
        });
        form.add(expressionGeneratePolicy);

        TextAreaFormGroup expression = new TextAreaFormGroup(ID_EXPRESSION, new PropertyModel<String>(model, MappingTypeDto.F_EXPRESSION),
                createStringResource("MappingEditorDialog.label.expression"), "SchemaHandlingStep.mapping.tooltip.expression", true, ID_LABEL_SIZE, ID_INPUT_SIZE,
                false, CODE_ROW_COUNT);
        expression.setOutputMarkupId(true);
        form.add(expression);

        DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType> conditionType = new DropDownFormGroup<ExpressionUtil.ExpressionEvaluatorType>(ID_CONDITION_TYPE,
                new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(model, MappingTypeDto.F_CONDITION_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this),
                createStringResource("MappingEditorDialog.label.conditionType"), "SchemaHandlingStep.mapping.tooltip.conditionType",
                true, ID_LABEL_SIZE, ID_INPUT_SIZE, false){

            @Override
            protected DropDownChoice createDropDown(String id, IModel<List<ExpressionUtil.ExpressionEvaluatorType>> choices,
                                                    IChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType> renderer,
                                                    boolean required) {
                return new DropDownChoice<>(id, getModel(), choices, renderer);
            }
        };
        conditionType.getInput().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateCondition();
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION));
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION_LANG));
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION_POLICY_REF));
            }
        });
        form.add(conditionType);

        DropDownFormGroup conditionLanguage = new DropDownFormGroup<>(ID_CONDITION_LANG,
                new PropertyModel<ExpressionUtil.Language>(model, MappingTypeDto.F_CONDITION_LANG),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
                new EnumChoiceRenderer<ExpressionUtil.Language>(this),
                createStringResource("MappingEditorDialog.label.language"), "SchemaHandlingStep.mapping.tooltip.conditionLanguage",
                true, ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        conditionLanguage.setOutputMarkupId(true);
        conditionLanguage.setOutputMarkupPlaceholderTag(true);
        conditionLanguage.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(model.getObject().getConditionType());
            }
        });
        conditionLanguage.getInput().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateConditionLanguage();
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION));
            }
        });
        form.add(conditionLanguage);

        DropDownFormGroup<ObjectReferenceType> conditionGeneratePolicy = new DropDownFormGroup<ObjectReferenceType>(ID_CONDITION_POLICY_REF,
                new PropertyModel<ObjectReferenceType>(model, MappingTypeDto.F_CONDITION_POLICY_REF),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                        return createPasswordPolicyList();
                    }
                },
                new IChoiceRenderer<ObjectReferenceType>() {

                    @Override
                    public Object getDisplayValue(ObjectReferenceType object) {
                        return policyMap.get(object.getOid());
                    }

                    @Override
                    public String getIdValue(ObjectReferenceType object, int index) {
                        return Integer.toString(index);
                    }
                }, createStringResource("MappingEditorDialog.label.passPolicyRef"), "SchemaHandlingStep.mapping.tooltip.conditionValuePolicyRef",
                true, ID_LABEL_SIZE, ID_INPUT_SIZE, false){

            @Override
            protected DropDownChoice createDropDown(String id, IModel<List<ObjectReferenceType>> choices,
                                                    IChoiceRenderer<ObjectReferenceType> renderer,
                                                    boolean required) {
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
        });
        conditionGeneratePolicy.getInput().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateConditionGeneratePolicy();
                target.add(get(getContentId() + ":" + ID_MAIN_FORM + ":" + ID_CONDITION));
            }
        });
        form.add(conditionGeneratePolicy);

        TextAreaFormGroup condition = new TextAreaFormGroup(ID_CONDITION, new PropertyModel<String>(model, MappingTypeDto.F_CONDITION),
                createStringResource("MappingEditorDialog.label.condition"), "SchemaHandlingStep.mapping.tooltip.condition", true, ID_LABEL_SIZE, ID_INPUT_SIZE,
                false, CODE_ROW_COUNT);
        condition.setOutputMarkupId(true);
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

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL,
                createStringResource("MappingEditorDialog.button.cancel")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                cancelPerformed(target);
            }
        };
        form.add(cancel);

        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE,
                createStringResource("MappingEditorDialog.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }
        };
        form.add(save);
    }

    private PageBase getPageBase(){
        return (PageBase) getPage();
    }

    private List<ObjectReferenceType> createPasswordPolicyList(){
        policyMap.clear();
        OperationResult result = new OperationResult(OPERATION_LOAD_PASSWORD_POLICIES);
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_PASSWORD_POLICIES);
        List<PrismObject<ValuePolicyType>> policies = null;
        List<ObjectReferenceType> references = new ArrayList<>();

        try{
            policies = getPageBase().getModelService().searchObjects(ValuePolicyType.class, new ObjectQuery(), null, task, result);
            result.recomputeStatus();
        } catch (Exception e){
            result.recordFatalError("Couldn't load password policies.", e);
            LoggingUtils.logException(LOGGER, "Couldn't load password policies", e);
        }

        // TODO - show error somehow
        // if(!result.isSuccess()){
        //    getPageBase().showResult(result);
        // }

        if(policies != null){
            ObjectReferenceType ref;

            for(PrismObject<ValuePolicyType> policy: policies){
                policyMap.put(policy.getOid(), WebMiscUtil.getName(policy));
                ref = new ObjectReferenceType();
                ref.setType(ValuePolicyType.COMPLEX_TYPE);
                ref.setOid(policy.getOid());
                references.add(ref);
            }
        }

        return references;
    }

    private void cancelPerformed(AjaxRequestTarget target){
        close(target);
    }

    private void savePerformed(AjaxRequestTarget target){
        try {
            if(inputModel != null){
                inputModel.setObject(model.getObject().prepareDtoToSave(getPageBase().getPrismContext()));
            } else {
                model.getObject().prepareDtoToSave(getPageBase().getPrismContext());
                inputModel = new PropertyModel<>(model, MappingTypeDto.F_MAPPING);
            }

        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Couldn't save mapping.", e, e.getStackTrace());
            error(getString("MappingEditorDialog.message.cantSave") + e);
        }

        updateComponents(target);
        target.add(getPageBase().getFeedbackPanel());
        close(target);
    }

    public void updateComponents(AjaxRequestTarget target){
        //Override this if update of component(s) holding this modal window is needed
    }
}

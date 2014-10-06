/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.dto.ExpressionTypeDto;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class ExpressionEditorPanel extends SimplePanel<ExpressionType>{

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionEditorPanel.class);

    private static final String DOT_CLASS = ExpressionEditorPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_PASSWORD_POLICIES = DOT_CLASS + "createPasswordPolicyList";

    private static final String ID_TYPE = "type";
    private static final String ID_LANGUAGE = "language";
    private static final String ID_POLICY_REF = "policyRef";
    private static final String ID_EXPRESSION = "expression";
    private static final String ID_LANGUAGE_CONTAINER = "languageContainer";
    private static final String ID_POLICY_CONTAINER = "policyRefContainer";
    private static final String ID_BUTTON_UPDATE = "update";
    private static final String ID_LABEL_TYPE = "typeLabel";
    private static final String ID_LABEL_EXPRESSION = "expressionLabel";

    private IModel<ExpressionTypeDto> model;
    private Map<String, String> policyMap = new HashMap<>();

    public ExpressionEditorPanel(String id, IModel<ExpressionType> model){
        super(id, model);
    }

    public IModel<ExpressionTypeDto> getExpressionModel(){
        return model;
    }

    private void loadModel(){
        if(model == null){
            model = new LoadableModel<ExpressionTypeDto>(false) {

                @Override
                protected ExpressionTypeDto load() {
                    return new ExpressionTypeDto(getModel().getObject(), getPageBase().getPrismContext());
                }
            };
        }
    }

    @Override
    protected void initLayout(){
        loadModel();

        Label typeLabel = new Label(ID_LABEL_TYPE, createStringResource(getTypeLabelKey()));
        add(typeLabel);

        DropDownChoice type = new DropDownChoice<>(ID_TYPE,
                new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(model, ExpressionTypeDto.F_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this));
        type.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpressionType();
                target.add(get(ID_LANGUAGE_CONTAINER), get(ID_POLICY_CONTAINER), get(ID_EXPRESSION));
            }
        });
        type.setOutputMarkupId(true);
        type.setOutputMarkupPlaceholderTag(true);
        type.setNullValid(true);
        add(type);

        WebMarkupContainer languageContainer = new WebMarkupContainer(ID_LANGUAGE_CONTAINER);
        languageContainer.setOutputMarkupId(true);
        languageContainer.setOutputMarkupPlaceholderTag(true);
        languageContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible(){
                return ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(model.getObject().getType());
            }
        });
        add(languageContainer);

        DropDownChoice language = new DropDownChoice<>(ID_LANGUAGE,
                new PropertyModel<ExpressionUtil.Language>(model, ExpressionTypeDto.F_LANGUAGE),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
                new EnumChoiceRenderer<ExpressionUtil.Language>(this));
        language.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpressionLanguage();
                target.add(get(ID_LANGUAGE_CONTAINER), get(ID_POLICY_CONTAINER), get(ID_EXPRESSION));
            }
        });
        language.setNullValid(false);
        languageContainer.add(language);

        WebMarkupContainer policyContainer = new WebMarkupContainer(ID_POLICY_CONTAINER);
        policyContainer.setOutputMarkupId(true);
        policyContainer.setOutputMarkupPlaceholderTag(true);
        policyContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible(){
                return ExpressionUtil.ExpressionEvaluatorType.GENERATE.equals(model.getObject().getType());
            }
        });
        add(policyContainer);

        DropDownChoice policyRef = new DropDownChoice<>(ID_POLICY_REF,
                new PropertyModel<ObjectReferenceType>(model, ExpressionTypeDto.F_POLICY_REF),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                        return createPasswordPolicyList();
                    }
                }, new IChoiceRenderer<ObjectReferenceType>() {

            @Override
            public Object getDisplayValue(ObjectReferenceType object) {
                return policyMap.get(object.getOid());
            }

            @Override
            public String getIdValue(ObjectReferenceType object, int index) {
                return Integer.toString(index);
            }
        });
        policyRef.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpressionValuePolicyRef();
                target.add(get(ID_LANGUAGE_CONTAINER), get(ID_POLICY_CONTAINER), get(ID_EXPRESSION));
            }
        });
        policyRef.setNullValid(true);
        policyContainer.add(policyRef);

        Label expressionLabel = new Label(ID_LABEL_EXPRESSION, createStringResource(getExpressionLabelKey()));
        add(expressionLabel);

        TextArea expression = new TextArea<>(ID_EXPRESSION, new PropertyModel<String>(model, ExpressionTypeDto.F_EXPRESSION));
        expression.setOutputMarkupId(true);
        add(expression);

        AjaxSubmitLink update = new AjaxSubmitLink(ID_BUTTON_UPDATE) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateExpressionPerformed(target);
            }
        };
        add(update);
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

    protected void updateExpressionPerformed(AjaxRequestTarget target){
        try {
            model.getObject().updateExpression(getPageBase().getPrismContext());

            success(getString("ExpressionEditorPanel.message.expressionSuccess"));
        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Could not create JAXBElement<?> from provided xml expression.", e);
            error(getString("ExpressionEditorPanel.message.cantSerialize", e));
        }

        performExpressionHook(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    /**
     *  Override this in component with ExpressionEditorPanel to provide additional functionality when expression is updated
     * */
    public void performExpressionHook(AjaxRequestTarget target){}

    /**
     *  Provide key for expression type label
     * */
    public String getTypeLabelKey(){
        return "ExpressionEditorPanel.label.type";
    }

    /**
     *  Provide key for expression label
     * */
    public String getExpressionLabelKey(){
        return "ExpressionEditorPanel.label.expression";
    }
}

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
package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.ExpressionVariableEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.IterationSpecificationTypeDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class ResourceIterationEditor extends SimplePanel<IterationSpecificationType>{

    private static final Trace LOGGER = TraceManager.getTrace(ResourceIterationEditor.class);

    private static final String DOT_CLASS = ResourceIterationEditor.class.getName() + ".";
    private static final String OPERATION_LOAD_PASSWORD_POLICIES = DOT_CLASS + "createPasswordPolicyList";

    private static final String ID_MAX_ITERATION = "maxIteration";
    private static final String ID_TOKEN_DESCRIPTION = "tokenDescription";
    private static final String ID_TOKEN_VARIABLE_LIST = "tokenVariableList";
    private static final String ID_TOKEN_RETURN_MULTIPLICITY = "tokenReturnMultiplicity";
    private static final String ID_PRE_DESCRIPTION = "preDescription";
    private static final String ID_PRE_VARIABLE_LIST = "preVariableList";
    private static final String ID_PRE_RETURN_MULTIPLICITY = "preReturnMultiplicity";
    private static final String ID_POST_DESCRIPTION = "postDescription";
    private static final String ID_POST_VARIABLE_LIST = "postVariableList";
    private static final String ID_POST_RETURN_MULTIPLICITY = "postReturnMultiplicity";
    private static final String ID_VARIABLE_EDITOR_MODAL = "variableEditor";
    private static final String ID_TOKEN_EXPR_TYPE = "tokenExpressionType";
    private static final String ID_TOKEN_EXPR = "tokenExpression";
    private static final String ID_PRE_EXPR_TYPE = "preExpressionType";
    private static final String ID_PRE_EXPR = "preExpression";
    private static final String ID_POST_EXPR_TYPE = "postExpressionType";
    private static final String ID_POST_EXPR = "postExpression";
    private static final String ID_TOKEN_EXPR_LANG = "tokenExpressionLanguage";
    private static final String ID_TOKEN_EXPR_POLICY = "tokenExpressionPolicyRef";
    private static final String ID_PRE_EXPR_LANG = "preExpressionLanguage";
    private static final String ID_PRE_EXPR_POLICY = "preExpressionPolicyRef";
    private static final String ID_POST_EXPR_LANG = "postExpressionLanguage";
    private static final String ID_POST_EXPR_POLICY = "postExpressionPolicyRef";
    private static final String ID_TOKEN_LANG_CONTAINER = "tokenLanguageContainer";
    private static final String ID_TOKEN_POLICY_CONTAINER = "tokenPolicyContainer";
    private static final String ID_PRE_LANG_CONTAINER = "preLanguageContainer";
    private static final String ID_PRE_POLICY_CONTAINER = "prePolicyContainer";
    private static final String ID_POST_LANG_CONTAINER = "postLanguageContainer";
    private static final String ID_POST_POLICY_CONTAINER = "postPolicyContainer";
    private static final String ID_T_MAX_ITERATION = "maxIterationTooltip";
    private static final String ID_T_TOKEN_VAR = "tokenVariableTooltip";
    private static final String ID_T_TOKEN_MUL = "tokenReturnMultiplicityTooltip";
    private static final String ID_T_PRE_VAR = "preVariableTooltip";
    private static final String ID_T_PRE_MUL = "preReturnMultiplicityTooltip";
    private static final String ID_T_POST_VAR = "postVariableTooltip";
    private static final String ID_T_POST_MUL = "postReturnMultiplicityTooltip";

    private Map<String, String> policyMap = new HashMap<>();
    private IModel<IterationSpecificationTypeDto> model;

    public ResourceIterationEditor(String id, IModel<IterationSpecificationType> iteration){
        super(id, iteration);
    }

    @Override
    public IModel<IterationSpecificationType> getModel(){
         IModel<IterationSpecificationType> model = super.getModel();

        if(model.getObject() == null){
            model.setObject(new IterationSpecificationType());
        }

        IterationSpecificationType iteration = model.getObject();

        if(iteration.getTokenExpression() == null){
            iteration.setTokenExpression(new ExpressionType());
        }

        if(iteration.getPreIterationCondition() == null){
            iteration.setPreIterationCondition(new ExpressionType());
        }

        if(iteration.getPostIterationCondition() == null){
            iteration.setPostIterationCondition(new ExpressionType());
        }

        return model;
    }

    private void loadModel(){
        if(this.model == null){
            this.model = new LoadableModel<IterationSpecificationTypeDto>(false) {

                @Override
                protected IterationSpecificationTypeDto load() {
                    return new IterationSpecificationTypeDto(getModel().getObject());
//                    return new IterationSpecificationTypeDto(getModel().getObject(), getPageBase().getPrismContext());
                }
            };
        }
    }

    @Override
    protected void initLayout(){
        loadModel();
        getModel();
        TextField maxIteration = new TextField<>(ID_MAX_ITERATION, new PropertyModel<Integer>(model,
                IterationSpecificationTypeDto.F_ITERATION + "." + "maxIterations"));
        add(maxIteration);

        prepareIterationSubsectionBody(IterationSpecificationType.F_TOKEN_EXPRESSION.getLocalPart(), ID_TOKEN_DESCRIPTION,
                ID_TOKEN_VARIABLE_LIST, ID_TOKEN_RETURN_MULTIPLICITY, ID_TOKEN_EXPR_TYPE, ID_TOKEN_EXPR,
                ID_TOKEN_EXPR_LANG, ID_TOKEN_EXPR_POLICY, IterationSpecificationTypeDto.TOKEN_EXPRESSION_PREFIX,
                ID_TOKEN_LANG_CONTAINER, ID_TOKEN_POLICY_CONTAINER);

        prepareIterationSubsectionBody(IterationSpecificationType.F_PRE_ITERATION_CONDITION.getLocalPart(), ID_PRE_DESCRIPTION,
                ID_PRE_VARIABLE_LIST, ID_PRE_RETURN_MULTIPLICITY, ID_PRE_EXPR_TYPE, ID_PRE_EXPR,
                ID_PRE_EXPR_LANG, ID_PRE_EXPR_POLICY, IterationSpecificationTypeDto.PRE_EXPRESSION_PREFIX,
                ID_PRE_LANG_CONTAINER, ID_PRE_POLICY_CONTAINER);

        prepareIterationSubsectionBody(IterationSpecificationType.F_POST_ITERATION_CONDITION.getLocalPart(), ID_POST_DESCRIPTION,
                ID_POST_VARIABLE_LIST, ID_POST_RETURN_MULTIPLICITY, ID_POST_EXPR_TYPE, ID_POST_EXPR,
                ID_POST_EXPR_LANG, ID_POST_EXPR_POLICY, IterationSpecificationTypeDto.POST_EXPRESSION_PREFIX,
                ID_POST_LANG_CONTAINER, ID_POST_POLICY_CONTAINER);

        Label maxItTooltip = new Label(ID_T_MAX_ITERATION);
        maxItTooltip.add(new InfoTooltipBehavior());
        add(maxItTooltip);

        Label tokenVarTooltip = new Label(ID_T_TOKEN_VAR);
        tokenVarTooltip.add(new InfoTooltipBehavior());
        add(tokenVarTooltip);

        Label tokenMulTooltip = new Label(ID_T_TOKEN_MUL);
        tokenMulTooltip.add(new InfoTooltipBehavior());
        add(tokenMulTooltip);

        Label preVarTooltip = new Label(ID_T_PRE_VAR);
        preVarTooltip.add(new InfoTooltipBehavior());
        add(preVarTooltip);

        Label preMulTooltip = new Label(ID_T_PRE_MUL);
        preMulTooltip.add(new InfoTooltipBehavior());
        add(preMulTooltip);

        Label postVarTooltip = new Label(ID_T_POST_VAR);
        postVarTooltip.add(new InfoTooltipBehavior());
        add(postVarTooltip);

        Label postMulTooltip = new Label(ID_T_POST_MUL);
        postMulTooltip.add(new InfoTooltipBehavior());
        add(postMulTooltip);

        initModals();
    }

    private void prepareIterationSubsectionBody(String containerValue, String descriptionId, String variableId,
                                                String returnMultiplicityId, String expressionType, final String expression,
                                                final String languageId, final String policyId, final String prefix,
                                                final String languageContainerId, final String policyContainerId){
        TextArea description = new TextArea<>(descriptionId, new PropertyModel<String>(model,
                IterationSpecificationTypeDto.F_ITERATION + "." + containerValue + ".description"));
        add(description);

        MultiValueTextEditPanel variableList = new MultiValueTextEditPanel<ExpressionVariableDefinitionType>(variableId,
                new PropertyModel<List<ExpressionVariableDefinitionType>>(model,
                        IterationSpecificationTypeDto.F_ITERATION + "." + containerValue + ".variable"), false, true){

            @Override
            protected IModel<String> createTextModel(final IModel<ExpressionVariableDefinitionType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        ExpressionVariableDefinitionType variable = model.getObject();

                        if(variable != null && variable.getName() != null){
                            return variable.getName().getLocalPart();
                        } else {
                            return null;
                        }
                    }
                };
            }

            @Override
            protected ExpressionVariableDefinitionType createNewEmptyItem(){
                return new ExpressionVariableDefinitionType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, ExpressionVariableDefinitionType object){
                expressionVariableEditPerformed(target, object);
            }
        };
        add(variableList);

        DropDownChoice returnMultiplicity = new DropDownChoice<>(returnMultiplicityId,
                new PropertyModel<ExpressionReturnMultiplicityType>(model,
                        IterationSpecificationTypeDto.F_ITERATION + "." + containerValue + ".returnMultiplicity"),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionReturnMultiplicityType.class),
                new EnumChoiceRenderer<ExpressionReturnMultiplicityType>(this));
        add(returnMultiplicity);

        /*
        DropDownChoice exprType = new DropDownChoice<>(expressionType,
                new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(model, prefix + "ExpressionType"),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this));
        exprType.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpression(prefix);
                target.add(get(expression));
                target.add(get(languageContainerId));
                target.add(get(policyContainerId));
            }
        });
        add(exprType);

        WebMarkupContainer langContainer = new WebMarkupContainer(languageContainerId);
        langContainer.setOutputMarkupId(true);
        langContainer.setOutputMarkupPlaceholderTag(true);
        langContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(model.getObject().getExpressionType(prefix))) {
                    return true;
                }
                return false;
            }
        });
        add(langContainer);

        DropDownChoice language = new DropDownChoice<>(languageId,
                new PropertyModel<ExpressionUtil.Language>(model, prefix + IterationSpecificationTypeDto.F_LANGUAGE),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
                new EnumChoiceRenderer<ExpressionUtil.Language>(this));
        language.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpressionLanguage(prefix);
                target.add(get(expression));
            }
        });
        langContainer.add(language);

        WebMarkupContainer policyContainer = new WebMarkupContainer(policyContainerId);
        policyContainer.setOutputMarkupId(true);
        policyContainer.setOutputMarkupPlaceholderTag(true);
        policyContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (ExpressionUtil.ExpressionEvaluatorType.GENERATE.equals(model.getObject().getExpressionType(prefix))) {
                    return true;
                }
                return false;
            }
        });
        add(policyContainer);

        DropDownChoice policy = new DropDownChoice<>(policyId,
                new PropertyModel<ObjectReferenceType>(model, prefix + IterationSpecificationTypeDto.F_POLICY_REF),
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
        policy.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.getObject().updateExpressionPolicy(prefix);
                target.add(get(expression));
            }
        });
        policyContainer.add(policy);

        TextArea expr = new TextArea<>(expression, new PropertyModel<String>(model, prefix + "Expression"));
        expr.setOutputMarkupId(true);
        add(expr);
        */
    }

    //TODO - optimize this - now we are loading this 3* when resource iteration is edited
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

    private void initModals(){
        ModalWindow variableEditor = new ExpressionVariableEditorDialog(ID_VARIABLE_EDITOR_MODAL, null){

            @Override
            public void updateComponents(AjaxRequestTarget target){
                target.add(ResourceIterationEditor.this.get(ID_POST_VARIABLE_LIST), ResourceIterationEditor.this.get(ID_PRE_VARIABLE_LIST),
                        ResourceIterationEditor.this.get(ID_TOKEN_VARIABLE_LIST));
            }
        };
        add(variableEditor);
    }

    private void expressionVariableEditPerformed(AjaxRequestTarget target, ExpressionVariableDefinitionType object){
        ExpressionVariableEditorDialog window = (ExpressionVariableEditorDialog) get(ID_VARIABLE_EDITOR_MODAL);
        window.updateModel(target, object);
        window.show(target);
    }

}

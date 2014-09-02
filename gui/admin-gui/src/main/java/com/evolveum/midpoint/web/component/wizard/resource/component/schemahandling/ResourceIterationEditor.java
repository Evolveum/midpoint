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
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.ExpressionVariableEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.IterationSpecificationTypeDto;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class ResourceIterationEditor extends SimplePanel{

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

    private Map<String, String> policyMap = new HashMap<>();

    public ResourceIterationEditor(String id, IModel<IterationSpecificationType> model){
        super(id, model);
    }

    @Override
    public IModel<IterationSpecificationTypeDto> getModel(){
         IModel<IterationSpecificationType> model = super.getModel();

        if(model.getObject() == null){
            model.setObject(new IterationSpecificationType());
        }

        final IterationSpecificationType iteration = model.getObject();

        if(iteration.getTokenExpression() == null){
            iteration.setTokenExpression(new ExpressionType());
        }

        if(iteration.getPreIterationCondition() == null){
            iteration.setPreIterationCondition(new ExpressionType());
        }

        if(iteration.getPostIterationCondition() == null){
            iteration.setPostIterationCondition(new ExpressionType());
        }

        IModel<IterationSpecificationTypeDto> iterationModel = new LoadableModel<IterationSpecificationTypeDto>(false) {
            @Override
            protected IterationSpecificationTypeDto load() {
                return new IterationSpecificationTypeDto(iteration);
            }
        };

        return iterationModel;
    }

    @Override
    protected void initLayout(){
        TextField maxIteration = new TextField<>(ID_MAX_ITERATION, new PropertyModel<Integer>(getModel(), "maxIterations"));
        add(maxIteration);

        prepareIterationSubsectionBody(IterationSpecificationType.F_TOKEN_EXPRESSION.getLocalPart(), ID_TOKEN_DESCRIPTION,
                ID_TOKEN_VARIABLE_LIST, ID_TOKEN_RETURN_MULTIPLICITY, ID_TOKEN_EXPR_TYPE, ID_TOKEN_EXPR,
                ID_TOKEN_EXPR_LANG, ID_TOKEN_EXPR_POLICY, IterationSpecificationTypeDto.TOKEN_EXPRESSION_PREFIX);

        prepareIterationSubsectionBody(IterationSpecificationType.F_PRE_ITERATION_CONDITION.getLocalPart(), ID_PRE_DESCRIPTION,
                ID_PRE_VARIABLE_LIST, ID_PRE_RETURN_MULTIPLICITY, ID_PRE_EXPR_TYPE, ID_PRE_EXPR,
                ID_PRE_EXPR_LANG, ID_PRE_EXPR_POLICY, IterationSpecificationTypeDto.PRE_EXPRESSION_PREFIX);

        prepareIterationSubsectionBody(IterationSpecificationType.F_POST_ITERATION_CONDITION.getLocalPart(), ID_POST_DESCRIPTION,
                ID_POST_VARIABLE_LIST, ID_POST_RETURN_MULTIPLICITY, ID_POST_EXPR_TYPE, ID_POST_EXPR,
                ID_POST_EXPR_LANG, ID_POST_EXPR_POLICY, IterationSpecificationTypeDto.POST_EXPRESSION_PREFIX);

        initModals();
    }

    private void prepareIterationSubsectionBody(String containerValue, String descriptionId, String variableId,
                                                String returnMultiplicityId, String expressionType, final String expression,
                                                final String languageId, final String policyId, final String prefix){
        TextArea description = new TextArea<>(descriptionId, new PropertyModel<String>(getModel(), containerValue + ".description"));
        add(description);

        MultiValueTextEditPanel variableList = new MultiValueTextEditPanel<ExpressionVariableDefinitionType>(variableId,
                new PropertyModel<List<ExpressionVariableDefinitionType>>(getModel(), containerValue + ".variable"), false, true){

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
                new PropertyModel<ExpressionReturnMultiplicityType>(getModel(), containerValue + ".returnMultiplicity"),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionReturnMultiplicityType.class),
                new EnumChoiceRenderer<ExpressionReturnMultiplicityType>(this));
        add(returnMultiplicity);

        DropDownChoice exprType = new DropDownChoice<>(expressionType,
                new PropertyModel<ExpressionUtil.ExpressionEvaluatorType>(this, prefix + "ExpressionType"),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.ExpressionEvaluatorType.class),
                new EnumChoiceRenderer<ExpressionUtil.ExpressionEvaluatorType>(this));
        exprType.add(new AjaxFormComponentUpdatingBehavior("onBlur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModel().getObject().updateExpression(prefix);
                target.add(get(expression));
                target.add(get(languageId));
                target.add(get(policyId));
            }
        });
        add(exprType);

        DropDownChoice language = new DropDownChoice<>(languageId,
                new PropertyModel<ExpressionUtil.Language>(getModel(), prefix + IterationSpecificationTypeDto.F_LANGUAGE),
                WebMiscUtil.createReadonlyModelFromEnum(ExpressionUtil.Language.class),
                new EnumChoiceRenderer<ExpressionUtil.Language>(this));
        language.setOutputMarkupId(true);
        language.setOutputMarkupPlaceholderTag(true);
        language.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (ExpressionUtil.ExpressionEvaluatorType.SCRIPT.equals(getModel().getObject().getExpressionType(prefix))) {
                    return true;
                }
                return false;
            }
        });
        language.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModel().getObject().updateExpressionLanguage(prefix);
                target.add(get(expression));
            }
        });
        add(language);

        DropDownChoice policy = new DropDownChoice<>(policyId,
                new PropertyModel<ObjectReferenceType>(getModel(), prefix + IterationSpecificationTypeDto.F_POLICY_REF),
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
        policy.setOutputMarkupId(true);
        policy.setOutputMarkupPlaceholderTag(true);
        policy.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (ExpressionUtil.ExpressionEvaluatorType.GENERATE.equals(getModel().getObject().getExpressionType(prefix))) {
                    return true;
                }
                return false;
            }
        });
        policy.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModel().getObject().updateExpressionPolicy(prefix);
            }
        });
        add(policy);

        TextArea expr = new TextArea<>(expression, new PropertyModel<String>(this, prefix + "Expression"));
        expr.setOutputMarkupId(true);
        add(expr);
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
        ModalWindow variableEditor = new ExpressionVariableEditorDialog(ID_VARIABLE_EDITOR_MODAL, null);
        add(variableEditor);
    }

    private void expressionVariableEditPerformed(AjaxRequestTarget target, ExpressionVariableDefinitionType object){
        ExpressionVariableEditorDialog window = (ExpressionVariableEditorDialog) get(ID_VARIABLE_EDITOR_MODAL);
        window.updateModel(target, object);
        window.show(target);
    }

}

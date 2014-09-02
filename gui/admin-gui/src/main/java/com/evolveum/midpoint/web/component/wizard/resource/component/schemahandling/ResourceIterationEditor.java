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

import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.ExpressionVariableEditorDialog;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 *  @author shood
 * */
public class ResourceIterationEditor extends SimplePanel{

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

    private static final String TOKEN_EXPRESSION_PREFIX = "token";
    private static final String PRE_EXPRESSION_PREFIX = "pre";
    private static final String POST_EXPRESSION_PREFIX = "post";

    //TODO - maybe add this to some DTO if possible
    private ExpressionUtil.ExpressionEvaluatorType tokenExpressionType;
    private ExpressionUtil.ExpressionEvaluatorType preExpressionType;
    private ExpressionUtil.ExpressionEvaluatorType postExpressionType;
    private String tokenExpression;
    private String preExpression;
    private String postExpression;

    public ResourceIterationEditor(String id, IModel<IterationSpecificationType> model){
        super(id, model);
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

    @Override
    protected void initLayout(){
        TextField maxIteration = new TextField<>(ID_MAX_ITERATION, new PropertyModel<Integer>(getModel(), "maxIterations"));
        add(maxIteration);

        prepareIterationSubsectionBody(IterationSpecificationType.F_TOKEN_EXPRESSION.getLocalPart(), ID_TOKEN_DESCRIPTION,
                ID_TOKEN_VARIABLE_LIST, ID_TOKEN_RETURN_MULTIPLICITY, ID_TOKEN_EXPR_TYPE, ID_TOKEN_EXPR, TOKEN_EXPRESSION_PREFIX);

        prepareIterationSubsectionBody(IterationSpecificationType.F_PRE_ITERATION_CONDITION.getLocalPart(), ID_PRE_DESCRIPTION,
                ID_PRE_VARIABLE_LIST, ID_PRE_RETURN_MULTIPLICITY, ID_PRE_EXPR_TYPE, ID_PRE_EXPR, PRE_EXPRESSION_PREFIX);

        prepareIterationSubsectionBody(IterationSpecificationType.F_POST_ITERATION_CONDITION.getLocalPart(), ID_POST_DESCRIPTION,
                ID_POST_VARIABLE_LIST, ID_POST_RETURN_MULTIPLICITY, ID_POST_EXPR_TYPE, ID_POST_EXPR, POST_EXPRESSION_PREFIX);

        initModals();
    }

    private void prepareIterationSubsectionBody(String containerValue, String descriptionId, String variableId,
                                                String returnMultiplicityId, String expressionType,
                                                final String expression, final String prefix){
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
                updateExpression(prefix);
                target.add(get(expression));
            }
        });
        add(exprType);

        TextArea expr = new TextArea<>(expression, new PropertyModel<String>(this, prefix + "Expression"));
        expr.setOutputMarkupId(true);
        add(expr);
    }

    private void updateExpression(String prefix){
        if(prefix.equals(TOKEN_EXPRESSION_PREFIX)){
            tokenExpression = ExpressionUtil.getExpressionString(tokenExpressionType);
        } else if(prefix.equals(PRE_EXPRESSION_PREFIX)){
            preExpression = ExpressionUtil.getExpressionString(preExpressionType);
        } else if(prefix.equals(POST_EXPRESSION_PREFIX)){
            postExpression = ExpressionUtil.getExpressionString(postExpressionType);
        }
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

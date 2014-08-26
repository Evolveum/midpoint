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
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
    private static final String ID_TOKEN_EXTENSION = "tokenExtensionButton";
    private static final String ID_TOKEN_STRING_FILTER_LIST = "tokenStringFilterList";
    private static final String ID_TOKEN_VARIABLE_LIST = "tokenVariableList";
    private static final String ID_TOKEN_RETURN_MULTIPLICITY = "tokenReturnMultiplicity";
    private static final String ID_TOKEN_EXPRESSION_EVALUATOR = "tokenExpressionEvaluatorList";
    private static final String ID_PRE_DESCRIPTION = "preDescription";
    private static final String ID_PRE_EXTENSION = "preExtensionButton";
    private static final String ID_PRE_STRING_FILTER_LIST = "preStringFilterList";
    private static final String ID_PRE_VARIABLE_LIST = "preVariableList";
    private static final String ID_PRE_RETURN_MULTIPLICITY = "preReturnMultiplicity";
    private static final String ID_PRE_EXPRESSION_EVALUATOR = "preExpressionEvaluatorList";
    private static final String ID_POST_DESCRIPTION = "postDescription";
    private static final String ID_POST_EXTENSION = "postExtensionButton";
    private static final String ID_POST_STRING_FILTER_LIST = "postStringFilterList";
    private static final String ID_POST_VARIABLE_LIST = "postVariableList";
    private static final String ID_POST_RETURN_MULTIPLICITY = "postReturnMultiplicity";
    private static final String ID_POST_EXPRESSION_EVALUATOR = "postExpressionEvaluatorList";

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
                ID_TOKEN_EXTENSION, ID_TOKEN_STRING_FILTER_LIST, ID_TOKEN_VARIABLE_LIST, ID_TOKEN_RETURN_MULTIPLICITY,
                ID_TOKEN_EXPRESSION_EVALUATOR);

        prepareIterationSubsectionBody(IterationSpecificationType.F_PRE_ITERATION_CONDITION.getLocalPart(), ID_PRE_DESCRIPTION,
                ID_PRE_EXTENSION, ID_PRE_STRING_FILTER_LIST, ID_PRE_VARIABLE_LIST, ID_PRE_RETURN_MULTIPLICITY,
                ID_PRE_EXPRESSION_EVALUATOR);

        prepareIterationSubsectionBody(IterationSpecificationType.F_POST_ITERATION_CONDITION.getLocalPart(), ID_POST_DESCRIPTION,
                ID_POST_EXTENSION, ID_POST_STRING_FILTER_LIST, ID_POST_VARIABLE_LIST, ID_POST_RETURN_MULTIPLICITY,
                ID_POST_EXPRESSION_EVALUATOR);
    }

    private void prepareIterationSubsectionBody(String containerValue, String descriptionId, String extensionId, String stringFilterId,
                                            String variableId, String returnMultiplicityId, String expressionEvaluatorId){
        TextArea description = new TextArea<>(descriptionId, new PropertyModel<String>(getModel(), containerValue + ".description"));
        add(description);

        AjaxLink extension = new AjaxLink(extensionId) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                extensionEditPerformed(target);
            }
        };
        add(extension);

        MultiValueTextEditPanel stringFilterList = new MultiValueTextEditPanel<StringFilterType>(stringFilterId,
                new PropertyModel<List<StringFilterType>>(getModel(), containerValue + ".stringFilter"), false, true){

            @Override
            protected IModel<String> createTextModel(final IModel<StringFilterType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        //TODO - what should we display as StringFilterType label?
                        return model.getObject().toString();
                    }
                };
            }

            @Override
            protected StringFilterType createNewEmptyItem(){
                return new StringFilterType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, StringFilterType object){
                stringFilterEditPerformed(target, object);
            }
        };
        add(stringFilterList);

        MultiValueTextEditPanel variableList = new MultiValueTextEditPanel<ExpressionVariableDefinitionType>(variableId,
                new PropertyModel<List<ExpressionVariableDefinitionType>>(getModel(), containerValue + ".variable"), false, true){

            @Override
            protected IModel<String> createTextModel(final IModel<ExpressionVariableDefinitionType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        //TODO - what should we display as ExpressionVariableDefinitionType label?
                        return model.getObject().toString();
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

        //TODO - how to edit this? How to save this?
        MultiValueTextPanel expressionEvaluatorList = new MultiValueTextPanel<>(expressionEvaluatorId,
                new PropertyModel<List<JAXBElement<?>>>(getModel(), containerValue + ".expressionEvaluator"));
        add(expressionEvaluatorList);
    }

    private void extensionEditPerformed(AjaxRequestTarget target){
        //TODO - implement this - open some ModalWindow with PrismObjectPanel with ExtensionType
    }

    private void stringFilterEditPerformed(AjaxRequestTarget target, StringFilterType object){
        //TODO - implement this - open some ModalWindow with StringFilterType editor
    }

    private void expressionVariableEditPerformed(AjaxRequestTarget target, ExpressionVariableDefinitionType object){
        //TODO - implement this - open some ModalWindow with ExpressionVariableDefinitionType editor
    }

}

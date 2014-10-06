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

package com.evolveum.midpoint.web.component.wizard.resource.component.synchronization;

import com.evolveum.midpoint.web.component.input.ExpressionEditorPanel;
import com.evolveum.midpoint.web.component.input.SearchFilterPanel;
import com.evolveum.midpoint.web.component.input.dto.SearchFilterTypeDto;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConditionalSearchFilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class ConditionalSearchFilterEditor extends SimplePanel<ConditionalSearchFilterType>{

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPRESSION_PANEL = "expressionPanel";
    private static final String ID_FILTER_CLAUSE_PANEL = "filterClausePanel";

    private IModel<ExpressionType> expression;

    public ConditionalSearchFilterEditor(String id, IModel<ConditionalSearchFilterType> model){
        super(id, model);
    }

    @Override
    public IModel<ConditionalSearchFilterType> getModel(){
        IModel<ConditionalSearchFilterType> model = super.getModel();

        if(model.getObject() == null){
            model.setObject(new ConditionalSearchFilterType());
        }

        return model;
    }

    private void loadExpression(){
        if(expression == null){
            expression = new LoadableModel<ExpressionType>(false) {

                @Override
                protected ExpressionType load() {
                    if(getModel() != null && getModel().getObject() != null){
                        return getModel().getObject().getCondition();
                    } else {
                        return new ExpressionType();
                    }
                }
            };
        }
    }

    @Override
    protected void initLayout(){
        loadExpression();

        TextArea description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(getModel(), "description"));
        add(description);

        ExpressionEditorPanel expressionEditor = new ExpressionEditorPanel(ID_EXPRESSION_PANEL, expression){

            @Override
            public void performExpressionHook(AjaxRequestTarget target){

                ExpressionType expression = null;
                if(getExpressionModel().getObject() != null && getExpressionModel().getObject().getExpressionObject() != null){
                    expression = getExpressionModel().getObject().getExpressionObject();
                }

                if(expression != null){
                    ConditionalSearchFilterEditor.this.getModel().getObject().setCondition(expression);
                }
            }

            @Override
            public String getTypeLabelKey() {
                return "ConditionalSearchFilterEditor.condition.type.label";
            }

            @Override
            public String getExpressionLabelKey() {
                return "ConditionalSearchFilterEditor.condition.label";
            }
        };
        add(expressionEditor);

        SearchFilterPanel filterClauseEditor = new SearchFilterPanel<ConditionalSearchFilterType>(ID_FILTER_CLAUSE_PANEL, getModel()){

            @Override
            public void performFilterClauseHook(AjaxRequestTarget target){
                if(model != null && model.getObject() != null && ConditionalSearchFilterEditor.this.getModel() != null){
                    SearchFilterTypeDto dto = model.getObject();
                    SearchFilterType filter = dto.getFilterObject();

                    if(filter != null){
                        ConditionalSearchFilterEditor.this.getModelObject().setFilterClauseXNode(filter.getFilterClauseXNode());
                    }
                }
            }
        };
        add(filterClauseEditor);
    }
}

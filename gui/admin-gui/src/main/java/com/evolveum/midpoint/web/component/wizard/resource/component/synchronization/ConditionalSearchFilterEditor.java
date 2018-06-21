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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.web.component.input.ExpressionEditorPanel;
import com.evolveum.midpoint.web.component.input.SearchFilterPanel;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConditionalSearchFilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class ConditionalSearchFilterEditor extends BasePanel<ConditionalSearchFilterType> {

    private static final String ID_EXPRESSION_PANEL = "expressionPanel";
    private static final String ID_FILTER_CLAUSE_PANEL = "filterClausePanel";

	// expression and filter must be non-null
    public ConditionalSearchFilterEditor(String id, NonEmptyModel<ConditionalSearchFilterType> model, PageResourceWizard parentPage) {
        super(id, model);
		initLayout(parentPage);
    }

    protected void initLayout(PageResourceWizard parentPage) {
        ExpressionEditorPanel expressionEditor = new ExpressionEditorPanel(ID_EXPRESSION_PANEL,
            new PropertyModel<>(getModel(), ConditionalSearchFilterType.F_CONDITION.getLocalPart()), parentPage) {

            @Override
            public void performExpressionHook(AjaxRequestTarget target) {
                if (getExpressionDtoModel().getObject() != null) {
					ExpressionType expression = getExpressionDtoModel().getObject().getExpressionObject();
					ConditionalSearchFilterEditor.this.getModel().getObject().setCondition(expression);
                }
            }

            @Override
            public String getTypeLabelKey() {
                return "ConditionalSearchFilterEditor.condition.type.label";
            }

			@Override
			public String getDescriptionLabelKey() {
				return "ConditionalSearchFilterEditor.condition.description.label";
			}

			@Override
			public String getUpdateLabelKey() {
				return "ConditionalSearchFilterEditor.condition.update.label";
			}

			@Override
            public String getExpressionLabelKey() {
                return "ConditionalSearchFilterEditor.condition.label";
            }
        };
        add(expressionEditor);

        SearchFilterPanel filterClauseEditor = new SearchFilterPanel<>(ID_FILTER_CLAUSE_PANEL, (NonEmptyModel<ConditionalSearchFilterType>) getModel(),
				parentPage.getReadOnlyModel());
        add(filterClauseEditor);
    }
}

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
package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class RelationDropDownChoicePanel extends BasePanel<QName> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = RelationDropDownChoicePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RELATION_DEFINITIONS = DOT_CLASS + "loadRelationDefinitions";
    private static final Trace LOGGER = TraceManager.getTrace(RelationDropDownChoicePanel.class);

    private static final String ID_INPUT = "input";

    private AreaCategoryType category;

    public RelationDropDownChoicePanel(String id, IModel<QName> model, AreaCategoryType category) {
        super(id, model);
        this.category = category;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        List<QName> choicesList = getChoicesList();
        QName defaultValue = choicesList.size() > 0 ? choicesList.get(0) : null;

        DropDownChoice<QName> input = new DropDownChoice<>(ID_INPUT, getModel() == null ? Model.of(defaultValue) : getModel(),
                Model.ofList(choicesList), getRenderer());
        input.setNullValid(false);
        input.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(input);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
    }

    protected List<QName> getChoicesList(){
        return WebComponentUtil.getCategoryRelationChoices(category, new OperationResult(OPERATION_LOAD_RELATION_DEFINITIONS), getPageBase());
    }

    protected IChoiceRenderer<QName> getRenderer(){
        return new IChoiceRenderer<QName>() {

            private static final long serialVersionUID = 1L;

            @Override
            public QName getObject(String id, IModel choices) {
                if (StringUtils.isBlank(id)) {
                    return null;
                }
                return ((List<QName>)choices.getObject()).get(Integer.parseInt(id));
            }

            @Override
            public Object getDisplayValue(QName object) {
                RelationDefinitionType def =
                        ObjectTypeUtil.findRelationDefinition(WebComponentUtil.getRelationDefinitions(
                                new OperationResult(OPERATION_LOAD_RELATION_DEFINITIONS), RelationDropDownChoicePanel.this.getPageBase()), object);
                if (def != null){
                    DisplayType display = def.getDisplay();
                    if (display != null){
                        String label = display.getLabel();
                        if (StringUtils.isNotEmpty(label)){
                            return getPageBase().createStringResource(label).getString();
                        }
                    }
                }
                return object.getLocalPart();
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        };
    }

    public QName getRelationValue() {
        return ((DropDownChoice<QName>) get(ID_INPUT)).getModelObject();
    }
}

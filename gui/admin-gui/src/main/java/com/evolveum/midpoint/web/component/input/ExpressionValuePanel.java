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
package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class ExpressionValuePanel extends BasePanel<ExpressionType>{
    private static final long serialVersionUID = 1L;

    private final static String ID_ADD_EXPRESSION_VALUE_BUTTON = "addExpressionValueButton";
    private final static String ID_LITERAL_VALUE_CONTAINER = "literalValueContainer";
    private final static String ID_LITERAL_VALUE_INPUT = "literalValueInput";
    private final static String ID_DELETE_VALUE_BUTTON = "deleteValueButton";
    private final static String ID_DELETE_TARGET_SEARCH_EXP_BUTTON = "deleteTargetSearchExpressionButton";
    private final static String ID_ASSOCIATION_TARGET_SEARCH_CONTAINER = "associationTargetSearchContainer";
    private final static String ID_TARGET_SEARCH_PATH_INPUT = "targetSearchPathInput";
    private final static String ID_TARGET_SEARCH_VALUE_INPUT = "targetSearchValueInput";

    public ExpressionValuePanel(String id, IModel<ExpressionType> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        DropdownButtonDto addValueButtonDto = new DropdownButtonDto("", "",
                getPageBase().createStringResource("ExpressionValuePanel.specifyExpression").getString(),
                createAddButtonInlineMenuItems());

        DropdownButtonPanel addValueButtonPanel = new DropdownButtonPanel(ID_ADD_EXPRESSION_VALUE_BUTTON, addValueButtonDto){
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass(){
                return "btn-sm btn-default";
            }
        };
        addValueButtonPanel.setOutputMarkupId(true);
        addValueButtonPanel.getButtonContainer().add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return  !(get(ID_LITERAL_VALUE_CONTAINER).isVisible() || get(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER).isVisible());
            }
        });
        add(addValueButtonPanel);

        initValueExpressionPanel();
        initAssociationTargetSearchExpressionPanel();
    }

    private void initValueExpressionPanel(){
        WebMarkupContainer literalValueContainer = new WebMarkupContainer(ID_LITERAL_VALUE_CONTAINER);
        literalValueContainer.setOutputMarkupId(true);
        literalValueContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return getValueExpressionValue() != null;
            }
        });
        add(literalValueContainer);

        TextPanel<String> literalValueInput = new TextPanel<String>(ID_LITERAL_VALUE_INPUT, Model.of(getValueExpressionValue()));
        literalValueInput.setOutputMarkupId(true);
        literalValueInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target){
                String inputValue = literalValueInput.getBaseFormComponent().getValue();
                ExpressionUtil.updateRawTypeEvaluatorValue(getModelObject(), inputValue, getPageBase().getPrismContext());
            }
        });
        literalValueContainer.add(literalValueInput);

        AjaxLink removeButton = new AjaxLink(ID_DELETE_VALUE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ExpressionUtil.removeEvaluatorByName(ExpressionValuePanel.this.getModelObject(), SchemaConstantsGenerated.C_VALUE);
                target.add(ExpressionValuePanel.this);
            }
        };
        literalValueContainer.add(removeButton);
    }

    private void initAssociationTargetSearchExpressionPanel(){
        WebMarkupContainer targetSearchContainer = new WebMarkupContainer(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER);
        targetSearchContainer.setOutputMarkupId(true);
        targetSearchContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

           @Override
            public boolean isVisible(){
               return getFilterValuesMap() != null && !getFilterValuesMap().isEmpty();
           }
        });
        add(targetSearchContainer);

        AjaxLink removeButton = new AjaxLink(ID_DELETE_TARGET_SEARCH_EXP_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ExpressionUtil.removeEvaluatorByName(ExpressionValuePanel.this.getModelObject(), SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH);
                target.add(ExpressionValuePanel.this);
            }
        };
        targetSearchContainer.add(removeButton);

        TextPanel<String> targetSearchFilterPathInput = new TextPanel<String>(ID_TARGET_SEARCH_PATH_INPUT, Model.of(getTargetSearchExpPathValue()));
        targetSearchFilterPathInput.setOutputMarkupId(true);
        targetSearchFilterPathInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target){
                String pathValue = targetSearchFilterPathInput.getBaseFormComponent().getValue();
                if (getModelObject() == null){
                    getModel().setObject(new ExpressionType());
                }
                ExpressionUtil.updateAssociationTargetSearchPath(getModelObject(), new ItemPathType(pathValue));
            }
        });
        targetSearchContainer.add(targetSearchFilterPathInput);

        TextPanel<String> targetSearchFilterValueInput = new TextPanel<String>(ID_TARGET_SEARCH_VALUE_INPUT, Model.of(getTargetSearchExpValue()));
        targetSearchFilterValueInput.setOutputMarkupId(true);
        targetSearchFilterValueInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target){
                String value = targetSearchFilterValueInput.getBaseFormComponent().getValue();
                if (getModelObject() == null){
                    getModel().setObject(new ExpressionType());
                }
                ExpressionUtil.updateAssociationTargetSearchValue(getModelObject(), value);
            }
        });
        targetSearchContainer.add(targetSearchFilterValueInput);

    }

    private String getValueExpressionValue(){
        JAXBElement element = ExpressionUtil.findEvaluatorByName(getModelObject(), SchemaConstantsGenerated.C_VALUE);
        if (element != null && element.getValue() instanceof RawType) {
            RawType raw = (RawType) element.getValue();
            try {
                return raw.getParsedRealValue(String.class);
            } catch (SchemaException ex) {
                return null;
            }
        }
        return null;
    }

    private String getTargetSearchExpPathValue(){
        MapXNode filterNodeMap = getFilterValuesMap();
        if (filterNodeMap == null || !filterNodeMap.containsKey(new QName("path"))){
            return null;
        }
        PrimitiveXNode<ItemPathType> pathValue = (PrimitiveXNode<ItemPathType>)filterNodeMap.get(new QName("path"));
        return pathValue != null ? pathValue.getValue().toString() : null;
    }

    private String getTargetSearchExpValue(){
        MapXNode filterNodeMap = getFilterValuesMap();
        if (filterNodeMap == null || !filterNodeMap.containsKey(new QName("value"))) {
            return null;
        }
        PrimitiveXNode valueNode = (PrimitiveXNode)filterNodeMap.get(new QName("value"));
        if (valueNode == null){
            return null;
        }
        if (valueNode.getValueParser() != null){
            return valueNode.getValueParser().getStringValue();
        } else {
            return valueNode.getValue().toString();
        }
    }

    private MapXNode getFilterValuesMap(){
        JAXBElement element = ExpressionUtil.findEvaluatorByName(getModelObject(), SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH);
        if (element != null && element.getValue() != null && element.getValue() instanceof SearchObjectExpressionEvaluatorType) {
            SearchFilterType filter = ((SearchObjectExpressionEvaluatorType) element.getValue()).getFilter();
            if (filter == null){
                return null;
            }
            MapXNode filterValue = filter.getFilterClauseXNode();
            return filterValue != null && filterValue.containsKey(new QName("equal")) ?
                    (MapXNode)filterValue.get(new QName("equal")) : null;

        }
        return null;
    }

    private List<InlineMenuItem> createAddButtonInlineMenuItems(){
        List<InlineMenuItem> menuList = new ArrayList<>();
        menuList.add(new InlineMenuItem(getPageBase().createStringResource("ExpressionValuePanel.addValueButtonDefaultTitle"),
                new InlineMenuItemAction(){
                    private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getModelObject() == null){
                    getModel().setObject(new ExpressionType());
                }
                ExpressionUtil.updateRawTypeEvaluatorValue(getModelObject(), "", getPageBase().getPrismContext());
                target.add(ExpressionValuePanel.this);
            }
                }));
        menuList.add(new InlineMenuItem(getPageBase().createStringResource("ExpressionValuePanel.addValueButtonTargetSearchTitle"),
                new InlineMenuItemAction(){
                    private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getModelObject() == null){
                    getModel().setObject(new ExpressionType());
                }
                ExpressionType expression = getModelObject();
                expression.getExpressionEvaluator().add(ExpressionUtil.createAssociationTargetSearchElement());
                target.add(ExpressionValuePanel.this);

            }
                }));
        return  menuList;
    }


}

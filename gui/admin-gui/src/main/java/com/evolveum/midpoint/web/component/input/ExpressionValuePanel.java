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
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 * For now only value/shadowRef and associationTargetSearch expression are supported to be edited.
 */

public class ExpressionValuePanel extends BasePanel<ExpressionType>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionValuePanel.class);
    private static final String DOT_CLASS = ExpressionValuePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + ".loadShadowTypeObject";

    private final static String ID_ADD_EXPRESSION_VALUE_BUTTON = "addExpressionValueButton";
    private final static String ID_SHADOW_REF_VALUE_CONTAINER = "shadowRefValueContainer";
    private final static String ID_SHADOW_REF_VALUE_INPUT = "shadowRefValueInput";
    private final static String ID_DELETE_SHADOW_REF_VALUE_BUTTON = "deleteShadowRefValueButton";
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
            public boolean isVisible(){
                return  !(get(ID_SHADOW_REF_VALUE_CONTAINER).isVisible() && !get(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER).isVisible());
            }
        });
        add(addValueButtonPanel);

        initValueExpressionPanel();
        initAssociationTargetSearchExpressionPanel();
    }

    private void initValueExpressionPanel(){
        WebMarkupContainer shadowRefValueContainer = new WebMarkupContainer(ID_SHADOW_REF_VALUE_CONTAINER);
        shadowRefValueContainer.setOutputMarkupId(true);
        shadowRefValueContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return ExpressionUtil.getShadowRefValue(getModelObject()) != null;
            }
        });
        add(shadowRefValueContainer);



        ChooseTypePanel<ShadowType> shadowRefPanel = new ChooseTypePanel<ShadowType>(ID_SHADOW_REF_VALUE_INPUT, getShadowRefValue()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectQuery getChooseQuery() {
                return new ObjectQuery();

            }

            @Override
            public Class<ShadowType> getObjectTypeClass() {
                return ShadowType.class;
            }
        };
        shadowRefValueContainer.add(shadowRefPanel);

        AjaxLink removeButton = new AjaxLink(ID_DELETE_SHADOW_REF_VALUE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ExpressionUtil.removeEvaluatorByName(ExpressionValuePanel.this.getModelObject(), SchemaConstantsGenerated.C_VALUE);
                target.add(ExpressionValuePanel.this);
            }
        };
        shadowRefValueContainer.add(removeButton);
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
                String path = targetSearchFilterPathInput.getBaseFormComponent().getValue();
                if (getModelObject() == null){
                    getModel().setObject(new ExpressionType());
                }
                try {
                    ExpressionUtil.updateAssociationTargetSearchValue(getModelObject(), path, value, getPageBase().getPrismContext());
                } catch (SchemaException ex){
                    getPageBase().getFeedbackPanel().getFeedbackMessages().add(new FeedbackMessage(ExpressionValuePanel.this, ex.getErrorTypeMessage(), 0));
                    target.add(getPageBase().getFeedbackPanel());
                }
            }
        });
        targetSearchContainer.add(targetSearchFilterValueInput);

    }

    private ObjectReferenceType getShadowRefValue() {
        ObjectReferenceType shadowRef = ExpressionUtil.getShadowRefValue(getModelObject());
        if (shadowRef == null || shadowRef.getOid() == null){
            return null;
        }
        PolyStringType shadowName = new PolyStringType(WebModelServiceUtils.resolveReferenceName(shadowRef, getPageBase()));
        shadowRef.setTargetName(shadowName);
        return shadowRef;
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
        XNode node = filterNodeMap.get(new QName("value"));
        if (node != null && node instanceof ListXNode) {
            if (((ListXNode) node).size() > 0) {
                node = ((ListXNode) node).get(0);
            }
        }
        PrimitiveXNode valueNode = (PrimitiveXNode) node;
        if (valueNode == null) {
            return null;
        }
        if (valueNode.getValueParser() != null) {
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
                ExpressionUtil.createShadowRefEvaluatorValue(getModelObject(), getPageBase().getPrismContext());
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

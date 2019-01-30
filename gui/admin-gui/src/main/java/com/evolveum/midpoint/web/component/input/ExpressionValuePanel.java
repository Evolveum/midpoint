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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + ".loadResourceTypeObject";

    private final static String ID_ADD_EXPRESSION_VALUE_BUTTON = "addExpressionValueButton";
    private final static String ID_SHADOW_REF_VALUE_CONTAINER = "shadowRefValueContainer";
    private final static String ID_SHADOW_REF_VALUE_INPUT = "shadowRefValueInput";
    private final static String ID_DELETE_SHADOW_REF_VALUE_BUTTON = "deleteShadowRefValueButton";
    private final static String ID_LITERAL_VALUE_CONTAINER = "literalValueContainer";
    private final static String ID_LITERAL_VALUE_INPUT = "literalValueInput";
    private final static String ID_DELETE_LITERAL_VALUE_BUTTON = "deleteLiteralValueButton";
    private final static String ID_DELETE_TARGET_SEARCH_EXP_BUTTON = "deleteTargetSearchExpressionButton";
    private final static String ID_ASSOCIATION_TARGET_SEARCH_CONTAINER = "associationTargetSearchContainer";
    private final static String ID_TARGET_SEARCH_PATH_INPUT = "targetSearchPathInput";
    private final static String ID_TARGET_SEARCH_VALUE_INPUT = "targetSearchValueInput";

    ConstructionType construction;
    PageBase pageBase;
    LoadableModel<List<ShadowType>> shadowsListModel;

    public ExpressionValuePanel(String id, IModel<ExpressionType> model, ConstructionType construction, PageBase pageBase){
        super(id, model);
        this.construction = construction;
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        DropdownButtonDto addValueButtonDto = new DropdownButtonDto("", "",
                createStringResource("ExpressionValuePanel.specifyExpression").getString(),
                createAddButtonInlineMenuItems());

        DropdownButtonPanel addValueButtonPanel = new DropdownButtonPanel(ID_ADD_EXPRESSION_VALUE_BUTTON, addValueButtonDto){
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass(){
                return "btn-sm btn-default";
            }
        };
        addValueButtonPanel.setOutputMarkupId(true);
        addValueButtonPanel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                boolean isShadowRefValueNull =  ExpressionUtil.getShadowRefValue(getModelObject(), pageBase.getPrismContext()) == null;

                MapXNode associationTargetSearchNode = ExpressionUtil.getAssociationTargetSearchFilterValuesMap(getModelObject());
                boolean isAssociationTargetSearchNull = associationTargetSearchNode == null || associationTargetSearchNode.isEmpty();

                List<String> literalValues = getLiteralValues();
                boolean isLiteralValueNull = literalValues == null || literalValues.isEmpty();
                return isShadowRefValueNull && isAssociationTargetSearchNull &&  isLiteralValueNull;

            }
        });
        add(addValueButtonPanel);

        initValueExpressionPanel();
        initLiteralValueExpressionPanel();
        initAssociationTargetSearchExpressionPanel();
    }

    private void initLiteralValueExpressionPanel(){
        WebMarkupContainer literalValueContainer = new WebMarkupContainer(ID_LITERAL_VALUE_CONTAINER);
        literalValueContainer.setOutputMarkupId(true);
        literalValueContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                List<String> literalValues = getLiteralValues();
                return literalValues != null && !literalValues.isEmpty();

            }
        });
        add(literalValueContainer);

        MultiValueTextPanel<String> literalValueInput = new MultiValueTextPanel<String>(ID_LITERAL_VALUE_INPUT,
                new IModel<List<String>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<String> getObject() {
                        return getLiteralValues();
                    }

                    @Override
                    public void setObject(List<String> strings) {
                        ExpressionUtil.updateLiteralExpressionValue(getModelObject(), strings, pageBase.getPrismContext());
                    }

                    @Override
                    public void detach() {

                    }
                },
                new NonEmptyLoadableModel<Boolean>(false) {
                    @NotNull
                    @Override
                    protected Boolean load() {
                        return false;
                    }
                },
                false){

            private static final long serialVersionUID = 1L;

            @Override
            protected void modelObjectUpdatePerformed(AjaxRequestTarget target, List<String> modelObject) {
                ExpressionUtil.updateLiteralExpressionValue(ExpressionValuePanel.this.getModelObject(),
                        modelObject, pageBase.getPrismContext());
            }
        };
        literalValueInput.setOutputMarkupId(true);
        literalValueContainer.add(literalValueInput);

        AjaxLink removeButton = new AjaxLink(ID_DELETE_LITERAL_VALUE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ExpressionUtil.removeEvaluatorByName(ExpressionValuePanel.this.getModelObject(), SchemaConstantsGenerated.C_VALUE);
                target.add(ExpressionValuePanel.this);
            }
        };
        literalValueContainer.add(removeButton);

    }

    private void initValueExpressionPanel(){
        WebMarkupContainer shadowRefValueContainer = new WebMarkupContainer(ID_SHADOW_REF_VALUE_CONTAINER);
        shadowRefValueContainer.setOutputMarkupId(true);
        shadowRefValueContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return ExpressionUtil.getShadowRefValue(getModelObject(), pageBase.getPrismContext()) != null;
            }
        });
        add(shadowRefValueContainer);

        shadowsListModel = new LoadableModel<List<ShadowType>>() {
            @Override
            protected List<ShadowType> load() {
                return WebComponentUtil.loadReferencedObjectList(ExpressionUtil.getShadowRefValue(ExpressionValuePanel.this.getModelObject(),
                        pageBase.getPrismContext()),
                        OPERATION_LOAD_SHADOW, pageBase);
            }
        };
        MultiValueChoosePanel<ShadowType> shadowRefPanel = new MultiValueChoosePanel<ShadowType>(ID_SHADOW_REF_VALUE_INPUT,
                shadowsListModel, Arrays.asList(ShadowType.class), false){
            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter getCustomFilter(){
                return WebComponentUtil.getShadowTypeFilterForAssociation(construction, OPERATION_LOAD_RESOURCE, pageBase);
            }

            @Override
            protected void removePerformedHook(AjaxRequestTarget target, ShadowType shadow) {
                if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                    ExpressionUtil.removeShadowRefEvaluatorValue(ExpressionValuePanel.this.getModelObject(), shadow.getOid(), pageBase.getPrismContext());
                }
            }

            @Override
            protected void choosePerformedHook(AjaxRequestTarget target, List<ShadowType> selectedList) {
                ShadowType shadow = selectedList != null && selectedList.size() > 0 ? selectedList.get(0) : null;
                if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                    ExpressionUtil.addShadowRefEvaluatorValue(ExpressionValuePanel.this.getModelObject(), shadow.getOid(), pageBase.getPrismContext());
                }
            }

            @Override
            protected void selectPerformed(AjaxRequestTarget target, List<ShadowType> chosenValues) {
                addPerformed(target, chosenValues);
            }

        };
        shadowRefPanel.setOutputMarkupId(true);







//        ChooseTypePanel<ShadowType> shadowRefPanel = new ChooseTypePanel<ShadowType>(ID_SHADOW_REF_VALUE_INPUT, getShadowRefValue()) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void executeCustomAction(AjaxRequestTarget target, ShadowType object) {
//                ExpressionUtil.addShadowRefEvaluatorValue(ExpressionValuePanel.this.getModelObject(), object == null ? null : object.getOid(),
//                        pageBase.getPrismContext());
//            }
//
//            @Override
//            protected void executeCustomRemoveAction(AjaxRequestTarget target) {
//                ExpressionUtil.removeEvaluatorByName(ExpressionValuePanel.this.getModelObject(), SchemaConstantsGenerated.C_VALUE);
//            }
//
//            @Override
//            protected ObjectQuery getChooseQuery() {
//                ObjectQuery query = new ObjectQuery();
//
//                ExpressionType expression = ExpressionValuePanel.this.getModelObject();
//                if (expression == null || construction == null){
//                    return new ObjectQuery();
//                }
//                PrismObject<ResourceType> resource = WebComponentUtil.getConstructionResource(construction, OPERATION_LOAD_RESOURCE,
//                        ExpressionValuePanel.this.getPageBase());
//                if (resource == null){
//                    return new ObjectQuery();
//                }
//
//                try {
//                    RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
//                    RefinedObjectClassDefinition oc = refinedResourceSchema.getRefinedDefinition(construction.getKind(), construction.getIntent());
//                    if (oc == null){
//                        return new ObjectQuery();
//                    }
//                    Collection<RefinedAssociationDefinition> refinedAssociationDefinitions = oc.getAssociationDefinitions();
//
//                    for (RefinedAssociationDefinition refinedAssociationDefinition : refinedAssociationDefinitions) {
//                        S_FilterEntryOrEmpty atomicFilter = QueryBuilder.queryFor(ShadowType.class, pageBase.getPrismContext());
//                        List<ObjectFilter> orFilterClauses = new ArrayList<>();
//                        refinedAssociationDefinition.getIntents()
//                                .forEach(intent -> orFilterClauses.add(atomicFilter.item(ShadowType.F_INTENT).eq(intent).buildFilter()));
//                        OrFilter intentFilter = OrFilter.createOr(orFilterClauses);
//
//                        AndFilter filter = (AndFilter) atomicFilter.item(ShadowType.F_KIND).eq(refinedAssociationDefinition.getKind()).and()
//                                .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid(), ResourceType.COMPLEX_TYPE).buildFilter();
//                        filter.addCondition(intentFilter);
//                        query.setFilter(filter);
//                    }
//                } catch (SchemaException ex) {
//                    LOGGER.error("Couldn't create query filter for ShadowType popup list: {}" , ex.getErrorTypeMessage());
//                }
//                return query;
//            }
//
//            @Override
//            public Class<ShadowType> getObjectTypeClass() {
//                return ShadowType.class;
//            }
//        };
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
               MapXNode node = ExpressionUtil.getAssociationTargetSearchFilterValuesMap(getModelObject());
               return node != null && !node.isEmpty();
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

        TextPanel<String> targetSearchFilterPathInput = new TextPanel<>(ID_TARGET_SEARCH_PATH_INPUT, Model.of(ExpressionUtil.getTargetSearchExpPathValue(getModelObject())));
        targetSearchFilterPathInput.setOutputMarkupId(true);
        targetSearchFilterPathInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target){
                String pathValue = targetSearchFilterPathInput.getBaseFormComponent().getValue();
                if (getModelObject() == null){
                    getModel().setObject(new ExpressionType());
                }
                try {
                    ExpressionUtil.updateAssociationTargetSearchPath(getModelObject(), getPrismContext().itemPathParser().asItemPathType(pathValue), getPrismContext());
                } catch (Exception ex){
                    pageBase.getFeedbackPanel().getFeedbackMessages().add(new FeedbackMessage(ExpressionValuePanel.this,
                            ex.getLocalizedMessage(), 0));
                    target.add(pageBase.getFeedbackPanel());
                }
            }
        });
        targetSearchContainer.add(targetSearchFilterPathInput);

        TextPanel<String> targetSearchFilterValueInput = new TextPanel<>(ID_TARGET_SEARCH_VALUE_INPUT, Model.of(ExpressionUtil.getTargetSearchExpValue(getModelObject())));
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
                    ExpressionUtil.updateAssociationTargetSearchValue(getModelObject(), path, value, pageBase.getPrismContext());
                } catch (SchemaException ex){
                    pageBase.getFeedbackPanel().getFeedbackMessages().add(new FeedbackMessage(ExpressionValuePanel.this, ex.getErrorTypeMessage(), 0));
                    target.add(pageBase.getFeedbackPanel());
                }
            }
        });
        targetSearchContainer.add(targetSearchFilterValueInput);

    }

    private List<InlineMenuItem> createAddButtonInlineMenuItems(){
        List<InlineMenuItem> menuList = new ArrayList<>();
        menuList.add(new InlineMenuItem(createStringResource("ExpressionValuePanel.addValueButtonDefaultTitle")) {
                         private static final long serialVersionUID = 1L;

                         @Override
                         public InlineMenuItemAction initAction() {
                             return new InlineMenuItemAction() {
                                 private static final long serialVersionUID = 1L;

                                 @Override
                                 public void onClick(AjaxRequestTarget target) {
                                     if (getModelObject() == null) {
                                         getModel().setObject(new ExpressionType());
                                     }
                                     ExpressionUtil.addShadowRefEvaluatorValue(getModelObject(), null, pageBase.getPrismContext());
                                     target.add(ExpressionValuePanel.this);
                                 }
                             };
                         }

                         @Override
                         public IModel<Boolean> getVisible() {
                             return Model.of(isAssociationExpression());
                         }
        });
        menuList.add(new InlineMenuItem(createStringResource("ExpressionValuePanel.addValueButtonTargetSearchTitle")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getModelObject() == null) {
                            getModel().setObject(new ExpressionType());
                        }
                        ExpressionType expression = getModelObject();
                        expression.getExpressionEvaluator().add(ExpressionUtil.createAssociationTargetSearchElement(getPrismContext()));
                        target.add(ExpressionValuePanel.this);

                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(isAssociationExpression());
            }
        });
        menuList.add(new InlineMenuItem(createStringResource("ExpressionValuePanel.addLiteralValueButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getModelObject() == null) {
                            getModel().setObject(new ExpressionType());
                        }
                        ExpressionUtil.updateLiteralExpressionValue(getModelObject(), Arrays.asList(""), pageBase.getPrismContext());
                        target.add(ExpressionValuePanel.this);

                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(!isAssociationExpression());
            }
        });
        return  menuList;
    }

    private List<String> getLiteralValues(){
        List<String> literalValueList = new ArrayList<>();
        try{
            return ExpressionUtil.getLiteralExpressionValues(getModelObject());
        } catch (SchemaException ex){
            LOGGER.error("Couldn't get literal expression value: {}", ex.getLocalizedMessage());
        }
        return literalValueList;
    }

    protected boolean isAssociationExpression(){
        return false;
    }
}

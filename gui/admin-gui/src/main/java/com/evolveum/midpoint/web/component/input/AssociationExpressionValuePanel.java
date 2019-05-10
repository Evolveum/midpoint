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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.DoubleButtonPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 * For now only value/shadowRef and associationTargetSearch expression are supported to be edited.
 */

public class AssociationExpressionValuePanel extends BasePanel<ExpressionType>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssociationExpressionValuePanel.class);
    private static final String DOT_CLASS = AssociationExpressionValuePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + ".loadShadowTypeObject";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + ".loadResourceTypeObject";

    private final static String ID_SHADOW_REF_EXPRESSION_BUTTON = "shadowRefExpressionButton";
    private final static String ID_ASSOCIATION_TARGET_SEARCH_EXPRESSION_BUTTON = "associationTargetSearchExpressionButton";
    private final static String ID_SHADOW_REF_VALUE_CONTAINER = "shadowRefValueContainer";
    private final static String ID_SHADOW_REF_VALUE_INPUT = "shadowRefValueInput";
    private final static String ID_ASSOCIATION_TARGET_SEARCH_CONTAINER = "associationTargetSearchContainer";
    private final static String ID_TARGET_SEARCH_PATH_INPUT = "targetSearchPathInput";
    private final static String ID_TARGET_SEARCH_VALUE_INPUT = "targetSearchValueInput";
    private static final String ID_FEEDBACK = "feedback";

    ConstructionType construction;
    LoadableModel<List<ShadowType>> shadowsListModel;
    private boolean isShadowRefPanelActive = true;

    public AssociationExpressionValuePanel(String id, IModel<ExpressionType> model, ConstructionType construction){
        super(id, model);
        this.construction = construction;

    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        isShadowRefPanelActive = ExpressionUtil.getShadowRefValue(getModelObject(), AssociationExpressionValuePanel.this.getPageBase().getPrismContext()) != null;
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupPlaceholderTag(true);
        add(feedback);

        AjaxButton shadowRefExpressionButton = new AjaxButton(ID_SHADOW_REF_EXPRESSION_BUTTON, createStringResource("ExpressionValuePanel.addValueButtonDefaultTitle")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                isShadowRefPanelActive = true;
                initShadowRefExpressionPanel();
                ajaxRequestTarget.add(AssociationExpressionValuePanel.this);
            }
        };
        shadowRefExpressionButton.setOutputMarkupId(true);
        add(shadowRefExpressionButton);

        AjaxButton associationTargetSearchExpressionButton = new AjaxButton(ID_ASSOCIATION_TARGET_SEARCH_EXPRESSION_BUTTON,
                createStringResource("ExpressionValuePanel.addValueButtonTargetSearchTitle")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                isShadowRefPanelActive = false;
                initAssociationTargetSearchExpressionPanel();
                ajaxRequestTarget.add(AssociationExpressionValuePanel.this);
            }
        };
        associationTargetSearchExpressionButton.setOutputMarkupId(true);
        add(associationTargetSearchExpressionButton);

        initShadowRefExpressionPanel();
        initAssociationTargetSearchExpressionPanel();
    }

    private void initShadowRefExpressionPanel(){
        WebMarkupContainer shadowRefValueContainer = new WebMarkupContainer(ID_SHADOW_REF_VALUE_CONTAINER);
        shadowRefValueContainer.setOutputMarkupId(true);
        shadowRefValueContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return isShadowRefPanelActive;
//                        ||
//                        ExpressionUtil.getShadowRefValue(getModelObject(), AssociationExpressionValuePanel.this.getPageBase().getPrismContext()) != null;
            }
        });
        addOrReplace(shadowRefValueContainer);

        shadowsListModel = new LoadableModel<List<ShadowType>>() {
            @Override
            protected List<ShadowType> load() {
                return WebComponentUtil.loadReferencedObjectList(ExpressionUtil.getShadowRefValue(AssociationExpressionValuePanel.this.getModelObject(),
                        AssociationExpressionValuePanel.this.getPageBase().getPrismContext()),
                        OPERATION_LOAD_SHADOW, AssociationExpressionValuePanel.this.getPageBase());
            }
        };
        MultiValueChoosePanel<ShadowType> shadowRefPanel = new MultiValueChoosePanel<ShadowType>(ID_SHADOW_REF_VALUE_INPUT,
                shadowsListModel, Arrays.asList(ShadowType.class), false){
            private static final long serialVersionUID = 1L;

            @Override
            protected void editValuePerformed(List<ShadowType> chosenValues, List<PrismReferenceValue> filterValues, AjaxRequestTarget target, boolean multiselect) {
                if (getCustomFilter() == null){
                    getFeedbackPanel().warn(createStringResource("ExpressionValuePanel.associationDefenitionsNotDefined").getString());
                    target.add(getFeedbackPanel());
                } else {
                    super.editValuePerformed(chosenValues, filterValues, target, multiselect);
                }
            }

            @Override
            protected ObjectFilter getCustomFilter(){
                return WebComponentUtil.getShadowTypeFilterForAssociation(construction, OPERATION_LOAD_RESOURCE, AssociationExpressionValuePanel.this.getPageBase());
            }

            @Override
            protected void removePerformedHook(AjaxRequestTarget target, ShadowType shadow) {
                if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                    ExpressionUtil.removeShadowRefEvaluatorValue(AssociationExpressionValuePanel.this.getModelObject(), shadow.getOid(), AssociationExpressionValuePanel.this.getPageBase().getPrismContext());
                }
            }

            @Override
            protected void choosePerformedHook(AjaxRequestTarget target, List<ShadowType> selectedList) {
                ShadowType shadow = selectedList != null && selectedList.size() > 0 ? selectedList.get(0) : null;
                if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                    ExpressionUtil.addShadowRefEvaluatorValue(AssociationExpressionValuePanel.this.getModelObject(), shadow.getOid(), AssociationExpressionValuePanel.this.getPageBase().getPrismContext());
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
//                ExpressionUtil.addShadowRefEvaluatorValue(AssociationExpressionValuePanel.this.getModelObject(), object == null ? null : object.getOid(),
//                        pageBase.getPrismContext());
//            }
//
//            @Override
//            protected void executeCustomRemoveAction(AjaxRequestTarget target) {
//                ExpressionUtil.removeEvaluatorByName(AssociationExpressionValuePanel.this.getModelObject(), SchemaConstantsGenerated.C_VALUE);
//            }
//
//            @Override
//            protected ObjectQuery getChooseQuery() {
//                ObjectQuery query = new ObjectQuery();
//
//                ExpressionType expression = AssociationExpressionValuePanel.this.getModelObject();
//                if (expression == null || construction == null){
//                    return new ObjectQuery();
//                }
//                PrismObject<ResourceType> resource = WebComponentUtil.getConstructionResource(construction, OPERATION_LOAD_RESOURCE,
//                        AssociationExpressionValuePanel.this.getPageBase());
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
    }

    private void initAssociationTargetSearchExpressionPanel(){
        WebMarkupContainer targetSearchContainer = new WebMarkupContainer(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER);
        targetSearchContainer.setOutputMarkupId(true);
        targetSearchContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

           @Override
            public boolean isVisible(){
               MapXNode node = ExpressionUtil.getAssociationTargetSearchFilterValuesMap(getModelObject());
               return !isShadowRefPanelActive;
           }
        });
        addOrReplace(targetSearchContainer);

        TextPanel<String> targetSearchFilterPathInput = new TextPanel<>(ID_TARGET_SEARCH_PATH_INPUT, Model.of(ExpressionUtil.getTargetSearchExpPathValue(getModelObject())));
        targetSearchFilterPathInput.setOutputMarkupId(true);
        targetSearchFilterPathInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target){
                String pathValue = targetSearchFilterPathInput.getBaseFormComponent().getValue();
                if (getModelObject() == null){
//                    getModel().setObject(new ExpressionType());
                }
//                try {
//                    ExpressionUtil.updateAssociationTargetSearchPath(getModelObject().getItem().getRealValue(), getPrismContext().itemPathParser().asItemPathType(pathValue), getPrismContext());
//                } catch (Exception ex){
//                    AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel().getFeedbackMessages().add(new FeedbackMessage(AssociationExpressionValuePanel.this,
//                            ex.getLocalizedMessage(), 0));
//                    target.add(AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel());
//                }
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
//                    getModel().setObject(new ExpressionType());
                }
//                try {
//                    ExpressionUtil.updateAssociationTargetSearchValue(getModelObject().getItem().getRealValue(), path, value, AssociationExpressionValuePanel.this.getPageBase().getPrismContext());
//                } catch (SchemaException ex){
//                    AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel().getFeedbackMessages().add(new FeedbackMessage(AssociationExpressionValuePanel.this, ex.getErrorTypeMessage(), 0));
//                    target.add(AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel());
//                }
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
//                                         getModel().setObject(new ExpressionType());
                                     }
//                                     ExpressionUtil.addShadowRefEvaluatorValue(getModelObject().getItem().getRealValue(), null, AssociationExpressionValuePanel.this.getPageBase().getPrismContext());
                                     target.add(AssociationExpressionValuePanel.this);
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
//                            getModel().setObject(new ExpressionType());
                        }
//                        ExpressionType expression = getModelObject().getItem().getRealValue();
//                        expression.getExpressionEvaluator().add(ExpressionUtil.createAssociationTargetSearchElement(getPrismContext()));
                        target.add(AssociationExpressionValuePanel.this);

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
//                            getModel().setObject(new ExpressionType());
                        }
//                        ExpressionUtil.updateLiteralExpressionValue(getModelObject().getItem().getRealValue(), Arrays.asList(""), AssociationExpressionValuePanel.this.getPageBase().getPrismContext());
                        target.add(AssociationExpressionValuePanel.this);

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
//        try{
//            return ExpressionUtil.getLiteralExpressionValues(getModelObject().getItem().getRealValue());
//        } catch (SchemaException ex){
//            LOGGER.error("Couldn't get literal expression value: {}", ex.getLocalizedMessage());
//        }
        return literalValueList;
    }

    protected boolean isAssociationExpression(){
        return false;
    }

    private FeedbackPanel getFeedbackPanel(){
        return (FeedbackPanel) get(ID_FEEDBACK);
    }
}

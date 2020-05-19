/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueObjectChoosePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
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

    private final static String ID_SHADOW_REF_VALUE_CONTAINER = "shadowRefValueContainer";
    private final static String ID_MULTI_SHADOW_REF_PANEL = "multiShadowRefPanel";
    private final static String ID_SHADOW_REF_VALUE_INPUT = "shadowRefValueInput";
    private final static String ID_SHADOW_ADD_BUTTON = "addButton";
    private final static String ID_SHADOW_REMOVE_BUTTON = "removeButton";
    private final static String ID_ASSOCIATION_TARGET_SEARCH_CONTAINER = "associationTargetSearchContainer";
    private final static String ID_TARGET_SEARCH_PATH_INPUT = "targetSearchPathInput";
    private final static String ID_TARGET_SEARCH_VALUE_INPUT = "targetSearchValueInput";
    private static final String ID_FEEDBACK = "feedback";

    ConstructionType construction;
//    List<ObjectReferenceType> shadowRefList;
    IModel<List<ObjectReferenceType>> shadowRefListModel;
    boolean addNewValue = false;

    public AssociationExpressionValuePanel(String id, IModel<ExpressionType> model, ConstructionType construction){
        super(id, model);
        this.construction = construction;

    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupPlaceholderTag(true);
        add(feedback);

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
                return ExpressionUtil.isShadowRefNodeExists(AssociationExpressionValuePanel.this.getModelObject());
            }
        });
        add(shadowRefValueContainer);

        MultiValueObjectChoosePanel<ObjectReferenceType> multiShadowRefPanel = new MultiValueObjectChoosePanel<ObjectReferenceType>(ID_MULTI_SHADOW_REF_PANEL,
                new LoadableModel<List<ObjectReferenceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<ObjectReferenceType> load() {
                        List<ObjectReferenceType> shadowRefList = ExpressionUtil.getShadowRefValue(AssociationExpressionValuePanel.this.getModelObject(),
                                AssociationExpressionValuePanel.this.getPageBase().getPrismContext());
                        return shadowRefList != null ? shadowRefList : new ArrayList<>();
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedTypes() {
                return Arrays.asList(ShadowType.COMPLEX_TYPE);
            }

            @Override
            protected ObjectFilter createCustomFilter(){
                return WebComponentUtil.getShadowTypeFilterForAssociation(construction, OPERATION_LOAD_RESOURCE, AssociationExpressionValuePanel.this.getPageBase());
            }

            @Override
            protected <O extends ObjectType> ObjectReferenceType createReferencableObject(O object) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(object.getOid());
                ref.setType(ShadowType.COMPLEX_TYPE);
                return ref;
            }

            @Override
            protected <O extends ObjectType> void chooseObjectPerformed(AjaxRequestTarget target, O object){
                ExpressionUtil.addShadowRefEvaluatorValue(AssociationExpressionValuePanel.this.getModelObject(), object.getOid(), AssociationExpressionValuePanel.this.getPrismContext());
            }
        };
        multiShadowRefPanel.setOutputMarkupId(true);
        shadowRefValueContainer.add(multiShadowRefPanel);
    }

    private void initAssociationTargetSearchExpressionPanel(){
        WebMarkupContainer targetSearchContainer = new WebMarkupContainer(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER);
        targetSearchContainer.setOutputMarkupId(true);
        targetSearchContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

           @Override
            public boolean isVisible(){
               return ExpressionUtil.findFirstEvaluatorByName(AssociationExpressionValuePanel.this.getModelObject(),
                       SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH) != null;
           }
        });
        add(targetSearchContainer);

        TextPanel<String> targetSearchFilterPathInput = new TextPanel<>(ID_TARGET_SEARCH_PATH_INPUT, Model.of(ExpressionUtil.getTargetSearchExpPathValue(getModelObject())));
        targetSearchFilterPathInput.setOutputMarkupId(true);
        targetSearchFilterPathInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target){
                updateAssociationTargetSearchExpressionValue(target);
//
//                String pathValue = targetSearchFilterPathInput.getBaseFormComponent().getValue();
//                if (AssociationExpressionValuePanel.this.getModelObject() == null){
//                    AssociationExpressionValuePanel.this.getModel().setObject(new ExpressionType());
//                }
//                try {
//                    ExpressionUtil.updateAssociationTargetSearchPath(AssociationExpressionValuePanel.this.getModelObject(),
//                            getPrismContext().itemPathParser().asItemPathType(pathValue), getPrismContext());
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
                updateAssociationTargetSearchExpressionValue(target);
            }
        });
        targetSearchContainer.add(targetSearchFilterValueInput);

    }

    private FeedbackPanel getFeedbackPanel(){
        return (FeedbackPanel) get(ID_FEEDBACK);
    }

    private void updateAssociationTargetSearchExpressionValue(AjaxRequestTarget target){
        String value = getAssociationSearchValueField().getBaseFormComponent().getValue();
        String path = getAssociationSearchPathField().getBaseFormComponent().getValue();
        if (getModelObject() == null){
            getModel().setObject(new ExpressionType());
        }
        try {
            ExpressionUtil.updateAssociationTargetSearchValue(getModelObject(), path, value, getPageBase().getPrismContext());
        } catch (SchemaException ex){
            AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel().getFeedbackMessages().add(new FeedbackMessage(AssociationExpressionValuePanel.this, ex.getErrorTypeMessage(), 0));
            target.add(AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel());
        }
    }

    private boolean shadowListHasNoNullValues(List<ObjectReferenceType> shadowsList){
        if (CollectionUtils.isEmpty(shadowsList)){
            return false;
        }
        for (ObjectReferenceType ref : shadowsList){
            if (ref == null){
                return false;
            }
        }
        return true;
    }

    private TextPanel<String> getAssociationSearchPathField(){
        return (TextPanel<String>) get(createComponentPath(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER, ID_TARGET_SEARCH_PATH_INPUT));
    }

    private TextPanel<String> getAssociationSearchValueField(){
        return (TextPanel<String>) get(createComponentPath(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER, ID_TARGET_SEARCH_VALUE_INPUT));
    }
}

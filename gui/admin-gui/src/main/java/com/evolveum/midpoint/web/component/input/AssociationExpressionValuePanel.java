/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueObjectChoosePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar
 * For now only value/shadowRef and associationTargetSearch expression are supported to be edited.
 */
public class AssociationExpressionValuePanel extends BasePanel<ExpressionType> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = AssociationExpressionValuePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + ".loadResourceTypeObject";

    private static final String ID_SHADOW_REF_VALUE_CONTAINER = "shadowRefValueContainer";
    private static final String ID_MULTI_SHADOW_REF_PANEL = "multiShadowRefPanel";
    private static final String ID_ASSOCIATION_TARGET_SEARCH_CONTAINER = "associationTargetSearchContainer";
    private static final String ID_TARGET_SEARCH_PATH_INPUT = "targetSearchPathInput";
    private static final String ID_TARGET_SEARCH_VALUE_INPUT = "targetSearchValueInput";
    private static final String ID_FEEDBACK = "feedback";

    private ConstructionType construction;

    public AssociationExpressionValuePanel(String id, IModel<ExpressionType> model, ConstructionType construction) {
        super(id, model);
        this.construction = construction;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupPlaceholderTag(true);
        add(feedback);

        initShadowRefExpressionPanel();
        initAssociationTargetSearchExpressionPanel();
    }

    private void initShadowRefExpressionPanel() {
        WebMarkupContainer shadowRefValueContainer = new WebMarkupContainer(ID_SHADOW_REF_VALUE_CONTAINER);
        shadowRefValueContainer.setOutputMarkupId(true);
        shadowRefValueContainer.add(new VisibleBehaviour(
                () -> showShadowRefPanel()
                        || ExpressionUtil.isShadowRefNodeExists(AssociationExpressionValuePanel.this.getModelObject())));
        add(shadowRefValueContainer);

        LoadableModel<List<ObjectReferenceType>> loadableModel = new LoadableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ObjectReferenceType> load() {
                List<ObjectReferenceType> refs = ExpressionUtil.getShadowRefValue(
                        AssociationExpressionValuePanel.this.getModelObject(),
                        getPageBase().getPrismContext());
                refs.forEach(ref -> WebModelServiceUtils.resolveReferenceName(ref, getPageBase()));

                return refs;
            }
        };
        MultiValueObjectChoosePanel<ObjectReferenceType> multiShadowRefPanel = new MultiValueObjectChoosePanel<>(
                ID_MULTI_SHADOW_REF_PANEL, loadableModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedTypes() {
                return Collections.singletonList(ShadowType.COMPLEX_TYPE);
            }

            @Override
            protected ObjectFilter createCustomFilter() {
                return WebComponentUtil.getShadowTypeFilterForAssociation(
                        construction, OPERATION_LOAD_RESOURCE,
                        AssociationExpressionValuePanel.this.getPageBase());
            }

            @Override
            protected <O extends ObjectType> ObjectReferenceType createReferencableObject(O object) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(object.getOid());
                ref.setType(ShadowType.COMPLEX_TYPE);
                return ref;
            }

            @Override
            protected <O extends ObjectType> void chooseObjectPerformed(AjaxRequestTarget target, O object) {
                ExpressionUtil.addShadowRefEvaluatorValue(
                        AssociationExpressionValuePanel.this.getModelObject(),
                        object.getOid(),
                        AssociationExpressionValuePanel.this.getPrismContext());
            }

            @Override
            protected void removeObjectPerformed(ObjectReferenceType object) {
                List<ObjectReferenceType> refs =  ExpressionUtil.getShadowRefValue(
                        AssociationExpressionValuePanel.this.getModelObject(), getPageBase().getPrismContext());
                refs.removeIf(ref -> ref.getOid().equals(object.getOid()));
                ExpressionUtil.updateShadowRefEvaluatorValue(
                        AssociationExpressionValuePanel.this.getModelObject(),
                        refs);
                getModel().detach();
            }
        };
        multiShadowRefPanel.setOutputMarkupId(true);
        shadowRefValueContainer.add(multiShadowRefPanel);
    }

    protected boolean showShadowRefPanel() {
        return false;
    }

    private void initAssociationTargetSearchExpressionPanel() {
        WebMarkupContainer targetSearchContainer = new WebMarkupContainer(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER);
        targetSearchContainer.setOutputMarkupId(true);
        targetSearchContainer.add(new VisibleBehaviour(() ->
                ExpressionUtil.findFirstEvaluatorByName(AssociationExpressionValuePanel.this.getModelObject(), SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH) != null));
        add(targetSearchContainer);

        TextPanel<String> targetSearchFilterPathInput = new TextPanel<>(ID_TARGET_SEARCH_PATH_INPUT, Model.of(ExpressionUtil.getTargetSearchExpPathValue(getModelObject())));
        targetSearchFilterPathInput.setOutputMarkupId(true);
        targetSearchFilterPathInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateAssociationTargetSearchExpressionValue(target);
            }
        });
        targetSearchContainer.add(targetSearchFilterPathInput);

        TextPanel<String> targetSearchFilterValueInput = new TextPanel<>(ID_TARGET_SEARCH_VALUE_INPUT, Model.of(ExpressionUtil.getTargetSearchExpValue(getModelObject())));
        targetSearchFilterValueInput.setOutputMarkupId(true);
        targetSearchFilterValueInput.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateAssociationTargetSearchExpressionValue(target);
            }
        });
        targetSearchContainer.add(targetSearchFilterValueInput);
    }

    private void updateAssociationTargetSearchExpressionValue(AjaxRequestTarget target) {
        String value = getAssociationSearchValueField().getBaseFormComponent().getValue();
        String path = getAssociationSearchPathField().getBaseFormComponent().getValue();
        if (getModelObject() == null) {
            getModel().setObject(new ExpressionType());
        }
        try {
            ExpressionUtil.updateAssociationTargetSearchValue(getModelObject(), path, value, getPageBase().getPrismContext());
        } catch (SchemaException ex) {
            AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel().getFeedbackMessages().add(new FeedbackMessage(AssociationExpressionValuePanel.this, ex.getErrorTypeMessage(), 0));
            target.add(AssociationExpressionValuePanel.this.getPageBase().getFeedbackPanel());
        }
    }

    private TextPanel<String> getAssociationSearchPathField() {
        //noinspection unchecked
        return (TextPanel<String>) get(createComponentPath(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER, ID_TARGET_SEARCH_PATH_INPUT));
    }

    private TextPanel<String> getAssociationSearchValueField() {
        //noinspection unchecked
        return (TextPanel<String>) get(createComponentPath(ID_ASSOCIATION_TARGET_SEARCH_CONTAINER, ID_TARGET_SEARCH_VALUE_INPUT));
    }
}

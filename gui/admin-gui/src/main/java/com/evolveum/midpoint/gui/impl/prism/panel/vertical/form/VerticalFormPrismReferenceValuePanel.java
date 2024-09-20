/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.form.CreateObjectForReferencePanel;
import com.evolveum.midpoint.gui.impl.component.form.ReferenceAutocompletePanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismReferenceValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

public class VerticalFormPrismReferenceValuePanel<R extends Referencable> extends PrismReferenceValuePanel<R> {

    private final static String INVALID_FIELD_CLASS = "is-invalid";

    public VerticalFormPrismReferenceValuePanel(String id, IModel<PrismReferenceValueWrapperImpl<R>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createDefaultPanel(String id) {
        return new ReferenceAutocompletePanel<>(id, new ItemRealValueModel<>(getModel())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter createCustomFilter() {
                return getParentWrapper().getFilter(getPageBase());
            }

            @Override
            protected boolean isEditButtonEnabled() {
                if (getModelObject() == null) {
                    return true;
                }
                return VerticalFormPrismReferenceValuePanel.this.getModelObject().isEditEnabled();
            }

            @Override
            protected Set<SearchItemType> getSpecialSearchItem() {
                return getParentWrapper().getPredefinedSearchItem();
            }

            @Override
            protected <O extends ObjectType> void choosePerformed(AjaxRequestTarget target, O object) {
                super.choosePerformed(target, object);
                getBaseFormComponent().validate();
                PageBase pageBase = findParent(PageBase.class);
                if (pageBase != null) {
                    target.add(pageBase.getFeedbackPanel());
                }
                target.add(getFeedback());
            }

            @Override
            protected boolean isButtonLabelVisible() {
                return !getSettings().isDisplayedInColumn();
            }

            @Override
            public List<QName> getSupportedTypes() {
                List<QName> targetTypeList = getParentWrapper().getTargetTypes();
                if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
                    return Arrays.asList(ObjectType.COMPLEX_TYPE);
                }
                return targetTypeList;
            }

            @Override
            protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
                if (AbstractRoleType.COMPLEX_TYPE.equals(getParentWrapper().getTargetTypeName())) {
                    return (Class<O>) RoleType.class;
                } else {
                    return super.getDefaultType(supportedTypes);
                }
            }

        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Component valuePanel = getValuePanel();

        if (valuePanel instanceof CreateObjectForReferencePanel createObjectPanel) {
            valuePanel = createObjectPanel.getReferencePanel();
        }

        if (valuePanel instanceof ReferenceAutocompletePanel<?>) {
            FormComponent baseFormComponent = ((ReferenceAutocompletePanel) valuePanel).getBaseFormComponent();

            FeedbackAlerts feedback = getFeedback();
            if (feedback != null) {
                feedback.setFilter(new ComponentFeedbackMessageFilter(baseFormComponent));
            }

            baseFormComponent.add(AttributeAppender.append("class", () -> {
                if (baseFormComponent.hasErrorMessage()) {
                    return INVALID_FIELD_CLASS;
                }
                return "";
            }));
            baseFormComponent.add(new AjaxFormComponentUpdatingBehavior("change") {

                private boolean lastValidationWasError = false;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    if (tag.getAttribute("class").contains(INVALID_FIELD_CLASS)) {
                        lastValidationWasError = true;
                    }
                }

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (lastValidationWasError) {
                        lastValidationWasError = false;
                        updateFeedbackPanel(target);
                        target.focusComponent(null);
                    }
                }

                @Override
                protected void onError(AjaxRequestTarget target, RuntimeException e) {
                    updateFeedbackPanel(target);
                }
            });
        }
    }

    protected boolean isRemoveButtonVisible() {
        if (getModelObject() != null && getModelObject().getOldValue() != null
                && getModelObject().getOldValue().getValueMetadata().isSingleValue()) {
            return false;
        }
        return super.isRemoveButtonVisible();
    }

    public void updateFeedbackPanel(AjaxRequestTarget target) {
        target.add(getFeedback());
        Component valuePanel = getValuePanel();
        if (valuePanel instanceof InputPanel) {
            FormComponent baseFormComponent = ((InputPanel) valuePanel).getBaseFormComponent();
            target.add(baseFormComponent);
            return;
        }

        if (valuePanel instanceof CreateObjectForReferencePanel createObjectPanel) {
            valuePanel = createObjectPanel.getReferencePanel();
        }

        if (valuePanel instanceof ReferenceAutocompletePanel<?>) {
            FormComponent baseFormComponent = ((ReferenceAutocompletePanel) valuePanel).getBaseFormComponent();
            target.add(baseFormComponent);
        }
    }

    protected FeedbackAlerts createFeedbackPanel(String idFeedback) {
        return new FeedbackLabels(idFeedback);
    }

    protected AjaxEventBehavior createEventBehavior() {
        return new AjaxFormComponentUpdatingBehavior("change") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateFeedbackPanel(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                updateFeedbackPanel(target);
            }
        };
    }

    protected String getCssClassForValueContainer() {
        return "";
    }
}

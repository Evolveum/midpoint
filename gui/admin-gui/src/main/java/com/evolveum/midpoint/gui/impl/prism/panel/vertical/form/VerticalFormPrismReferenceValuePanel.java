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

import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismReferenceValuePanel;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.INullAcceptingValidator;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.AbstractSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismReferencePanelContext;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class VerticalFormPrismReferenceValuePanel<R extends Referencable> extends PrismReferenceValuePanel<R> {

    public VerticalFormPrismReferenceValuePanel(String id, IModel<PrismReferenceValueWrapperImpl<R>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createDefaultPanel(String id) {
        VerticalFormPrismValueObjectSelectorPanel<R> panel
                = new VerticalFormPrismValueObjectSelectorPanel<R>(id, new ItemRealValueModel<>(getModel())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter createCustomFilter() {
                return getParentWrapper().getFilter();
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
                target.add(getPageBase().getFeedbackPanel());
                target.add(getFeedback());
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

        panel.getBaseFormComponent().add((INullAcceptingValidator) createExpressionValidator());
        return panel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        Component valuePanel = getValuePanel();
        if (valuePanel instanceof VerticalFormPrismReferenceValuePanel) {
            FormComponent baseFormComponent = ((VerticalFormPrismValueObjectSelectorPanel) valuePanel).getBaseFormComponent();
            baseFormComponent.add(AttributeAppender.append("class", () -> {
                if (baseFormComponent.hasErrorMessage()) {
                    return "is-invalid";
                }
                return "";
            }));
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

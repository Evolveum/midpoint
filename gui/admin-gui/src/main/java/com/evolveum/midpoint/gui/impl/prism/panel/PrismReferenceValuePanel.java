/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismReferencePanelContext;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

public class PrismReferenceValuePanel<R extends Referencable> extends PrismValuePanel<R, PrismReferenceWrapper<R>, PrismReferenceValueWrapperImpl<R>> {

    public PrismReferenceValuePanel(String id, IModel<PrismReferenceValueWrapperImpl<R>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected <PC extends ItemPanelContext> PC createPanelCtx(IModel<PrismReferenceWrapper<R>> wrapper) {
        PrismReferencePanelContext<R> panelCtx = new PrismReferencePanelContext<>(wrapper);
        return (PC) panelCtx;
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismReferenceWrapper<R> itemWrapper) {
        return (PV) getPrismContext().itemFactory().createReferenceValue();
    }

    @Override
    protected Component createDefaultPanel(String id) {
        ValueChoosePanel<R> panel = new ValueChoosePanel<R>(id, new ItemRealValueModel<>(getModel())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter createCustomFilter() {
                return getParentWrapper().getFilter();
            }

            @Override
            protected boolean isEditButtonEnabled() {
//
//                    //TODO only is association
//                    return getModelObject() == null;
                if (getModelObject() == null) {
                    return true;
                }
                return PrismReferenceValuePanel.this.getModelObject().isEditEnabled();

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
        getFeedback().setFilter(new ComponentFeedbackMessageFilter(panel));
        return panel;
    }


    private PrismReferenceWrapper<R> getParentWrapper() {
        return getModelObject().getParent();
    }
}

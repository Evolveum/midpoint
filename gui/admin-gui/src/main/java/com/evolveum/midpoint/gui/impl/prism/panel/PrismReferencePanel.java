/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperVisibilitySpecification;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.validation.IValidatable;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismReferencePanelContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.util.ExpressionValidator;

/**
 * @author katka
 *
 */
public class PrismReferencePanel<R extends Referencable> extends ItemPanel<PrismReferenceValueWrapperImpl<R>, PrismReferenceWrapper<R>>{

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PrismReferencePanel.class);

    private static final String ID_HEADER = "header";
    private static final String ID_VALUE = "value";
    private static final String ID_FEEDBACK = "feedback";

    public PrismReferencePanel(String id, IModel<PrismReferenceWrapper<R>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Panel createHeaderPanel() {
        return new PrismReferenceHeaderPanel<R>(ID_HEADER, getModel());
    }

    @Override
    protected Component createValuePanel(ListItem<PrismReferenceValueWrapperImpl<R>> item) {
        PrismReferenceValuePanel<R> valuePanel = new PrismReferenceValuePanel<>("value", item.getModel(), getSettings());
        item.add(valuePanel);
        return valuePanel;
//        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
//        feedback.setOutputMarkupId(true);
//        item.add(feedback);
//
//        if (componentFactory != null) {
//            PrismReferencePanelContext<?> panelCtx = new PrismReferencePanelContext<>(getModel());
//            panelCtx.setComponentId(ID_VALUE);
//            panelCtx.setParentComponent(this);
//            panelCtx.setRealValueModel((IModel)item.getModel());
//
//            Panel panel = componentFactory.createPanel(panelCtx);
//            item.add(panel);
//            return panel;
//        } else {
//            ValueChoosePanel<R> panel = new ValueChoosePanel<R>(ID_VALUE, new ItemRealValueModel<>(item.getModel())) {
//
//                private static final long serialVersionUID = 1L;
//
//                @Override
//                protected ObjectFilter createCustomFilter() {
//                    return PrismReferencePanel.this.getModelObject().getFilter();
//                }
//
//                @Override
//                protected boolean isEditButtonEnabled() {
////                    if (getModel() == null) {
////                        return true;
////                    }
////
////                    //TODO only is association
////                    return getModelObject() == null;
//                    if (item.getModel() == null || item.getModelObject() == null) {
//                        return true;
//                    }
//                    return item.getModelObject().isEditEnabled();
//
//                }
//
//                @Override
//                protected <O extends ObjectType> void choosePerformed(AjaxRequestTarget target, O object) {
//                    super.choosePerformed(target, object);
//                    getBaseFormComponent().validate();
//                    target.add(getPageBase().getFeedbackPanel());
//                    target.add(feedback);
//                }
//
//                @Override
//                public List<QName> getSupportedTypes() {
//                    List<QName> targetTypeList = PrismReferencePanel.this.getModelObject().getTargetTypes();
//                    if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
//                        return Arrays.asList(ObjectType.COMPLEX_TYPE);
//                    }
//                    return targetTypeList;
//                }
//
//                @Override
//                protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
//                    if (AbstractRoleType.COMPLEX_TYPE.equals(PrismReferencePanel.this.getModelObject().getTargetTypeName())) {
//                        return (Class<O>) RoleType.class;
//                    } else {
//                        return super.getDefaultType(supportedTypes);
//                    }
//                }
//
//            };
//
//            ExpressionValidator<String> expressionValidator = new ExpressionValidator<String>(
//                    LambdaModel.of(getModel().getObject()::getFormComponentValidator), getPageBase()) {
//
//                private static final long serialVersionUID = 1L;
//
//                @Override
//                protected Object getValueToValidate(IValidatable<String> validatable) {
//                    return item.getModelObject().getRealValue();
//                }
//            };
//            panel.getBaseFormComponent().add(expressionValidator);
//            feedback.setFilter(new ComponentFeedbackMessageFilter(panel));
//            item.add(panel);
//
//            return panel;
//        }
    }

    protected void addValue(AjaxRequestTarget target) {
        PrismReferenceWrapper<R> referenceWrapper = getModel().getObject();
         PrismReferenceValue newValue = getPrismContext().itemFactory().createReferenceValue();

        WebPrismUtil.createNewValueWrapper(referenceWrapper, newValue, getPageBase(), target);

        target.add(PrismReferencePanel.this);
    }

    @Override
    public boolean isEnabled() {
        if (getEditabilityHandler() != null && !getEditabilityHandler().isEditable(getModelObject())) {
            return false;
        }
        return !getModelObject().isReadOnly() || isLink(getModelObject());
    }

    private boolean isLink(PrismReferenceWrapper<R> iw){
        boolean isLink = false;
        if (CollectionUtils.isNotEmpty(iw.getValues()) && iw.getValues().size() == 1) {
            isLink = iw.getValues().get(0).isLink();
        }
        return isLink;
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismReferenceWrapper<R> itemWrapper) {
        return (PV) getPrismContext().itemFactory().createReferenceValue();
    }
}

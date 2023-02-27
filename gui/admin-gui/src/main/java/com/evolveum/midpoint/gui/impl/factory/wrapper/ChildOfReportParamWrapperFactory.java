/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismReferencePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractReportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author katka
 */
@Component
public class ChildOfReportParamWrapperFactory<R extends Referencable> extends PrismReferenceWrapperFactory<R> {

    private static final Trace LOGGER = TraceManager.getTrace(ChildOfReportParamWrapperFactory.class);

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return super.match(def) && parent != null && parent.getDefinition() != null
                && AbstractReportWorkDefinitionType.F_REPORT_PARAM.equivalent(parent.getDefinition().getItemName());
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public PrismReferenceWrapper<R> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
        ItemName name = def.getItemName();

        Item<?, ?> childItem = parent.getNewValue().findItem(name);

        if ((skipCreateWrapper(def, ItemStatus.NOT_CHANGED, context, childItem == null || CollectionUtils.isEmpty(childItem.getValues())))
            || !(childItem.getRealValue() instanceof RawType)) {
            LOGGER.trace("Skipping creating wrapper for non-existent item. It is not supported for {}", def);
            if (parent != null && parent.getNewValue() != null) {
                parent.getNewValue().remove(childItem);
            }
            return null;
        }

        if (childItem == null) {
            childItem = parent.getNewValue().findOrCreateItem(name);
        }

        PrismReference newItem = (PrismReference) def.instantiate();
        ObjectReferenceType parsedRealValue = ((RawType) childItem.getRealValue()).getParsedRealValue(ObjectReferenceType.class);
        newItem.add(parsedRealValue.asReferenceValue());

        PrismReferenceWrapper<R> itemWrapper = createWrapperInternal(parent, newItem, ItemStatus.NOT_CHANGED, context);
        itemWrapper.setMetadata(context.isMetadata());
        itemWrapper.setProcessProvenanceMetadata(context.isProcessMetadataFor(itemWrapper.getPath()));

        registerWrapperPanel(itemWrapper);

        List<PrismReferenceValueWrapperImpl<R>> valueWrappers = createValuesWrapper(itemWrapper, newItem, context);
        itemWrapper.getValues().addAll(valueWrappers);
        itemWrapper.setShowEmpty(context.isShowEmpty(), false);
        setupWrapper(itemWrapper);
        return itemWrapper;
    }

    @Override
    protected PrismReferenceValue createNewValue(PrismReference item) {
        PrismReferenceValue prv = getPrismContext().itemFactory().createReferenceValue();
        item.getValues().add(prv);
        return prv;
    }

    @Override
    protected PrismReferenceWrapper<R> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismReference item,
            ItemStatus status, WrapperContext ctx) {

        PrismReferenceWrapperImpl<R> referenceWrapper = new PrismReferenceWrapperImpl<>(parent, item, status);
        if (QNameUtil.match(FocusType.F_LINK_REF, item.getElementName())) {
            referenceWrapper.setOnlyForDeltaComputation(true);
        }
        return referenceWrapper;
    }

    @Override
    public PrismReferenceValueWrapperImpl<R> createValueWrapper(PrismReferenceWrapper<R> parent, PrismReferenceValue value, ValueStatus status,
            WrapperContext context) {

        return new PrismReferenceValueWrapperImpl<>(parent, value, status);
    }

    @Override
    public void registerWrapperPanel(PrismReferenceWrapper<R> wrapper) {
        getRegistry().registerWrapperPanel(wrapper.getTypeName(), PrismReferencePanel.class);
    }

    @Override
    protected void setupWrapper(PrismReferenceWrapper<R> wrapper) {
        // TODO Auto-generated method stub

    }

}

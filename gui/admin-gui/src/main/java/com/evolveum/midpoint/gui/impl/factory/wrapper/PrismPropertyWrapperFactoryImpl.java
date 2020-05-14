/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.Collection;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author katka
 */
@Component
public class PrismPropertyWrapperFactoryImpl<T> extends ItemWrapperFactoryImpl<PrismPropertyWrapper<T>, PrismPropertyValue<T>, PrismProperty<T>, PrismPropertyValueWrapper<T>>{

    private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyWrapperFactoryImpl.class);

    @Autowired protected SchemaHelper schemaHelper;

    private static final String DOT_CLASS = PrismPropertyWrapperFactoryImpl.class.getSimpleName() + ".";
    private static final String OPERATION_LOAD_LOOKUP_TABLE = DOT_CLASS + "loadLookupTable";

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismPropertyDefinition;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected PrismPropertyValue<T> createNewValue(PrismProperty<T> item) throws SchemaException {
        PrismPropertyValue<T> newValue = getPrismContext().itemFactory().createPropertyValue();
        item.add(newValue);
        return newValue;
    }

    @Override
    protected PrismPropertyWrapper<T> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismProperty<T> item,
            ItemStatus status, WrapperContext wrapperContext) {
        PrismPropertyWrapper<T> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
        propertyWrapper.setPredefinedValues(getPredefinedValues(item, wrapperContext));
        return propertyWrapper;
    }

    protected LookupTableType getPredefinedValues(PrismProperty<T> item, WrapperContext wrapperContext) {
        PrismReferenceValue valueEnumerationRef = item.getDefinition().getValueEnumerationRef();
        if (valueEnumerationRef == null) {
            return null;
        }
            //TODO: task and result from context
            Task task = wrapperContext.getTask();
            OperationResult result = wrapperContext.getResult().createSubresult(OPERATION_LOAD_LOOKUP_TABLE);
            Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
                    .createLookupTableRetrieveOptions(schemaHelper);

            try {
                PrismObject<LookupTableType> lookupTable = getModelService().getObject(LookupTableType.class, valueEnumerationRef.getOid(), options, task, result);
                result.computeStatusIfUnknown();
                return lookupTable.asObjectable();
            } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException
                    | ConfigurationException | ExpressionEvaluationException e) {
                LOGGER.error("Cannot load lookup table for {} ", item);
                result.recordFatalError("Cannot load lookupTable for " + item + ", Reason: " + e.getMessage(), e);
                //TODO throw???
            }

            return null;

    }

    @Override
    public PrismPropertyValueWrapper<T> createValueWrapper(PrismPropertyWrapper<T> parent, PrismPropertyValue<T> value,
            ValueStatus status, WrapperContext context) {
        return new PrismPropertyValueWrapper<>(parent, value, status);
    }

    @Override
    public void registerWrapperPanel(PrismPropertyWrapper<T> wrapper) {
        getRegistry().registerWrapperPanel(wrapper.getTypeName(), PrismPropertyPanel.class);
    }

    @Override
    protected void setupWrapper(PrismPropertyWrapper<T> wrapper) {

    }

}

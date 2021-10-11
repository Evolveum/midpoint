/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.Collection;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
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

    @Autowired protected ModelService modelService;
    @Autowired protected SchemaHelper schemaHelper;
    @Autowired protected TaskManager taskManager;

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
    protected PrismPropertyWrapper<T> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<T> item,
            ItemStatus status, WrapperContext wrapperContext) {
        getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), PrismPropertyPanel.class);
        PrismPropertyWrapper<T> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
        PrismReferenceValue valueEnumerationRef = item.getDefinition().getValueEnumerationRef();
        if (valueEnumerationRef != null) {
            //TODO: task and result from context
            Task task = wrapperContext.getTask();
            OperationResult result = wrapperContext.getResult().createSubresult(OPERATION_LOAD_LOOKUP_TABLE);
            Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
                    .createLookupTableRetrieveOptions(schemaHelper);

            try {
                PrismObject<LookupTableType> lookupTable = modelService.getObject(LookupTableType.class, valueEnumerationRef.getOid(), options, task, result);
                propertyWrapper.setPredefinedValues(lookupTable.asObjectable());
                result.computeStatusIfUnknown();
            } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException
                    | ConfigurationException | ExpressionEvaluationException e) {
                LOGGER.error("Cannot load lookup table for {} ", item);
                result.recordFatalError("Cannot load lookupTable for " + item + ", Reason: " + e.getMessage(), e);
                //TODO throw???
            }
        }
        return propertyWrapper;
    }

    @Override
    public PrismPropertyValueWrapper<T> createValueWrapper(PrismPropertyWrapper<T> parent, PrismPropertyValue<T> value,
            ValueStatus status, WrapperContext context) throws SchemaException {
        PrismPropertyValueWrapper<T> valueWrapper = new PrismPropertyValueWrapper<>(parent, value, status);
        return valueWrapper;
    }

    @Override
    protected void setupWrapper(PrismPropertyWrapper<T> wrapper) {

    }

}

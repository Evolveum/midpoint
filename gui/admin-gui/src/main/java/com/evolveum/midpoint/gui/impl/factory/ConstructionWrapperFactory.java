/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.ConstructionValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConstructionWrapperFactory extends AssignmentDetailsWrapperFactoryImpl<ConstructionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionWrapperFactory.class);

    @Autowired private ModelService model;

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return ConstructionType.COMPLEX_TYPE .equals(def.getTypeName());
    }


    @Override
    public PrismContainerValueWrapper<ConstructionType> createContainerValueWrapper(PrismContainerWrapper<ConstructionType> objectWrapper, PrismContainerValue<ConstructionType> objectValue, ValueStatus status, WrapperContext context) {
        ConstructionValueWrapper constructionValueWrapper = new ConstructionValueWrapper(objectWrapper, objectValue, status);
        ConstructionType constructionType = objectValue.asContainerable();
        if (constructionType.getResourceRef() != null) {
            PrismObject resource = constructionType.getResourceRef().asReferenceValue().getObject();
            if (resource != null) {
                constructionValueWrapper.setResource(resource);
                return constructionValueWrapper;
            }

        }

        ObjectReferenceType resourceRef = constructionType.getResourceRef();
        if (resourceRef.getOid() == null) {
            return constructionValueWrapper;
        }

        PrismObject<ResourceType> resource = null;
        try {
            resource = model.getObject(ResourceType.class, resourceRef.getOid(), SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), context.getTask(), context.getResult());
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.error("Problem occurred during resolving resource, reason: {}", e.getMessage(), e);
            context.getResult().recordFatalError("A problem occurred during resolving resource, reason: " + e.getMessage(), e);
            return constructionValueWrapper;
        }

        constructionValueWrapper.setResource(resource);
        return constructionValueWrapper;
    }
}

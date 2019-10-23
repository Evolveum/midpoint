/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.LayerRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.ShadowWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValuePanel;
import com.evolveum.midpoint.gui.impl.prism.ShadowWrapperImpl;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author katka
 *
 */
@Component
public class ShadowWrapperFactoryImpl extends PrismObjectWrapperFactoryImpl<ShadowType> {

    private static final transient Trace LOGGER = TraceManager.getTrace(ShadowWrapperFactoryImpl.class);

    @Autowired private GuiComponentRegistry registry;
    @Autowired private ModelService modelService;

//    public ShadowWrapper createObjectWrapper(PrismObject<ShadowType> object, ItemStatus status, WrapperContext context) throws SchemaException {
//        applySecurityConstraints(object, context);
//
//        ShadowWrapperImpl shadowWrapper = new ShadowWrapperImpl(object, status);
//        context.setShowEmpty(ItemStatus.ADDED == status ? true : false);
//        PrismContainerValueWrapper<ShadowType> valueWrapper = createValueWrapper(shadowWrapper, object.getValue(), ItemStatus.ADDED == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);
//        shadowWrapper.getValues().add(valueWrapper);
//
//        registry.registerWrapperPanel(object.getDefinition().getTypeName(), PrismObjectValuePanel.class);
//        return shadowWrapper;
//
//    }

    @Override
    public PrismObjectWrapper<ShadowType> createObjectWrapper(PrismObject<ShadowType> object, ItemStatus status) {
        return new ShadowWrapperImpl(object, status);
    }

//    @Override
//    protected void applySecurityConstraints(PrismObject<ShadowType> object, WrapperContext context) {
//
//        AuthorizationPhaseType phase = context.getAuthzPhase();
//        Task task = context.getTask();
//        OperationResult result = context.getResult();
//
//
//        try {
//            ShadowType shadow = (ShadowType) object.asObjectable();
//            ResourceShadowDiscriminator discr = new ResourceShadowDiscriminator(resolveOid(shadow.getResourceRef()),
//                    shadow.getKind(), shadow.getIntent(), shadow.getTag(), false);
//            context.setDiscriminator(discr);
//            PrismObjectDefinition<ShadowType> shadowDefinition = modelInteractionService.getEditShadowDefinition(discr, phase, task, result);
//            object.applyDefinition(shadowDefinition);
//
//            PrismObject<ResourceType> resource = resolveResource(shadow.getResourceRef(), task, result);
//            context.setResource(resource.asObjectable());
//            RefinedObjectClassDefinition objectClassDefinitionForEditing =
//                    modelInteractionService.getEditObjectClassDefinition(shadow.asPrismObject(), resource, phase, task, result);
//
//        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException
//                | CommunicationException | SecurityViolationException e) {
//            // TODO Auto-generated catch block
//            // TODO error handling
//        }
//    }

//    private String resolveOid(ObjectReferenceType ref) throws SchemaException {
//        if (ref == null) {
//            throw new SchemaException("Cannot resolve oid from null reference");
//        }
//
//        return ref.getOid();
//    }
//
//    private PrismObject<ResourceType> resolveResource(ObjectReferenceType ref, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
//        if (ref == null) {
//            throw new SchemaException("Cannot resolve oid from null reference");
//        }
//
//        return modelService.getObject(ResourceType.class, ref.getOid(), null, task, result);
//
//    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismObjectDefinition && QNameUtil.match(def.getTypeName(), ShadowType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 99;
    }

}

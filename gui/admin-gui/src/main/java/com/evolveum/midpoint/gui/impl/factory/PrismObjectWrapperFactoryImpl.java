/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katka
 */
@Component
public class PrismObjectWrapperFactoryImpl<O extends ObjectType> extends PrismContainerWrapperFactoryImpl<O> implements PrismObjectWrapperFactory<O> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismObjectWrapperFactoryImpl.class);

    private static final String DOT_CLASS = PrismObjectWrapperFactoryImpl.class.getName() + ".";

    private static final QName VIRTUAL_CONTAINER_COMPLEX_TYPE = new QName("VirtualContainerType");
    private static final QName VIRTUAL_CONTAINER = new QName("virtualContainer");

    @Autowired private GuiComponentRegistry registry;
    @Autowired protected ModelInteractionService modelInteractionService;

    public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status, WrapperContext context) throws SchemaException {

        try {
            applySecurityConstraints(object, context);
        } catch (CommunicationException | ObjectNotFoundException | SecurityViolationException | ConfigurationException | ExpressionEvaluationException e) {
            context.getResult().recordFatalError("Cannot create object wrapper for " + object + ". An error occurred: " + e.getMessage(), e);
            throw new SchemaException(e.getMessage(), e);
        }
        if (context.getObjectStatus() == null) {
            context.setObjectStatus(status);
        }
        context.setObject(object);

        Collection<VirtualContainersSpecificationType> virtualContainers = modelInteractionService.determineVirtualContainers(object, context.getTask(), context.getResult());
        context.setVirtualContainers(virtualContainers);

        PrismObjectWrapper<O> objectWrapper = createObjectWrapper(object, status);
        if (context.getReadOnly() != null) {
            objectWrapper.setReadOnly(context.getReadOnly().booleanValue());
        }
        context.setShowEmpty(ItemStatus.ADDED == status);
        objectWrapper.setExpanded(true);
        PrismContainerValueWrapper<O> valueWrapper = createValueWrapper(objectWrapper, object.getValue(), ItemStatus.ADDED == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);
        objectWrapper.getValues().add(valueWrapper);

        registry.registerWrapperPanel(object.getDefinition().getTypeName(), PrismContainerPanel.class);
        return objectWrapper;

    }

    @Override
    public PrismObjectValueWrapper<O> createContainerValueWrapper(PrismContainerWrapper<O> objectWrapper, PrismContainerValue<O> objectValue, ValueStatus status, WrapperContext context) {
        return new PrismObjectValueWrapperImpl<O>((PrismObjectWrapper<O>) objectWrapper, (PrismObjectValue<O>) objectValue, status);
    }

    public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status) {
        return new PrismObjectWrapperImpl<O>(object, status);
    }

    @Override
    public PrismContainerValueWrapper<O> createValueWrapper(PrismContainerWrapper<O> parent, PrismContainerValue<O> value, ValueStatus status, WrapperContext context) throws SchemaException {
        PrismContainerValueWrapper<O> objectValueWrapper = super.createValueWrapper(parent, value, status, context);

        if (CollectionUtils.isEmpty(context.getVirtualContainers())) {
            return objectValueWrapper;
        }

        for (VirtualContainersSpecificationType virtualContainer : context.getVirtualContainers()) {

            MutableComplexTypeDefinition mCtd = getPrismContext().definitionFactory().createComplexTypeDefinition(VIRTUAL_CONTAINER_COMPLEX_TYPE);
            DisplayType display = virtualContainer.getDisplay();

            //TODO: support full polystring -> translations could be defined directly there.
            mCtd.setDisplayName(WebComponentUtil.getOrigStringFromPoly(display.getLabel()));
            mCtd.setHelp(WebComponentUtil.getOrigStringFromPoly(display.getHelp()));
            mCtd.setRuntimeSchema(true);

            MutablePrismContainerDefinition def = getPrismContext().definitionFactory().createContainerDefinition(VIRTUAL_CONTAINER, mCtd);
            def.setMaxOccurs(1);
            def.setDisplayName(WebComponentUtil.getOrigStringFromPoly(display.getLabel()));
            def.setDynamic(true);

            ItemWrapperFactory factory = getRegistry().findWrapperFactory(def);
            if (factory == null) {
                LOGGER.warn("Cannot find factory for {}. Skipping wrapper creation.", def);
                continue;
            }

            WrapperContext ctx = context.clone();
            ctx.setVirtualItemSpecification(virtualContainer.getItem());
            ItemWrapper iw = factory.createWrapper(objectValueWrapper, def, ctx);
            iw.setVisibleOverwrite(virtualContainer.getVisibility());

            if (iw == null) {
                continue;
            }
            ((List) objectValueWrapper.getItems()).add(iw);

        }

        return objectValueWrapper;
    }

    /**
     * @param object apply security constraint to the object, update wrapper context with additional information, e.g. shadow related attributes, ...
     */
    protected void applySecurityConstraints(PrismObject<O> object, WrapperContext context) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        AuthorizationPhaseType phase = context.getAuthzPhase();
        Task task = context.getTask();
        OperationResult result = context.getResult();

        try {
            PrismObjectDefinition<O> objectDef = modelInteractionService.getEditObjectDefinition(object, phase, task, result);
            object.applyDefinition(objectDef, true);
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException
                | CommunicationException | SecurityViolationException e) {
            throw e;
        }

    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismObjectDefinition;
    }

    @Override
    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 100;
    }

}

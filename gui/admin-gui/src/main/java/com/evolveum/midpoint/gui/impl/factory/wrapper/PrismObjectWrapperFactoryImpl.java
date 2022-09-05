/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.MetadataItemProcessingSpec;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismObjectValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismObjectWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 */
@Component
public class PrismObjectWrapperFactoryImpl<O extends ObjectType> extends PrismContainerWrapperFactoryImpl<O> implements PrismObjectWrapperFactory<O> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismObjectWrapperFactoryImpl.class);

    private static final QName VIRTUAL_CONTAINER_COMPLEX_TYPE = new QName("VirtualContainerType");
    private static final QName VIRTUAL_CONTAINER = new QName("virtualContainer");

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

        setupContextWithMetadataProcessing(object, context);

        PrismObjectWrapper<O> objectWrapper = createObjectWrapper(object, status);
        if (context.getReadOnly() != null) {
            objectWrapper.setReadOnly(context.getReadOnly());
        }
        context.setShowEmpty(ItemStatus.ADDED == status);
        objectWrapper.setExpanded(true);
        PrismContainerValueWrapper<O> valueWrapper = createValueWrapper(objectWrapper, object.getValue(), ItemStatus.ADDED == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);
        setupMetadata(objectWrapper, valueWrapper, context);
        objectWrapper.getValues().add(valueWrapper);
        registerWrapperPanel(objectWrapper);

        return objectWrapper;

    }

    public void updateWrapper(PrismObjectWrapper<O> wrapper, WrapperContext context) throws SchemaException {
        try {
            applySecurityConstraints(wrapper.getObject(), context);
        } catch (CommunicationException | ObjectNotFoundException | SecurityViolationException | ConfigurationException | ExpressionEvaluationException e) {
            context.getResult().recordFatalError("Cannot create object wrapper for " + wrapper.getObject() + ". An error occurred: " + e.getMessage(), e);
            throw new SchemaException(e.getMessage(), e);
        }
        if (context.getObjectStatus() == null) {
            context.setObjectStatus(wrapper.getStatus());
        }
        context.setObject(wrapper.getObject());

        setupContextWithMetadataProcessing(wrapper.getObject(), context);

        context.setShowEmpty(ItemStatus.ADDED == wrapper.getStatus());
        wrapper.setExpanded(true);

        wrapper.getValue().getItems().clear();

        PrismContainerValueWrapper<O> valueWrapper = createValueWrapper(wrapper, wrapper.getObject().getValue(), ItemStatus.ADDED == wrapper.getStatus() ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);
        setupMetadata(wrapper, valueWrapper, context);
        wrapper.getValues().clear();
        wrapper.getValues().add(valueWrapper);

        registerWrapperPanel(wrapper);
    }

    @Override
    public PrismObjectValueWrapper<O> createContainerValueWrapper(PrismContainerWrapper<O> objectWrapper, PrismContainerValue<O> objectValue, ValueStatus status, WrapperContext context) {
        return new PrismObjectValueWrapperImpl<>((PrismObjectWrapper<O>) objectWrapper, (PrismObjectValue<O>) objectValue, status);
    }

    public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status) {
        return new PrismObjectWrapperImpl<>(object, status);
    }

    @Override
    public PrismContainerValueWrapper<O> createValueWrapper(PrismContainerWrapper<O> parent, PrismContainerValue<O> value, ValueStatus status, WrapperContext context) throws SchemaException {
        PrismContainerValueWrapper<O> objectValueWrapper = super.createValueWrapper(parent, value, status, context);

        if (CollectionUtils.isEmpty(context.getVirtualContainers())) {
            return objectValueWrapper;
        }

        for (VirtualContainersSpecificationType virtualContainer : context.getVirtualContainers()) {

            if (virtualContainer.getPath() != null) {
                continue;
            }

            MutableComplexTypeDefinition mCtd = getPrismContext().definitionFactory().createComplexTypeDefinition(VIRTUAL_CONTAINER_COMPLEX_TYPE);
            DisplayType display = virtualContainer.getDisplay();

            //TODO: support full polystring -> translations could be defined directly there.
            if (display == null || display.getLabel() == null) {
                mCtd.setDisplayName("N/A");
            } else {
                mCtd.setDisplayName(WebComponentUtil.getOrigStringFromPoly(display.getLabel()));
                mCtd.setHelp(WebComponentUtil.getOrigStringFromPoly(display.getHelp()));
            }

            mCtd.setRuntimeSchema(true);

            MutablePrismContainerDefinition<?> def = getPrismContext().definitionFactory().createContainerDefinition(VIRTUAL_CONTAINER, mCtd);
            def.setMaxOccurs(1);
            if (display != null && display.getLabel() != null) {
                def.setDisplayName(WebComponentUtil.getOrigStringFromPoly(display.getLabel()));
            }
            def.setDynamic(true);
            def.setRuntimeSchema(true);

            ItemWrapperFactory<?, ?, ?> factory = getRegistry().findWrapperFactory(def, null);
            if (factory == null) {
                LOGGER.warn("Cannot find factory for {}. Skipping wrapper creation.", def);
                continue;
            }

            WrapperContext ctx = context.clone();
            ctx.setVirtualItemSpecification(virtualContainer.getItem());

            PrismContainer<?> virtualPrismContainer = def.instantiate();
            ItemStatus virtualContainerStatus = context.getObjectStatus() != null ? context.getObjectStatus() : ItemStatus.NOT_CHANGED;


            ItemWrapper<?,?> iw = factory.createWrapper(objectValueWrapper, virtualPrismContainer, virtualContainerStatus, ctx);
            if (iw == null) {
                continue;
            }

            if (iw instanceof PrismContainerWrapper) {
                PrismContainerWrapper<?> cw = (PrismContainerWrapper<?>) iw;
                cw.setIdentifier(virtualContainer.getIdentifier());
                cw.setVirtual(true);
                if (virtualContainer.isExpanded() != null) {
                    cw.getValues().forEach(vw -> vw.setExpanded(virtualContainer.isExpanded()));
                }
            }
            iw.setVisibleOverwrite(virtualContainer.getVisibility());

            objectValueWrapper.addItem(iw);

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
            PrismObjectDefinition<O> objectDef = getModelInteractionService().getEditObjectDefinition(object, phase, task, result);
            object.applyDefinition(objectDef, true);
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException
                | CommunicationException | SecurityViolationException e) {
            LOGGER.error("Exception while applying security constraints: {}", e.getMessage(), e);
            throw e;
        }

    }

    protected void setupContextWithMetadataProcessing(PrismObject<O> object, WrapperContext context) {
         try {
            MetadataItemProcessingSpec metadataItemProcessingSpec = getModelInteractionService().getMetadataItemProcessingSpec(ValueMetadataType.F_PROVENANCE, object, context.getTask(), context.getResult());
             context.setMetadataItemProcessingSpec(metadataItemProcessingSpec);
        } catch (Throwable e) { //we don't want any surprises
            LOGGER.error("Cannot get metadata processing items, reason: " + e.getMessage(), e);
            return;
        }

    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismObjectDefinition;
    }

    @Override
    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 100;
    }

}

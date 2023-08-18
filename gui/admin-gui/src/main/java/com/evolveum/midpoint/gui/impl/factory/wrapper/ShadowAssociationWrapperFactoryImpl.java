/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ShadowAssociationWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class ShadowAssociationWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ShadowAssociationType> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAssociationWrapperFactoryImpl.class);

    @Autowired private PrismReferenceWrapperFactory referenceWrapperFactory;

    private static final String CREATE_ASSOCIATION_WRAPPER = "createAssociationWrapper";
    private static final String DOT_CLASS = ShadowAssociationWrapperFactoryImpl.class.getName() + ".";
    private static final String OPERATON_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), ShadowAssociationType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    protected PrismContainerWrapper<ShadowAssociationType> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismContainer<ShadowAssociationType> childContainer,
            ItemStatus status, WrapperContext ctx) {

        OperationResult parentResult = ctx.getResult();

        if (isNotShadow(ctx, parentResult)) {
            return super.createWrapperInternal(parent, childContainer, status, ctx);
        }

        if (isNotAssociation(childContainer)) {
            parentResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Association for " + childContainer.getComplexTypeDefinition().getTypeClass() + " is not supported");
            LOGGER.debug("Association for {} is not supported", childContainer.getComplexTypeDefinition().getTypeClass());
            return super.createWrapperInternal(parent, childContainer, status, ctx);
        }

        if (ctx.getObject() == null) {
            return super.createWrapperInternal(parent, childContainer, status, ctx);
        }
        ShadowType shadow = (ShadowType) ctx.getObject().asObjectable();
        PrismObject<ResourceType> resource = loadResource(shadow, ctx);
        if (resource == null) {
            return super.createWrapperInternal(parent, childContainer, status, ctx);
        }


        Collection<ResourceAssociationDefinition> resourceAssociationDefinitions = loadRefinedAssociationDefinitions(resource, shadow, parentResult);

        if (resourceAssociationDefinitions == null) {
            return super.createWrapperInternal(parent, childContainer, status, ctx);
        }

        ctx.setResource(resource.asObjectable());
        ctx.setRefinedAssociationDefinitions(resourceAssociationDefinitions);


        ShadowAssociationWrapperImpl associationWrapper = createShadowAssociationWrapper(parent, childContainer, shadow, status, ctx);
        if (associationWrapper == null) {
            return super.createWrapperInternal(parent, childContainer, status, ctx);
        }

        return associationWrapper;
    }

    private PrismReferenceDefinition createShadowAssocationDef(ResourceAssociationDefinition resourceAssociationDefinitions) {
        MutablePrismReferenceDefinition shadowRefDef = getPrismContext()
                .definitionFactory().createReferenceDefinition(resourceAssociationDefinitions.getName(), ObjectReferenceType.COMPLEX_TYPE);
        shadowRefDef.toMutable().setMaxOccurs(-1);
        shadowRefDef.setDisplayName(resourceAssociationDefinitions.getDisplayName());
        shadowRefDef.setTargetTypeName(ShadowType.COMPLEX_TYPE);
        return shadowRefDef;

    }

    @Override
    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        return super.canCreateWrapper(def, status, context, isEmptyValue);
    }

    private PrismObject<ResourceType> loadResource(ShadowType shadow, WrapperContext ctx) {
        String resourceOid = shadow.getResourceRef().getOid();
        if (resourceOid == null) {
            return null;
        }
        Task task = ctx.getTask();
        OperationResult result = ctx.getResult().createMinorSubresult(OPERATON_LOAD_RESOURCE);
        try {
            PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class, resourceOid, null, task, result);
            result.recordSuccess();
            return resource;
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | ConfigurationException | CommunicationException | SecurityViolationException e) {
            LOGGER.error("Cannot find resource referenced from shadow. {}", e.getMessage(), e);
            result.recordPartialError("Could not find resource referenced from shadow.", e);
            return null;
        }

    }

    private Collection<ResourceAssociationDefinition> loadRefinedAssociationDefinitions(PrismObject<ResourceType> resource, ShadowType shadow, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(CREATE_ASSOCIATION_WRAPPER);
        ResourceSchema refinedResourceSchema;
        try {
            refinedResourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Cannot get refined schema for {}, {}", resource, e.getMessage(), e);
            result.recordPartialError("Could not get fined schema for " + resource, e);
            return null;
        }
        ShadowKindType kind = shadow.getKind();
        String shadowIntent = shadow.getIntent();
        ResourceObjectDefinition objectDefinition;
        if (ShadowUtil.isKnown(kind) && ShadowUtil.isKnown(shadowIntent)) {
            objectDefinition = refinedResourceSchema.findObjectDefinition(kind, shadowIntent);
            // Note that object definition may be null here (if there's no definition for given kind+intent present)
        } else {
            // TODO what should we do in this case?
            objectDefinition = null;
        }
        if (objectDefinition == null) {
            LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Association for " + kind + "/" + shadowIntent + " not supported by resource " + resource);
            return null;
        }
        Collection<ResourceAssociationDefinition> resourceAssociationDefinitions = objectDefinition.getAssociationDefinitions();

        if (CollectionUtils.isEmpty(resourceAssociationDefinitions)) {
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Association for " + kind + "/" + shadowIntent + " not supported by resource " + resource);
            LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
            return null;
        }
        result.computeStatusIfUnknown();
        return resourceAssociationDefinitions;
    }

    private boolean isNotShadow(WrapperContext ctx, OperationResult parentResult) {
        PrismObject<?> object = ctx.getObject();
        if (object == null) {
            return true;
        }
        ObjectType objectType = (ObjectType) object.asObjectable();
        if (!(objectType instanceof ShadowType)) {
            parentResult.recordFatalError("Something very strange happened. Association container in the" + objectType.getClass().getSimpleName() + "?");
            return true;
        }
        return false;

    }

    private ShadowAssociationWrapperImpl createShadowAssociationWrapper(PrismContainerValueWrapper<?> parent,
            PrismContainer<ShadowAssociationType> childContainer, ShadowType shadow, ItemStatus status, WrapperContext ctx) {
        //we need to switch association wrapper to single value
        //the transformation will be as following:
        // we have single value ShadowAssociationType || ResourceObjectAssociationType, and from each shadowAssociationType we will create
        // property - name of the property will be association type(QName) and the value will be shadowRef
        OperationResult parentResult = ctx.getResult();

        ResourceType resource = ctx.getResource();
        PrismContainerDefinition<ShadowAssociationType> associationDefinition = childContainer.getDefinition().clone();
        associationDefinition.toMutable().setMaxOccurs(1);
        PrismContainer associationTransformed;
        try {
            associationTransformed = associationDefinition.instantiate();
        } catch (SchemaException e) {
            parentResult.recordPartialError("Association for " + shadow.getKind() + "/" + shadow.getIntent() + " cannot be created " + resource, e);
            LOGGER.error("Association for {}/{} on resource {} cannot be created: {}", shadow.getKind(), shadow.getIntent(), resource, e.getMessage(), e);
            return null;

        }
        ShadowAssociationWrapperImpl associationWrapper = new ShadowAssociationWrapperImpl(parent, associationTransformed, status);
        return associationWrapper;
    }

    @Override
    protected List<PrismContainerValueWrapper<ShadowAssociationType>> createValuesWrapper(PrismContainerWrapper<ShadowAssociationType> itemWrapper, PrismContainer<ShadowAssociationType> item, WrapperContext context) throws SchemaException {
        if (!(itemWrapper instanceof ShadowAssociationWrapperImpl)) {
            return super.createValuesWrapper(itemWrapper, item, context);
        }

        ShadowAssociationWrapperImpl associationWrapper = (ShadowAssociationWrapperImpl) itemWrapper;

        PrismContainerValueWrapper<ShadowAssociationType> shadowValueWrapper = createContainerValueWrapper(associationWrapper,
                associationWrapper.getItem().createNewValue(),
                ItemStatus.ADDED ==  associationWrapper.getStatus() ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);

        Collection<PrismReferenceWrapper> shadowReferences = new ArrayList<>();
        for (ResourceAssociationDefinition def : context.getRefinedAssociationDefinitions()) {
            PrismReference shadowAss = fillInShadowReference(def, item);

            PrismReferenceWrapper shadowReference = (PrismReferenceWrapper) referenceWrapperFactory.createWrapper(shadowValueWrapper, shadowAss, shadowAss.isEmpty() ? ItemStatus.ADDED : ItemStatus.NOT_CHANGED, context);
            shadowReference.setFilter(WebComponentUtil.createAssociationShadowRefFilter(
                    def,
                    getPrismContext(),
                    context.getResource().getOid()));
            shadowReferences.add(shadowReference);
        }

        shadowValueWrapper.getItems().addAll((Collection) shadowReferences);
        setupExpanded(shadowValueWrapper);
        return Collections.singletonList(shadowValueWrapper);

    }

    private void setupExpanded(PrismContainerValueWrapper<ShadowAssociationType> shadowValueWrapper) {
        if (CollectionUtils.isEmpty(shadowValueWrapper.getItems())) {
            shadowValueWrapper.setExpanded(false);
            return;
        }

        if (shadowValueWrapper.getItems().size() > 1) {
            shadowValueWrapper.setExpanded(true);
            return;
        }

        ItemWrapper<?, ?> itemWrapper = shadowValueWrapper.getItems().iterator().next();
        if (itemWrapper.isEmpty()) {
            shadowValueWrapper.setExpanded(false);
        } else {
            shadowValueWrapper.setExpanded(true);
        }

    }

    private PrismReference fillInShadowReference(ResourceAssociationDefinition def, PrismContainer<ShadowAssociationType> item) throws SchemaException {
        PrismReferenceDefinition shadowRefDef = createShadowAssocationDef(def);
        PrismReference shadowAss = shadowRefDef.instantiate();

        for (PrismContainerValue<ShadowAssociationType> associationValue : item.getValues()) {
            ShadowAssociationType shadowAssociation = associationValue.asContainerable();
            if (shadowAssociation.getName().equals(def.getName())
                    && associationValue.contains(ShadowAssociationType.F_SHADOW_REF)) {
                shadowAss.add(associationValue.findReference(ShadowAssociationType.F_SHADOW_REF).getValue().clone());
            }
        }

        return shadowAss;
    }

    private boolean isNotAssociation(PrismContainer<ShadowAssociationType> association) {
        return association == null || association.getDefinition() == null
                || !(association.getDefinition().getCompileTimeClass().equals(ShadowAssociationType.class));
    }

}

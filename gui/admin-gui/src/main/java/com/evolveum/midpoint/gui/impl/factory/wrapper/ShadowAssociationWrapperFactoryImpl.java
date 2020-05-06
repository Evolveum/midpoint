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

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;

import com.evolveum.midpoint.web.component.prism.ValueStatus;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
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
    protected PrismContainerWrapper<ShadowAssociationType> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<ShadowAssociationType> childContainer,
            ItemStatus status, WrapperContext ctx) {

        OperationResult parentResult = ctx.getResult();

        if (isNotShadow(parent, parentResult)) {
            return super.createWrapper(parent, childContainer, status, ctx);
        }

        if (isNotAssociation(childContainer)) {
            parentResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Association for " + childContainer.getComplexTypeDefinition().getTypeClass() + " is not supported");
            LOGGER.debug("Association for {} is not supported", childContainer.getComplexTypeDefinition().getTypeClass());
            return super.createWrapper(parent, childContainer, status, ctx);
        }

        ShadowType shadow = (ShadowType) parent.getRealValue();
        PrismObject<ResourceType> resource = loadResource(shadow, ctx);
        if (resource == null) {
            return super.createWrapper(parent, childContainer, status, ctx);
        }


        Collection<RefinedAssociationDefinition> refinedAssociationDefinitions = loadRefinedAssociationDefinitions(resource, shadow, parentResult);

        if (refinedAssociationDefinitions == null) {
            return super.createWrapper(parent, childContainer, status, ctx);
        }


        ShadowAssociationWrapperImpl associationWrapper = createShadowAssociationWrapper(parent, childContainer, resource,
                refinedAssociationDefinitions, shadow, status, parentResult);
        if (associationWrapper == null) {
            return super.createWrapper(parent, childContainer, status, ctx);
        }

        return associationWrapper;
    }

    private PrismReferenceDefinition createShadowAssocationDef(RefinedAssociationDefinition refinedAssociationDefinitions) {
        MutablePrismReferenceDefinition shadowRefDef = getPrismContext()
                .definitionFactory().createReferenceDefinition(refinedAssociationDefinitions.getName(), ObjectReferenceType.COMPLEX_TYPE);
        shadowRefDef.toMutable().setMaxOccurs(-1);
        shadowRefDef.setDisplayName(refinedAssociationDefinitions.getDisplayName());
        shadowRefDef.setTargetTypeName(ShadowType.COMPLEX_TYPE);
        return shadowRefDef;

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

    private Collection<RefinedAssociationDefinition> loadRefinedAssociationDefinitions(PrismObject<ResourceType> resource, ShadowType shadow, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(CREATE_ASSOCIATION_WRAPPER);
        RefinedResourceSchema refinedResourceSchema;
        try {
            refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
        } catch (SchemaException e) {
            LOGGER.error("Cannot get refined schema for {}, {}", resource, e.getMessage(), e);
            result.recordPartialError("Could not get fined schema for " + resource, e);
            return null;
        }
        ShadowKindType kind = shadow.getKind();
        String shadowIntent = shadow.getIntent();
        RefinedObjectClassDefinition oc = refinedResourceSchema.getRefinedDefinition(kind, shadowIntent);
        if (oc == null) {
            LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Association for " + kind + "/" + shadowIntent + " not supported by resource " + resource);
            return null;
        }
        Collection<RefinedAssociationDefinition> refinedAssociationDefinitions = oc.getAssociationDefinitions();

        if (CollectionUtils.isEmpty(refinedAssociationDefinitions)) {
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Association for " + kind + "/" + shadowIntent + " not supported by resource " + resource);
            LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
            return null;
        }
        result.computeStatusIfUnknown();
        return refinedAssociationDefinitions;
    }

    private boolean isNotShadow(PrismContainerValueWrapper<?> parent, OperationResult parentResult) {
        ObjectType objectType = (ObjectType) parent.getRealValue();
        if (!(objectType instanceof ShadowType)) {
            parentResult.recordFatalError("Something very strange happened. Association container in the" + objectType.getClass().getSimpleName() + "?");
            return true;
        }
        return false;

    }

    private ShadowAssociationWrapperImpl createShadowAssociationWrapper(PrismContainerValueWrapper<?> parent,
            PrismContainer<ShadowAssociationType> childContainer, PrismObject<ResourceType> resource,
            Collection<RefinedAssociationDefinition> refinedAssociationDefinitions,
            ShadowType shadow, ItemStatus status, OperationResult parentResult) {
        //we need to switch association wrapper to single value
        //the transformation will be as following:
        // we have single value ShadowAssociationType || ResourceObjectAssociationType, and from each shadowAssociationType we will create
        // property - name of the property will be association type(QName) and the value will be shadowRef
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
        getRegistry().registerWrapperPanel(associationTransformed.getDefinition().getTypeName(), PrismContainerPanel.class);
        ShadowAssociationWrapperImpl associationWrapper = new ShadowAssociationWrapperImpl(parent, associationTransformed, status);

        associationWrapper.setResource(resource.asObjectable());
        associationWrapper.setRefinedAssociationDefinitions(refinedAssociationDefinitions);
        return associationWrapper;
    }

    @Override
    protected <ID extends ItemDefinition<PrismContainer<ShadowAssociationType>>> List<PrismContainerValueWrapper<ShadowAssociationType>> createValuesWrapper(PrismContainerWrapper<ShadowAssociationType> itemWrapper, PrismContainer<ShadowAssociationType> item, WrapperContext context) throws SchemaException {
        if (!(itemWrapper instanceof ShadowAssociationWrapperImpl)) {
            return super.createValuesWrapper(itemWrapper, item, context);
        }

        ShadowAssociationWrapperImpl associationWrapper = (ShadowAssociationWrapperImpl) itemWrapper;

        PrismContainerValueWrapper<ShadowAssociationType> shadowValueWrapper = createContainerValueWrapper(associationWrapper,
                associationWrapper.getItem().createNewValue(),
                ItemStatus.ADDED ==  associationWrapper.getStatus() ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);

        Collection<PrismReferenceWrapper> shadowReferences = new ArrayList<>();
        for (RefinedAssociationDefinition def : associationWrapper.getRefinedAssociationDefinitions()) {
            PrismReference shadowAss = fillInShadowReference(def, item);

            PrismReferenceWrapper shadowReference = (PrismReferenceWrapper) referenceWrapperFactory.createCompleteWrapper(shadowValueWrapper, shadowAss, shadowAss.isEmpty() ? ItemStatus.ADDED : ItemStatus.NOT_CHANGED, context);
            shadowReference.setFilter(WebComponentUtil.createAssociationShadowRefFilter(def,
                    getPrismContext(), associationWrapper.getResource().getOid()));
            shadowReferences.add(shadowReference);
        }

        shadowValueWrapper.getItems().addAll((Collection) shadowReferences);
        return Collections.singletonList(shadowValueWrapper);

    }

    private PrismReference fillInShadowReference(RefinedAssociationDefinition def, PrismContainer<ShadowAssociationType> item) throws SchemaException {
        PrismReferenceDefinition shadowRefDef = createShadowAssocationDef(def);
        PrismReference shadowAss = shadowRefDef.instantiate();

        for (PrismContainerValue<ShadowAssociationType> associationValue : item.getValues()) {
            ShadowAssociationType shadowAssociation = associationValue.asContainerable();
            if (shadowAssociation.getName().equals(def.getName())) {
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

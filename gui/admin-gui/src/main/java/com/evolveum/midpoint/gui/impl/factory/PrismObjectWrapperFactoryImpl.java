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
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
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
    private static final String OPERATION_DETERMINE_VIRTUAL_CONTAINERS = DOT_CLASS + "determineVirtualContainers";

    private static final QName VIRTUAL_CONTAINER_COMPLEX_TYPE = new QName("VirtualContainerType");
    private static final QName VIRTUAL_CONTAINER = new QName("virtualContainer");

    @Autowired private GuiComponentRegistry registry;
    @Autowired protected ModelInteractionService modelInteractionService;

    public PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status, WrapperContext context) throws SchemaException {

        try {
            applySecurityConstraints(object, context);
        } catch (CommunicationException | ObjectNotFoundException | SecurityViolationException | ConfigurationException | ExpressionEvaluationException e) {
            context.getResult().recordFatalError("Cannot create object wrapper for " + object + ". An eeror occured: " + e.getMessage(), e);
            throw new SchemaException(e.getMessage(), e);
        }
        if (context.getObjectStatus() == null) {
            context.setObjectStatus(status);
        }

        List<VirtualContainersSpecificationType> virtualContainers = determineVirtualContainers(object, context);
        context.setVirtualContainers(virtualContainers);

        PrismObjectWrapper<O> objectWrapper = createObjectWrapper(object, status);
        if (context.getReadOnly() != null) {
            objectWrapper.setReadOnly(context.getReadOnly().booleanValue());
        }
        context.setShowEmpty(ItemStatus.ADDED == status);
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

            if (iw == null) {
                continue;
            }
            ((List) objectValueWrapper.getItems()).add(iw);

        }

        return objectValueWrapper;
    }

    private List<VirtualContainersSpecificationType> determineVirtualContainers(PrismObject<O> object, WrapperContext context) {

        OperationResult result = context.getResult().createMinorSubresult(OPERATION_DETERMINE_VIRTUAL_CONTAINERS);
        if (AssignmentHolderType.class.isAssignableFrom(object.getCompileTimeClass())) {

            try {
                ArchetypePolicyType archetypePolicyType = modelInteractionService.determineArchetypePolicy((PrismObject) object, result);
                if (archetypePolicyType != null) {
                    ArchetypeAdminGuiConfigurationType archetyAdminGui = archetypePolicyType.getAdminGuiConfiguration();
                    if (archetyAdminGui != null) {
                        GuiObjectDetailsPageType guiDetails = archetyAdminGui.getObjectDetails();
                        if (guiDetails != null) {
                            return guiDetails.getContainer();
                        }
                    }
                }
            } catch (SchemaException | ConfigurationException e) {
                LOGGER.error("Cannot determine virtual containers for {}, reason: {}", object, e.getMessage(), e);
                result.recordPartialError("Cannot determine virtual containers for " + object + ", reason: " + e.getMessage(), e);
                return null;
            }

        }

        QName objectType = object.getDefinition().getTypeName();
        try {
            CompiledUserProfile userProfile = modelInteractionService.getCompiledUserProfile(context.getTask(), context.getResult());
            GuiObjectDetailsSetType objectDetailsSetType = userProfile.getObjectDetails();
            if (objectDetailsSetType == null) {
                result.recordSuccess();
                return null;
            }
            List<GuiObjectDetailsPageType> detailsPages = objectDetailsSetType.getObjectDetailsPage();
            for (GuiObjectDetailsPageType detailsPage : detailsPages) {
                if (objectType == null) {
                    LOGGER.trace("Object type is not known, skipping considering custom details page settings.");
                    continue;
                }
                if (detailsPage.getType() == null) {
                    LOGGER.trace("Object type for details page {} not know, skipping considering custom details page settings.", detailsPage);
                    continue;
                }

                if (QNameUtil.match(objectType, detailsPage.getType())) {
                    result.recordSuccess();
                    return detailsPage.getContainer();
                }
            }
            result.recordSuccess();
            return null;
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            LOGGER.error("Cannot determine virtual containers for {}, reason: {}", objectType, e.getMessage(), e);
            result.recordPartialError("Cannot determine virtual containers for " + objectType + ", reason: " + e.getMessage(), e);
            return null;
        }

    }

    /**
     * @param object apply security constraint to the object, update wrapper context with additional information, e.g. shadow related attributes, ...
     */
    protected void applySecurityConstraints(PrismObject<O> object, WrapperContext context) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        AuthorizationPhaseType phase = context.getAuthzPhase();
        Task task = context.getTask();
        OperationResult result = context.getResult();

        ObjectReferenceType archetypesToBeAdded = null;
        if (AssignmentHolderType.class.isAssignableFrom(object.getCompileTimeClass())) {
            archetypesToBeAdded = listArchetypes((PrismObject) object);
            if (archetypesToBeAdded != null) {
                applyArchetypes((PrismObject) object, archetypesToBeAdded);
            }
        }

        try {
            PrismObjectDefinition<O> objectDef = modelInteractionService.getEditObjectDefinition(object, phase, task, result);
            object.applyDefinition(objectDef, true);
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException
                | CommunicationException | SecurityViolationException e) {
            throw e;
        } finally {
            if (archetypesToBeAdded != null) {
                cleanupArchetypesToBeAdded((PrismObject) object, archetypesToBeAdded);
            }
        }

    }

    private <AH extends AssignmentHolderType> void applyArchetypes(PrismObject<AH> object, ObjectReferenceType ref) throws SchemaException {
        PrismReference archetypeRef = object.findReference(AssignmentHolderType.F_ARCHETYPE_REF);

        if (archetypeRef == null) {
            archetypeRef = object.findOrCreateReference(AssignmentHolderType.F_ARCHETYPE_REF);
        }

        if (CollectionUtils.isNotEmpty(archetypeRef.getValues())) {
            throw new SchemaException("Cannot apply new archetype to the object with already assigned archetype.");
        }

        archetypeRef.getValues().add(ref.asReferenceValue());
    }

    private <AH extends AssignmentHolderType> void cleanupArchetypesToBeAdded(PrismObject<AH> object, ObjectReferenceType ref) throws SchemaException {
        //Now we expect thet object can have just one archetyperef, so if something was added, we just remove it.

        PrismReference archetype = object.findReference(AssignmentHolderType.F_ARCHETYPE_REF);
        if (archetype == null) {
            return;
        }

        if (archetype.getValues() != null && archetype.getValues().size() > 1) {
            throw new SchemaException("More then one archetype ref found, but this is not supported.");
        }

        object.removeReference(AssignmentHolderType.F_ARCHETYPE_REF);
    }

    private <AH extends AssignmentHolderType> ObjectReferenceType listArchetypes(PrismObject<AH> object) throws SchemaException {
        PrismContainer<AssignmentType> assignmentContainer = object.findContainer(AssignmentHolderType.F_ASSIGNMENT);
        Collection<AssignmentType> assignments = null;
        if (assignmentContainer != null) {
            assignments = assignmentContainer.getRealValues();
        }

        if (CollectionUtils.isEmpty(assignments)) {
            return null;
        }

        List<AssignmentType> archetypeAssignments = assignments.stream().filter(a -> a.getTargetRef() != null && QNameUtil.match(ArchetypeType.COMPLEX_TYPE, a.getTargetRef().getType())).collect(Collectors.toList());

        if (archetypeAssignments.size() > 1) {
            throw new SchemaException("More then one archetype assignment not supported.");
        }

        if (CollectionUtils.isEmpty(archetypeAssignments)) {
            return null;
        }

        AssignmentType archetypeAssignment = archetypeAssignments.iterator().next();

        PrismReference existingArchetypeRefs = object.findReference(AssignmentHolderType.F_ARCHETYPE_REF);
        if (existingArchetypeRefs == null || CollectionUtils.isEmpty(existingArchetypeRefs.getRealValues())) {
            return archetypeAssignment.getTargetRef();
        }

        return null;

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

/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.model.api.util.ResourceUtils;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.focus.component.FocusProjectionsPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Util methods for working with shadows and resources.
 */
public class ProvisioningObjectsUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningObjectsUtil.class);

    private static final String DOT_CLASS = ProvisioningObjectsUtil.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    @NotNull
    public static String determineDisplayNameForDefinition(ShadowType shadow, PageBase pageBase) {
        ResourceObjectDefinition def = loadResourceObjectDefinition(shadow, pageBase);
        if (def == null) {
            return ""; //TODO do we want something like N/A here?
        }
        if (def.getDisplayName() != null) {
            return def.getDisplayName();
        }
        return shadow.getKind() + "/" + shadow.getIntent();
    }

    private static ResourceObjectDefinition loadResourceObjectDefinition(ShadowType shadow, PageBase pageBase) {
        ResourceType resource = loadResource(shadow, pageBase);
        ResourceSchema resourceSchema;
        try {
            resourceSchema = getRefinedSchema(resource, pageBase);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get refined schema for {}", e, resource);
            return null;
        }

        try {
            return resourceSchema.findObjectDefinition(shadow.getKind(), shadow.getIntent(), shadow.getObjectClass());
        } catch (Exception e) {
            LOGGER.debug(
                    "Couldn't get ResourceObjectDefinition for kind: " + shadow.getKind() + ", intent: " + shadow.getIntent()
                            + " in resource schema " + resourceSchema,
                    e);
            return null;
        }
    }

    private static ResourceType loadResource(ShadowType shadow, PageBase pageBase) {
        if (shadow == null) {
            return null;
        }
        ObjectReferenceType resourceRef = shadow.getResourceRef();
        if (resourceRef == null) {
            LOGGER.warn("No resource in shadow, something is wrong.");
            return null;
        }
        Task task = pageBase.createSimpleTask(OPERATION_LOAD_RESOURCE);
        PrismObject<ResourceType> resource = WebModelServiceUtils.resolveReferenceNoFetch(resourceRef, pageBase, task, task.getResult());
        if (resource == null) {
            LOGGER.warn("Couldn't load resource with oid {}", resourceRef.getOid());
            return null;
        }
        return resource.asObjectable();
    }

    public static ResourceSchema getRefinedSchema(ResourceType resource, PageBase pageBase) throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource.asPrismObject(), LayerType.PRESENTATION);
        if (refinedSchema == null) { //TODO why do we need this?
            Task task = pageBase.createSimpleTask(FocusProjectionsPanel.class.getSimpleName() + ".loadResource");
            OperationResult result = task.getResult();
            PrismObject<ResourceType> resourcePrism = WebModelServiceUtils.loadObject(ResourceType.class, resource.getOid(), pageBase, task, result);
            if (resourcePrism == null) {
                pageBase.error(pageBase.getString("pageAdminFocus.message.couldntCreateAccountNoSchema",
                        resource.getName()));
                return null;
            }
            resource = resourcePrism.asObjectable();
            result.recomputeStatus();

            refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource, LayerType.PRESENTATION);

            if (refinedSchema == null) {
                pageBase.error(pageBase.getString("pageAdminFocus.message.couldntCreateAccountNoSchema",
                        resource.getName()));
            }
        }
        return refinedSchema;
    }

    public static IModel<String> getResourceLabelModel(ShadowType shadow, PageBase pageBase) {
        return pageBase.createStringResource("DisplayNamePanel.resource",
                WebComponentUtil.getReferencedObjectDisplayNamesAndNames(shadow.getResourceRef(), false));
    }

    public static IModel<String> getResourceAttributesLabelModel(ShadowType shadow, PageBase pageBase) {
        StringBuilder sb = new StringBuilder();
        if (shadow != null) {
            if (shadow.getObjectClass() != null && !StringUtils.isBlank(shadow.getObjectClass().getLocalPart())) {
                sb.append(pageBase.createStringResource("DisplayNamePanel.objectClass", shadow.getObjectClass().getLocalPart()).getString());
            }
            if (shadow.getKind() != null && !StringUtils.isBlank(shadow.getKind().name())) {
                sb.append(", ");
                sb.append(pageBase.createStringResource("DisplayNamePanel.kind", shadow.getKind().name()).getString());
            }

            if (!StringUtils.isBlank(shadow.getIntent())) {
                sb.append(", ");
                sb.append(pageBase.createStringResource("DisplayNamePanel.intent", shadow.getIntent()).getString());
            }

            if (!StringUtils.isBlank(shadow.getTag())) {
                sb.append(", ");
                sb.append(pageBase.createStringResource("DisplayNamePanel.tag", shadow.getTag()).getString());
            }
            return Model.of(sb.toString());
        }
        return Model.of("");
    }

    public static String getPendingOperationsLabels(ShadowType shadow, BasePanel panel) {
        if (shadow == null || shadow.getPendingOperation().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        List<PendingOperationType> operations = shadow.getPendingOperation();
        sb.append("\n").append(panel.getString("DisplayNamePanel.pendingOperation")).append(":");
        boolean isFirst = true;
        for (PendingOperationType operation : operations) {
            if (operation != null) {
                if (!isFirst) {
                    sb.append(", ");
                } else {
                    sb.append(" ");
                }
                sb.append(getPendingOperationLabel(operation, panel));
                isFirst = false;
            }
        }
        return sb.toString();
    }

    public static String getPendingOperationLabel(PendingOperationType realValue, BasePanel<?> panel) {
        StringBuilder sb = new StringBuilder();
        boolean empty = true;
        ObjectDeltaType delta = realValue.getDelta();
        if (delta != null && delta.getChangeType() != null) {
            sb.append(panel.getString(delta.getChangeType()));
            empty = false;
        }
        PendingOperationTypeType type = realValue.getType();
        if (type != null) {
            if (!empty) {
                sb.append(" ");
            }
            sb.append("(").append(panel.getString(type)).append(")");
        }
        OperationResultStatusType rStatus = realValue.getResultStatus();
        PendingOperationExecutionStatusType eStatus = realValue.getExecutionStatus();
        if (!empty) {
            sb.append(" ");
        }
        sb.append(panel.getString("PendingOperationType.label.status")).append(": ");
        if (rStatus == null) {
            sb.append(panel.getString(eStatus));
        } else {
            sb.append(panel.getString(rStatus));
        }
        return sb.toString();
    }

    public static String filterNonDeadProjections(List<ShadowWrapper> projectionWrappers) {
        if (projectionWrappers == null) {
            return "0";
        }

        int nonDead = 0;
        for (ShadowWrapper projectionWrapper : projectionWrappers) {
            if (projectionWrapper.isDead()) {
                continue;
            }
            nonDead++;
        }
        return Integer.toString(nonDead);
    }

    public static String countLinkFroNonDeadShadows(Collection<ObjectReferenceType> refs) {
        return Integer.toString(countLinkForNonDeadShadows(refs));
    }

    public static int countLinkForNonDeadShadows(Collection<ObjectReferenceType> refs) {
        int count = 0;
        for (ObjectReferenceType ref : refs) {
            if (QNameUtil.match(ref.getRelation(), SchemaConstants.ORG_RELATED)) {
                continue;
            }
            count++;
        }
        return count;
    }

    public static int countLinkForDeadShadows(Collection<ObjectReferenceType> refs) {
        int count = 0;
        for (ObjectReferenceType ref : refs) {
            if (QNameUtil.match(ref.getRelation(), SchemaConstants.ORG_RELATED)) {
                count++;
            }
        }
        return count;
    }

    public static ObjectFilter getShadowTypeFilterForAssociation(
            ConstructionType construction, String operation, PageBase pageBase) {
        return getShadowTypeFilterForAssociation(construction, null, operation, pageBase);
    }

    public static ObjectFilter getShadowTypeFilterForAssociation(
            ConstructionType construction, ItemName association, String operation, PageBase pageBase) {
        if (construction == null) {
            return null;
        }
        PrismObject<ResourceType> resource = getConstructionResource(construction, operation, pageBase);
        if (resource == null) {
            return null;
        }

        try {
            ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
            ResourceObjectDefinition oc = schema.findDefinitionForConstruction(construction);
            return getShadowTypeFilterForAssociation(oc, association);
        } catch (SchemaException | ConfigurationException ex) {
            LOGGER.error("Couldn't create query filter for ShadowType for association: {}", ex.getErrorTypeMessage());
        }
        return null;
    }

    public static ObjectFilter getShadowTypeFilterForAssociation(ResourceObjectDefinition oc, ItemName association) {
        if (oc == null) {
            return null;
        }
        var shadowAssociationDefinitions = oc.getAssociationDefinitions();

        List<ObjectFilter> filters = new ArrayList<>();
        for (ShadowAssociationDefinition shadowAssociationDefinition : shadowAssociationDefinitions) {
            if (association != null && !shadowAssociationDefinition.getItemName().equivalent(association)) {
                continue;
            }
            ObjectFilter filter = shadowAssociationDefinition.createTargetObjectsFilter(true);
            filters.add(filter);
        }
        PrismContext prismContext = PrismContext.get();
        return prismContext.queryFactory().createOr(filters);
    }

    /** Creates a filter that provides all shadows eligible as the target value for this association. */
    public static ObjectFilter createAssociationShadowRefFilter(
            ShadowReferenceAttributeDefinition shadowReferenceAttributeDefinition,
            PrismContext prismContext, String resourceOid) {
        return shadowReferenceAttributeDefinition.createTargetObjectsFilter(true);
    }

    public static ItemVisibility checkShadowActivationAndPasswordVisibility(ItemWrapper<?, ?> itemWrapper,
            ShadowType shadowType) {
        ObjectReferenceType resourceRef = shadowType.getResourceRef();
        if (resourceRef == null) {
            //TODO: what to return if we don't have resource available?
            return ItemVisibility.AUTO;
        }
        PrismObject<ResourceType> resource = resourceRef.asReferenceValue().getObject();
        if (resource == null) {
            //TODO: what to return if we don't have resource available?
            return ItemVisibility.AUTO;
        }
        ResourceType resourceType = resource.asObjectable();

        ResourceObjectDefinition ocd = null;

        try {
            ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
            ocd = resourceSchema.findDefinitionForShadow(shadowType);
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Cannot find refined definition for {} in {}", shadowType, resource);
        }
        ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType =
                ResourceTypeUtil.findObjectTypeDefinition(resource, shadowType.getKind(), shadowType.getIntent());

        if (SchemaConstants.PATH_ACTIVATION.equivalent(itemWrapper.getPath())) {
            if (ResourceTypeUtil.isActivationCapabilityEnabled(resourceType, resourceObjectTypeDefinitionType)) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(itemWrapper.getPath())) {
            if (ResourceTypeUtil.isActivationStatusCapabilityEnabled(resourceType, resourceObjectTypeDefinitionType)) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS.equivalent(itemWrapper.getPath())) {
            if (ResourceTypeUtil.isActivationLockoutStatusCapabilityEnabled(resourceType, resourceObjectTypeDefinitionType)) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (SchemaConstants.PATH_ACTIVATION_VALID_FROM.equivalent(itemWrapper.getPath())) {
            if (ResourceTypeUtil.isActivationValidityFromCapabilityEnabled(resourceType, resourceObjectTypeDefinitionType)) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (SchemaConstants.PATH_ACTIVATION_VALID_TO.equivalent(itemWrapper.getPath())) {
            if (ResourceTypeUtil.isActivationValidityToCapabilityEnabled(resourceType, resourceObjectTypeDefinitionType)) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (SchemaConstants.PATH_PASSWORD.equivalent(itemWrapper.getPath())) {
            if (ResourceTypeUtil.isPasswordCapabilityEnabled(resourceType, resourceObjectTypeDefinitionType)) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (ShadowType.F_ASSOCIATIONS.equivalent(itemWrapper.getPath())) {
            if (ocd != null && CollectionUtils.isNotEmpty(ocd.getReferenceAttributeDefinitions())) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        return ItemVisibility.AUTO;

    }

    public static boolean isActivationSupported(ShadowType shadowType, IModel<ResourceType> resourceModel) {
        ResourceType resource = resourceModel.getObject();
        if (resource == null) {
            return true; //TODO should be true?
        }

        ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType = ResourceTypeUtil.findObjectTypeDefinition(resource.asPrismObject(), shadowType.getKind(), shadowType.getIntent());

        return ResourceTypeUtil.isActivationCapabilityEnabled(resource, resourceObjectTypeDefinitionType);

    }

    public static boolean isPasswordSupported(ShadowType shadowType, IModel<ResourceType> resourceModel) {
        ResourceType resource = resourceModel.getObject();
        if (resource == null) {
            return true; //TODO should be true?
        }

        ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType = ResourceTypeUtil.findObjectTypeDefinition(resource.asPrismObject(), shadowType.getKind(), shadowType.getIntent());

        return ResourceTypeUtil.isPasswordCapabilityEnabled(resource, resourceObjectTypeDefinitionType);

    }

    public static boolean isAssociationSupported(ShadowType shadowType, IModel<ResourceType> resourceModel) {
        ResourceType resource = resourceModel.getObject();
        if (resource == null) {
            return true; //TODO should be true?
        }

        ResourceObjectDefinition ocd = null;

        try {
            ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchema(resource.asPrismObject());
            ocd = resourceSchema.findDefinitionForShadow(shadowType);
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Cannot find refined definition for {} in {}", shadowType, resource);
        }

        if (ocd == null) {
            return false;
        }

        return CollectionUtils.isNotEmpty(ocd.getReferenceAttributeDefinitions());
    }

    public static void toggleResourceMaintenance(@NotNull PrismObject<ResourceType> resource, String operation, AjaxRequestTarget target, PageBase pageBase) {
        AdministrativeAvailabilityStatusType resourceAdministrativeAvailabilityStatus = ResourceTypeUtil.getAdministrativeAvailabilityStatus(resource.asObjectable());
        AdministrativeAvailabilityStatusType finalStatus = AdministrativeAvailabilityStatusType.MAINTENANCE; // default new value for existing null

        if (resourceAdministrativeAvailabilityStatus != null) {
            finalStatus = switch (resourceAdministrativeAvailabilityStatus) {
                case MAINTENANCE -> AdministrativeAvailabilityStatusType.OPERATIONAL;
                case OPERATIONAL -> AdministrativeAvailabilityStatusType.MAINTENANCE;
            };
        }

        switchResourceMaintenance(resource, operation, target, pageBase, finalStatus);
    }

    public static void switchResourceMaintenance(@NotNull PrismObject<ResourceType> resource, String operation, AjaxRequestTarget target, PageBase pageBase, AdministrativeAvailabilityStatusType mode) {
        Task task = pageBase.createSimpleTask(operation);
        OperationResult parentResult = new OperationResult(operation);

        try {
            ObjectDelta<ResourceType> objectDelta = pageBase.getPrismContext().deltaFactory().object()
                    .createModificationReplaceProperty(ResourceType.class, resource.getOid(), ItemPath.create(ResourceType.F_ADMINISTRATIVE_OPERATIONAL_STATE,
                            new QName("administrativeAvailabilityStatus")), mode);

            pageBase.getModelService().executeChanges(MiscUtil.createCollection(objectDelta), null, task, parentResult);

        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                | ExpressionEvaluationException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error changing resource administrative operational state", e);
            parentResult.recordFatalError(pageBase.createStringResource("pageResource.setMaintenance.failed").getString(), e);
        }

        parentResult.computeStatus();
        pageBase.showResult(parentResult, "pageResource.setMaintenance.failed");
        target.add(pageBase.getFeedbackPanel());
    }

    public static void refreshResourceSchema(@NotNull PrismObject<ResourceType> resource, String operation, AjaxRequestTarget target, PageBase pageBase) {
        OperationResult result = new OperationResult(operation);
        refreshResourceSchema(resource, operation, target, pageBase, result);
    }

    public static void refreshResourceSchema(@NotNull PrismObject<ResourceType> resource, String operation, PageBase pageBase, boolean showResult) {
        OperationResult result = new OperationResult(operation);
        refreshResourceSchema(resource, operation, pageBase, result, true);
    }

    public static void refreshResourceSchema(@NotNull PrismObject<ResourceType> resource, String operation, AjaxRequestTarget target, PageBase pageBase, OperationResult result) {
        refreshResourceSchema(resource, operation, pageBase, result, true);
        target.add(pageBase.getFeedbackPanel());
    }

    private static void refreshResourceSchema(@NotNull PrismObject<ResourceType> resource, String operation, PageBase pageBase, OperationResult result, boolean showResult) {
        Task task = pageBase.createSimpleTask(operation);

        try {
            ResourceUtils.deleteSchema(resource, pageBase.getModelService(), task, result);
            pageBase.getModelService().testResource(resource.getOid(), task, result); // try to load fresh schema
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                | ExpressionEvaluationException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error refreshing resource schema", e);
            result.recordFatalError(pageBase.createStringResource("WebComponentUtil.message.refreshResourceSchema.fatalError").getString(), e);
        }

        result.computeStatus();
        if (showResult) {
            pageBase.showResult(result, "pageResource.refreshSchema.failed");
        }
    }

    public static void partialConfigurationTest(@NotNull PrismObject<ResourceType> resource, PageBase pageBase, Task task, OperationResult result) {
        try {
            pageBase.getModelService().testResourcePartialConfiguration(resource, task, result);
        } catch (ObjectNotFoundException | SchemaException | ConfigurationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error partial configuration of resource", e);
            result.recordFatalError(pageBase.createStringResource("WebComponentUtil.message.partialConfigurationTest.fatalError").getString(), e);
        }
        result.computeStatus();
    }

    public static CapabilityCollectionType getNativeCapabilities(ResourceType resource, PageBase pageBase) {
        OperationResult result = new OperationResult("load native capabilities");
        try {
            return pageBase.getModelService().getNativeCapabilities(resource.getConnectorRef().getOid(), result);
        } catch (ObjectNotFoundException | SchemaException
                | CommunicationException | ConfigurationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error getting native capabilities", e);
            result.recordFatalError(pageBase.createStringResource("WebComponentUtil.message.gettingNativeCapabilities.fatalError").getString(), e);
            return new CapabilityCollectionType();
        }
    }

    public static Collection<QName> loadResourceObjectClassValues(ResourceType resource, PageBase pageBase) {
        try {
            ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
            if (schema != null) {
                return schema.getObjectClassNames();
            }
        } catch (SchemaException | ConfigurationException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object class list from resource.", e);
            pageBase.error("Couldn't load object class list from resource.");
        }
        return new ArrayList<>();
    }

    public static List<ShadowAssociationDefinition> getReferenceAssociationDefinition(ConstructionType construction, PageBase pageBase) {
        List<ShadowAssociationDefinition> associationDefinitions = new ArrayList<>();

        if (construction == null) {
            return associationDefinitions;
        }
        PrismObject<ResourceType> resource = getConstructionResource(construction, "load resource", pageBase);
        if (resource == null) {
            return associationDefinitions;
        }
        try {
            ResourceObjectDefinition oc = getResourceObjectDefinition(construction, pageBase);
            if (oc == null) {
                LOGGER.debug("Association for {} not supported by resource {}", construction, resource);
                return associationDefinitions;
            }
            return getRefinedAssociationDefinition(oc);
        } catch (Exception ex) {
            LOGGER.error("Association for {} not supported by resource {}: {}", construction, resource, ex.getLocalizedMessage());
        }
        return associationDefinitions;
    }

    private static List<ShadowReferenceAttributeDefinition> getReferenceAssociationDefinition(@NotNull ResourceObjectDefinition oc) {

        List<ShadowReferenceAttributeDefinition> associationDefinitions = new ArrayList<>(oc.getReferenceAttributeDefinitions());

        if (CollectionUtils.isEmpty(associationDefinitions)) {
            LOGGER.debug("Association not supported by resource object definition {}", oc);
            return associationDefinitions;
        }
        return associationDefinitions;
    }

    private static List<ShadowAssociationDefinition> getRefinedAssociationDefinition(@NotNull ResourceObjectDefinition oc) {

        List<ShadowAssociationDefinition> associationDefinitions = new ArrayList<>(oc.getAssociationDefinitions());

        if (CollectionUtils.isEmpty(associationDefinitions)) {
            LOGGER.debug("Association not supported by resource object definition {}", oc);
            return associationDefinitions;
        }
        return associationDefinitions;
    }

    public static ResourceObjectDefinition getResourceObjectDefinition(ConstructionType construction, PageBase pageBase) throws CommonException {

        if (construction == null) {
            return null;
        }
        PrismObject<ResourceType> resource = getConstructionResource(construction, "load resource", pageBase);
        if (resource == null) {
            return null;
        }

        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (schema == null) {
            return null;
        }
        return schema.findDefinitionForConstruction(construction);
    }

    public static List<ShadowReferenceAttributeDefinition> getReferenceAssociationDefinition(ResourceType resource, ShadowKindType kind, String intent) {
        List<ShadowReferenceAttributeDefinition> associationDefinitions = new ArrayList<>();

        try {

            if (resource == null) {
                return associationDefinitions;
            }
            ResourceSchema refinedResourceSchema = ResourceSchemaFactory.getCompleteSchema(resource.asPrismObject());
            if (com.evolveum.midpoint.schema.util.ShadowUtil.isNotKnown(kind)) {
                return associationDefinitions;
            }

            ResourceObjectDefinition oc;
            if (com.evolveum.midpoint.schema.util.ShadowUtil.isNotKnown(intent)) {
                oc = refinedResourceSchema.findDefaultDefinitionForKind(kind);
            } else {
                oc = refinedResourceSchema.findObjectDefinition(kind, intent);
            }
            if (oc == null) {
                LOGGER.debug("Association for {}/{} not supported by resource {}", kind, intent, resource);
                return associationDefinitions;
            }
            return getReferenceAssociationDefinition(oc);
        } catch (Exception ex) {
            LOGGER.error("Association for {}/{} not supported by resource {}: {}", kind, intent, resource, ex.getLocalizedMessage());
        }
        return associationDefinitions;
    }

    public static List<ShadowAssociationDefinition> getRefinedAssociationDefinition(ResourceType resource, ShadowKindType kind, String intent) {
        List<ShadowAssociationDefinition> associationDefinitions = new ArrayList<>();

        try {

            if (resource == null) {
                return associationDefinitions;
            }
            ResourceSchema refinedResourceSchema = ResourceSchemaFactory.getCompleteSchema(resource.asPrismObject());
            if (com.evolveum.midpoint.schema.util.ShadowUtil.isNotKnown(kind)) {
                return associationDefinitions;
            }

            ResourceObjectDefinition oc;
            if (com.evolveum.midpoint.schema.util.ShadowUtil.isNotKnown(intent)) {
                oc = refinedResourceSchema.findDefaultDefinitionForKind(kind);
            } else {
                oc = refinedResourceSchema.findObjectDefinition(kind, intent);
            }
            if (oc == null) {
                LOGGER.debug("Association for {}/{} not supported by resource {}", kind, intent, resource);
                return associationDefinitions;
            }
            return getRefinedAssociationDefinition(oc);
        } catch (Exception ex) {
            LOGGER.error("Association for {}/{} not supported by resource {}: {}", kind, intent, resource, ex.getLocalizedMessage());
        }
        return associationDefinitions;
    }

    public static ShadowAssociationDefinition getRefinedAssociationDefinition(ItemName association, ConstructionType construction, PageBase pageBase) {
        if (construction == null || association == null) {
            return null;
        }

        ResourceObjectDefinition objectDef = null;
        try {
            objectDef = getResourceObjectDefinition(construction, pageBase);
        } catch (CommonException e) {
            LOGGER.debug("Couldn't find ResourceObjectDefinition for construction " + construction);
        }
        if (objectDef == null) {
            return null;
        }

        for (ShadowAssociationDefinition shadowAssociationDefinition : objectDef.getAssociationDefinitions()) {
            if (shadowAssociationDefinition.getItemName().equivalent(association)) {
                return shadowAssociationDefinition;
            }
        }
        return null;
    }

    public static String getAssociationDisplayName(ShadowReferenceAttributeDefinition assocDef) {
        if (assocDef != null) {
            return assocDef.getHumanReadableDescription();
        } else {
            return "";
        }
    }

    public static String getAssociationDisplayName(ShadowAssociationDefinition assocDef) {
        return getAssociationDisplayName(assocDef.getReferenceAttributeDefinition());
    }

    @Deprecated
    public static ExpressionType getAssociationExpression(PrismContainerValueWrapper<AssignmentType> assignmentValueWrapper, PageBase pageBase) {
        return getAssociationExpression(assignmentValueWrapper, false, null, pageBase);
    }

    //TODO refactor..
    @Deprecated
    public static ExpressionType getAssociationExpression(PrismContainerValueWrapper<AssignmentType> assignmentValueWrapper,
            boolean createIfNotExist, PrismContext prismContext, PageBase pageBase) {
        if (assignmentValueWrapper == null) {
            return null;
        }
        if (createIfNotExist && prismContext == null) {
            throw new IllegalArgumentException("createIfNotExist is set but prismContext is null");
        }
        PrismContainerWrapper<ResourceObjectAssociationType> association;
        try {
            association = assignmentValueWrapper
                    .findContainer(ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION));
        } catch (SchemaException e) {
            LOGGER.error("Cannot find association wrapper, reason: {}", e.getMessage(), e);
            pageBase.getSession().error("Cannot find association wrapper, reason: " + e.getMessage());
            return null;
        }

        if (association == null || association.getValues() == null || association.getValues().size() == 0) {
            return null;
        }
        PrismContainerValueWrapper<ResourceObjectAssociationType> associationValueWrapper = association.getValues().get(0);
        PrismPropertyWrapper<ExpressionType> expressionWrapper;
        try {
            expressionWrapper = associationValueWrapper.findProperty(ItemPath.create(ResourceObjectAssociationType.F_OUTBOUND, MappingType.F_EXPRESSION));
        } catch (SchemaException e) {
            LOGGER.error("Cannot find expression wrapper, reason: {}", e.getMessage(), e);
            pageBase.getSession().error("Cannot find expression wrapper, reason: " + e.getMessage());
            return null;
        }

        if (expressionWrapper == null) {
            return null;
        }
        List<PrismPropertyValueWrapper<ExpressionType>> expressionValues = expressionWrapper.getValues();
        if (expressionValues == null || expressionValues.size() == 0) {
            return null;
        }
        try {
            ExpressionType expression = expressionValues.get(0).getRealValue();
            if (expression == null && createIfNotExist) {
                expression = new ExpressionType();
                PrismPropertyValue<ExpressionType> exp = prismContext.itemFactory().createPropertyValue(expression);
                WrapperContext context = new WrapperContext(null, null);
                PrismPropertyValueWrapper<ExpressionType> val = (PrismPropertyValueWrapper<ExpressionType>) pageBase
                        .createValueWrapper(expressionWrapper, exp, ValueStatus.ADDED, context);
                expressionValues.remove(0);
                expressionValues.add(0, val);
            }
        } catch (SchemaException e) {
            // TODO erro handling
            return null;
        }
        return expressionValues.get(0).getRealValue();
    }

    public static PrismObject<ResourceType> getConstructionResource(ConstructionType construction, String operation, PageBase pageBase) {
        ObjectReferenceType resourceRef = construction.getResourceRef();
        if (resourceRef.asReferenceValue().getObject() != null) {
            return resourceRef.asReferenceValue().getObject();
        }
        OperationResult result = new OperationResult(operation);
        Task task = pageBase.createSimpleTask(operation);
        return WebModelServiceUtils.resolveReferenceNoFetch(resourceRef, pageBase, task, result);
    }

    public static boolean activationNotSupported(ResourceType resource) {
        return resource != null && !ResourceTypeUtil.isActivationCapabilityEnabled(resource, null);
    }

    public static ResourceType resolveResource(ShadowType shadowType, boolean isColumn, PageBase pageBase) {
        PrismObject<ResourceType> prismResource = shadowType.getResourceRef().getObject();
        LOGGER.trace("Resource reference in shadow with full object : {}", shadowType);
        if (prismResource != null) {
            return prismResource.asObjectable();
        }
        if (!isColumn) {
            Task task = pageBase.createSimpleTask("Load Resource");
            try {
                // Do not set object to shadow.resourceRef. We don't want to serialize it
                return pageBase.getModelObjectResolver().resolve(
                        shadowType.getResourceRef(), ResourceType.class,
                        SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), "Load Resource", task, task.getResult());
            } catch (CommonException e) {
                //ignore exception
            }
        }
        return null;
    }

    public static List<ShadowRelationParticipantType> getObjectsOfSubject(ShadowReferenceAttributeDefinition refAttrDef) {
        List<ShadowRelationParticipantType> objects = new ArrayList<>();

        refAttrDef.getTargetParticipantTypes().forEach(objectParticipantDef -> {
            @NotNull ResourceObjectDefinition objectDef = objectParticipantDef.getObjectDefinition();
            if (objectDef.getObjectClassDefinition().isEmbedded()) {
                objectDef.getReferenceAttributeDefinitions().forEach(associationRefAttrDef -> {
                    associationRefAttrDef.getTargetParticipantTypes().forEach(associationObjectParticipantDef -> {
                        @NotNull ResourceObjectDefinition associationObjectDef = associationObjectParticipantDef.getObjectDefinition();
                        if (associationObjectDef.getObjectClassDefinition().isEmbedded()) {
                            return;
                        }
                        objects.add(associationObjectParticipantDef);
                    });
                });
                return;
            }

            objects.add(objectParticipantDef);
        });
        return objects;
    }

    public static List<ShadowReferenceAttributeDefinition> getShadowReferenceAttributeDefinitions(
            CompleteResourceSchema resourceSchema) {
        List<ShadowReferenceAttributeDefinition> list = new ArrayList<>();
        resourceSchema.getObjectTypeDefinitions().forEach(objectTypeDef ->
                objectTypeDef.getReferenceAttributeDefinitions().forEach(
                        associationDef -> {
                            if (!associationDef.canRead()
                                    || ShadowReferenceParticipantRole.SUBJECT != associationDef.getParticipantRole()) {
                                return;
                            }
                            list.add(associationDef);
                        }));
        return list;
    }

    @Nullable
    public static ItemName getAssociationForConstructionAndShadow(@NotNull ConstructionType construction, @NotNull ShadowType shadow, PageBase pageBase) {
        if (construction.getAssociation() != null
                && construction.getAssociation().size() == 1
                && construction.getAssociation().get(0).getRef() != null) {
            return construction.getAssociation().get(0).getRef().getItemPath().lastName();
        }

        ResourceObjectDefinition objectDef = null;
        try {
            objectDef = getResourceObjectDefinition(construction, pageBase);
        } catch (CommonException e) {
            LOGGER.debug("Couldn't find ResourceObjectDefinition for construction " + construction);
        }
        if (objectDef == null) {
            return null;
        }

        List<? extends ShadowAssociationDefinition> associationDefs = objectDef.getAssociationDefinitions().stream()
                .filter(assocDef -> assocDef.getObjectParticipants().values().stream()
                        .anyMatch(participant -> participant.matches(shadow)))
                .toList();

        if (associationDefs.isEmpty()) {
            LOGGER.debug("Couldn't find association definition for shadow " + shadow + " in " + objectDef);
            return null;
        }

        if (associationDefs.size() > 1) {
            LOGGER.debug("Couldn't find one association definition for shadow " + shadow + " found: " + associationDefs);
            return null;
        }

        return associationDefs.get(0).getItemName();
    }
}

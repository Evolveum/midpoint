/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismItemDefinitionType;

import org.jetbrains.annotations.NotNull;

public class SearchableItemsDefinitions {
    private static final Trace LOGGER = TraceManager.getTrace(SearchableItemsDefinitions.class);
    private static final String DOT_CLASS = SearchableItemsDefinitions.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";


    private static final Map<Class<?>, List<ItemPath>> SEARCHABLE_OBJECTS = new HashMap<>();
    private static final Map<CollectionPanelType, List<ItemPath>> SHADOW_SEARCHABLE_ITEMS = new HashMap<>();
    private static final Map<QName, List<ItemPath>> SEARCHABLE_ASSIGNMENT_ITEMS = new HashMap<>();

    private Class<?> type;
    private ResourceShadowCoordinates coordinates;
    private ModelServiceLocator modelServiceLocator;
    private CollectionPanelType collectionPanelType;
    private QName assignmentTargetType;
    private ItemDefinition<?> containerDefinition;

    private ResourceObjectDefinition resourceObjectDefinition;
    private boolean history;

    public SearchableItemsDefinitions(Class<?> type, ModelServiceLocator modelServiceLocator) {
        this.type = type;
        this.modelServiceLocator = modelServiceLocator;
    }

    public SearchableItemsDefinitions additionalSearchContext(SearchContext ctx) {
        if (ctx == null) {
            return this;
        }
        this.resourceObjectDefinition = ctx.getResourceObjectDefinition();
        this.containerDefinition = ctx.getDefinitionOverride();
        this.assignmentTargetType = ctx.getAssignmentTargetType();
        this.collectionPanelType = ctx.getPanelType();
        this.history = ctx.isHistory();
        return this;
    }


    static {
        SEARCHABLE_OBJECTS.put(ObjectType.class, Arrays.asList(
                ItemPath.create(ObjectType.F_NAME),
                ItemPath.create(ObjectType.F_LIFECYCLE_STATE),
                ItemPath.create(ObjectType.F_SUBTYPE),
                ItemPath.create(ObjectType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP),
                ItemPath.create(ObjectType.F_METADATA, MetadataType.F_MODIFY_TIMESTAMP),
                ItemPath.create(ObjectType.F_EFFECTIVE_MARK_REF)
        ));
        SEARCHABLE_OBJECTS.put(AssignmentHolderType.class, Arrays.asList(
                ItemPath.create(AssignmentHolderType.F_ARCHETYPE_REF)
        ));
        SEARCHABLE_OBJECTS.put(FocusType.class, Arrays.asList(
                ItemPath.create(FocusType.F_ROLE_MEMBERSHIP_REF),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO)
        ));
        SEARCHABLE_OBJECTS.put(UserType.class, Arrays.asList(
                ItemPath.create(UserType.F_TITLE),
                ItemPath.create(UserType.F_GIVEN_NAME),
                ItemPath.create(UserType.F_FAMILY_NAME),
                ItemPath.create(UserType.F_FULL_NAME),
                ItemPath.create(UserType.F_ADDITIONAL_NAME),
                ItemPath.create(UserType.F_COST_CENTER),
                ItemPath.create(UserType.F_EMAIL_ADDRESS),
                ItemPath.create(UserType.F_TELEPHONE_NUMBER),
                ItemPath.create(UserType.F_EMPLOYEE_NUMBER),
                ItemPath.create(UserType.F_ORGANIZATIONAL_UNIT),
                ItemPath.create(UserType.F_LOCALITY),
                ItemPath.create(UserType.F_PERSONAL_NUMBER)
        ));
        SEARCHABLE_OBJECTS.put(RoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_NAME)

        ));
        SEARCHABLE_OBJECTS.put(ServiceType.class, Arrays.asList(
                ItemPath.create(ServiceType.F_NAME),
                ItemPath.create(ServiceType.F_URL)
        ));
        SEARCHABLE_OBJECTS.put(ConnectorHostType.class, Arrays.asList(
                ItemPath.create(ConnectorHostType.F_HOSTNAME)
        ));
        SEARCHABLE_OBJECTS.put(ConnectorType.class, Arrays.asList(
                ItemPath.create(ConnectorType.F_CONNECTOR_BUNDLE),
                ItemPath.create(ConnectorType.F_CONNECTOR_VERSION),
                ItemPath.create(ConnectorType.F_CONNECTOR_TYPE)
        ));
        SEARCHABLE_OBJECTS.put(AbstractRoleType.class, Arrays.asList(
                ItemPath.create(AbstractRoleType.F_IDENTIFIER),
                ItemPath.create(AbstractRoleType.F_REQUESTABLE),
                ItemPath.create(RoleType.F_DISPLAY_NAME)
        ));
        SEARCHABLE_OBJECTS.put(OrgType.class, Arrays.asList(
                ItemPath.create(OrgType.F_COST_CENTER),
                ItemPath.create(OrgType.F_TENANT),
                ItemPath.create(OrgType.F_PARENT_ORG_REF),
                ItemPath.create(OrgType.F_LOCALITY)
        ));
        SEARCHABLE_OBJECTS.put(NodeType.class, Arrays.asList(
                ItemPath.create(NodeType.F_NODE_IDENTIFIER)
        ));
        SEARCHABLE_OBJECTS.put(TaskType.class, Arrays.asList(
                ItemPath.create(TaskType.F_TASK_IDENTIFIER),
                ItemPath.create(TaskType.F_NODE),
                ItemPath.create(TaskType.F_RESULT_STATUS),
                ItemPath.create(TaskType.F_EXECUTION_STATE),
                ItemPath.create(TaskType.F_HANDLER_URI),
                ItemPath.create(TaskType.F_OBJECT_REF)

        ));

        SEARCHABLE_OBJECTS.put(AssignmentType.class, Arrays.asList(
                ItemPath.create(AssignmentType.F_TARGET_REF),
                // Prism now supports search by reference target name (in form of @/name) so
                // it is okay to have this, even if repository assignment view is not enabled
                ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(), ObjectType.F_NAME),
                ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF),
                ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS)
        ));

        SEARCHABLE_OBJECTS.put(CaseWorkItemType.class, Arrays.asList(
                ItemPath.create(AbstractWorkItemType.F_NAME),
                ItemPath.create(CaseWorkItemType.F_ASSIGNEE_REF),
                ItemPath.create(CaseWorkItemType.F_ORIGINAL_ASSIGNEE_REF),
                ItemPath.create(PrismConstants.T_PARENT, CaseType.F_STATE),
                ItemPath.create(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF),
                ItemPath.create(CaseWorkItemType.F_PERFORMER_REF)
        ));

        SEARCHABLE_OBJECTS.put(CaseType.class, Arrays.asList(
                ItemPath.create(CaseType.F_STATE),
                ItemPath.create(CaseType.F_PARENT_REF),
                ItemPath.create(CaseType.F_REQUESTOR_REF),
                ItemPath.create(CaseType.F_TARGET_REF),
                ItemPath.create(CaseType.F_OBJECT_REF)
        ));

        SEARCHABLE_OBJECTS.put(ObjectPolicyConfigurationType.class, Arrays.asList(
                ItemPath.create(ObjectPolicyConfigurationType.F_SUBTYPE),
                ItemPath.create(ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF)
        ));

        SEARCHABLE_OBJECTS.put(AuditEventRecordType.class, Arrays.asList(
                ItemPath.create(AuditEventRecordType.F_TIMESTAMP),
                ItemPath.create(AuditEventRecordType.F_INITIATOR_REF),
                ItemPath.create(AuditEventRecordType.F_EVENT_TYPE),
                ItemPath.create(AuditEventRecordType.F_TARGET_OWNER_REF),
                ItemPath.create(AuditEventRecordType.F_CHANGED_ITEM),
                ItemPath.create(AuditEventRecordType.F_OUTCOME),
                ItemPath.create(AuditEventRecordType.F_CHANNEL),
                ItemPath.create(AuditEventRecordType.F_HOST_IDENTIFIER),
                ItemPath.create(AuditEventRecordType.F_REQUEST_IDENTIFIER),
                ItemPath.create(AuditEventRecordType.F_REFERENCE),
                ItemPath.create(AuditEventRecordType.F_TASK_IDENTIFIER)
        ));

        SEARCHABLE_OBJECTS.put(ClassLoggerConfigurationType.class, Arrays.asList(
                ItemPath.create(ClassLoggerConfigurationType.F_APPENDER),
                ItemPath.create(ClassLoggerConfigurationType.F_PACKAGE)
        ));

        SEARCHABLE_OBJECTS.put(SimulationResultType.class, Arrays.asList(
                ItemPath.create(SimulationResultType.F_START_TIMESTAMP),
                ItemPath.create(SimulationResultType.F_END_TIMESTAMP),
                ItemPath.create(SimulationResultType.F_ROOT_TASK_REF)
        ));

        SEARCHABLE_OBJECTS.put(SimulationResultProcessedObjectType.class, Arrays.asList(
                ItemPath.create(SimulationResultProcessedObjectType.F_NAME),
                ItemPath.create(SimulationResultProcessedObjectType.F_STATE),
                ItemPath.create(SimulationResultProcessedObjectType.F_TYPE),
                ItemPath.create(SimulationResultProcessedObjectType.F_OID),
                ItemPath.create(SimulationResultProcessedObjectType.F_EVENT_MARK_REF)
        ));

        SEARCHABLE_OBJECTS.put(AccessCertificationCampaignType.class, Arrays.asList(
                ItemPath.create(AccessCertificationCampaignType.F_DEFINITION_REF),
                ItemPath.create(AccessCertificationCampaignType.F_START_TIMESTAMP),
                ItemPath.create(AccessCertificationCampaignType.F_END_TIMESTAMP),
                ItemPath.create(AccessCertificationCampaignType.F_STATE),
                ItemPath.create(AccessCertificationCampaignType.F_STAGE)
        ));

        SEARCHABLE_OBJECTS.put(AccessCertificationCaseType.class, Arrays.asList(
                ItemPath.create(AccessCertificationCaseType.F_OBJECT_REF),
                ItemPath.create(AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME),
                ItemPath.create(AccessCertificationCaseType.F_OUTCOME),
                ItemPath.create(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationWorkItemType.F_ASSIGNEE_REF),
                ItemPath.create(AccessCertificationCaseType.F_TARGET_REF)
        ));

        SEARCHABLE_OBJECTS.put(AccessCertificationWorkItemType.class, Arrays.asList(
                ItemPath.create(AccessCertificationWorkItemType.F_NAME),
                ItemPath.create(PrismConstants.T_PARENT, AccessCertificationCaseType.F_OBJECT_REF),
                ItemPath.create(PrismConstants.T_PARENT, AccessCertificationCaseType.F_TARGET_REF),
                ItemPath.create(AccessCertificationWorkItemType.F_ASSIGNEE_REF),
                ItemPath.create(AccessCertificationWorkItemType.F_ORIGINAL_ASSIGNEE_REF),
                ItemPath.create(AccessCertificationWorkItemType.F_CANDIDATE_REF),
                ItemPath.create(AccessCertificationWorkItemType.F_PERFORMER_REF),
//                ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT),
                ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME),
                ItemPath.create(AccessCertificationWorkItemType.F_CREATE_TIMESTAMP),
                ItemPath.create(AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
        ));

        SEARCHABLE_OBJECTS.put(PrismItemDefinitionType.class, Arrays.asList(
                ItemPath.create(PrismItemDefinitionType.F_NAME),
                ItemPath.create(PrismItemDefinitionType.F_DISPLAY_NAME),
                ItemPath.create(PrismItemDefinitionType.F_REQUIRED),
                ItemPath.create(PrismItemDefinitionType.F_MULTIVALUE),
                ItemPath.create(PrismItemDefinitionType.F_INDEXED)
        ));

        SEARCHABLE_OBJECTS.put(GuiObjectListViewType.class, Arrays.asList(
                ItemPath.create(GuiObjectListViewType.F_TYPE),
                ItemPath.create(GuiObjectListViewType.F_IDENTIFIER),
                ItemPath.create(GuiObjectListViewType.F_DISPLAY, DisplayType.F_LABEL)
        ));
    }

    static {
        SHADOW_SEARCHABLE_ITEMS.put(CollectionPanelType.PROJECTION_SHADOW,
                Arrays.asList(
                        ItemPath.create(ShadowType.F_OBJECT_CLASS),
                        ItemPath.create(ShadowType.F_RESOURCE_REF),
                        ItemPath.create(ShadowType.F_DEAD),
                        ItemPath.create(ShadowType.F_INTENT),
                        ItemPath.create(ShadowType.F_KIND),
                        ItemPath.create(ShadowType.F_EXISTS),
                        ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)));

        SHADOW_SEARCHABLE_ITEMS.put(CollectionPanelType.REPO_SHADOW,
                Arrays.asList(
                        ItemPath.create(ShadowType.F_DEAD),
                        ItemPath.create(ShadowType.F_INTENT),
                        ItemPath.create(ShadowType.F_KIND),
                        ItemPath.create(ShadowType.F_EXISTS),
                        ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)));

        SHADOW_SEARCHABLE_ITEMS.put(CollectionPanelType.DEBUG,
                Arrays.asList(
                        ItemPath.create(ShadowType.F_OBJECT_CLASS),
                        ItemPath.create(ShadowType.F_RESOURCE_REF),
                        ItemPath.create(ShadowType.F_INTENT),
                        ItemPath.create(ShadowType.F_KIND),
                        ItemPath.create(ShadowType.F_EXISTS),
                        ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)));

        SHADOW_SEARCHABLE_ITEMS.put(CollectionPanelType.ASSOCIABLE_SHADOW,
                Arrays.asList(
                        ItemPath.create(ShadowType.F_DEAD),
                        ItemPath.create(ShadowType.F_EXISTS),
                        ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)));

    }

    static {
        SEARCHABLE_ASSIGNMENT_ITEMS.put(RoleType.COMPLEX_TYPE, Arrays.asList(
                ItemPath.create(AssignmentType.F_TARGET_REF),
                ItemPath.create(AssignmentType.F_TENANT_REF),
                ItemPath.create(AssignmentType.F_ORG_REF)));

        SEARCHABLE_ASSIGNMENT_ITEMS.put(ResourceType.COMPLEX_TYPE, Arrays.asList(
                ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF)));

        SEARCHABLE_ASSIGNMENT_ITEMS.put(PolicyRuleType.COMPLEX_TYPE, Arrays.asList(
                ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME),
                ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
                        PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF)
                ));
    }


    private static List<ItemPath> getSearchableItemsFor(Class<?> typeClass, CollectionPanelType shadowSearchType) {
        if (ShadowType.class.equals(typeClass)) {
            return SHADOW_SEARCHABLE_ITEMS.get(shadowSearchType);
        }

        return SearchableItemsDefinitions.SEARCHABLE_OBJECTS.get(typeClass);
    }

    @NotNull public PathKeyedMap<ItemDefinition<?>> createAvailableSearchItems() {

        Collection<ItemPath> extensionPaths = createExtensionPaths();

        PathKeyedMap<ItemDefinition<?>> searchableDefinitions = new PathKeyedMap<>();

        ItemDefinition<?> containerDef = getDefinition();
        collectExtensionDefinitions(containerDef, extensionPaths, searchableDefinitions);

        collectionNonExtensionDefinitions(containerDef, searchableDefinitions, isUseSuperclassDefinition());

        collectAttributesDefinitions(searchableDefinitions);

        return searchableDefinitions;

    }

    private boolean isUseSuperclassDefinition() {
        return !ShadowType.class.equals(type) || CollectionPanelType.RESOURCE_SHADOW != collectionPanelType;

    }

    private ItemDefinition<?> getDefinition() {
        if (containerDefinition != null) {
            return containerDefinition;
        }
        if (ObjectType.class.isAssignableFrom(type)) {
            return containerDefinition = findObjectDefinition();
        }
        if (Containerable.class.isAssignableFrom(type)) {
            return containerDefinition = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass((Class<Containerable>) type);
        }
        if (Referencable.class.isAssignableFrom(type)) { //TODO probably some "extended" definition with target specification should be here
            return containerDefinition = PrismContext.get().getSchemaRegistry().findItemDefinitionByType(ObjectReferenceType.COMPLEX_TYPE);
        }
        throw new UnsupportedOperationException("Either explicit definition must be declared, or the type must be of ObjectType or Contianerable");
    }

    private Collection<ItemPath> createExtensionPaths() {
        List<ItemPath> extensionPaths = new ArrayList<>();
        extensionPaths.add(ObjectType.F_EXTENSION);
        if (AssignmentType.class.equals(type)) {
            if (assignmentTargetType == null || isAssignmentTargetTypeObjectable()) {
                extensionPaths.add(ItemPath.create(AssignmentType.F_TARGET_REF,
                        new ObjectReferencePathSegment(assignmentTargetType), ObjectType.F_EXTENSION));
            }
        }
        return extensionPaths;
    }

    private boolean isAssignmentTargetTypeObjectable() {
        TypeDefinition targetRefTypeDef = PrismContext.get().getSchemaRegistry().findTypeDefinitionByType(assignmentTargetType);
        return targetRefTypeDef != null && targetRefTypeDef.getCompileTimeClass() != null
                && ObjectType.class.isAssignableFrom(targetRefTypeDef.getCompileTimeClass());
    }

    private PrismObjectDefinition findObjectDefinition() {

        Task task = modelServiceLocator.createSimpleTask(LOAD_OBJECT_DEFINITION);
        OperationResult result = task.getResult();
        try {
            if (Modifier.isAbstract(type.getModifiers())) {
                SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
                return registry.findObjectDefinitionByCompileTimeClass((Class<? extends ObjectType>)type);
            }
            PrismObject empty = modelServiceLocator.getPrismContext().createObject((Class<? extends ObjectType>)type);

            if (ShadowType.class.equals(type)) {
                return modelServiceLocator.getModelInteractionService().getEditShadowDefinition(
                        coordinates, AuthorizationPhaseType.REQUEST, task, result);
            } else {
                return modelServiceLocator.getModelInteractionService().getEditObjectDefinition(
                        empty, AuthorizationPhaseType.REQUEST, task, result);
            }
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | SecurityViolationException ex) {
            result.recordFatalError(ex.getMessage());
            throw new SystemException(ex);
        }
    }
    private void collectExtensionDefinitions(ItemDefinition<?> containerDef, Collection<ItemPath> extensionPaths, Map<ItemPath, ItemDefinition<?>> searchableItems) {
        if (containerDef == null) {
            return;
        }

        if (CollectionPanelType.RESOURCE_SHADOW == collectionPanelType) {
            return;
        }

        for (ItemPath path : extensionPaths) {

            PrismContainerDefinition ext = containerDef.findItemDefinition(path, PrismContainerDefinition.class);
            if (ext == null) {
                LOGGER.trace("No extension defined, shipping collecting extension search items");
                return;
            }
            Map<ItemPath, ItemDefinition<?>> extensionItems = ((List<ItemDefinition<?>>) ext.getDefinitions()).stream()
                    .filter(SearchableItemsDefinitions::isNotContainerAndIsIndexed)
                    .collect(Collectors.toMap(d -> ItemPath.create(path, d.getItemName()), d -> d));
            searchableItems.putAll(extensionItems);
        }
    }

    private <C extends Containerable> void collectionNonExtensionDefinitions(ItemDefinition<?> itemDefinition, Map<ItemPath, ItemDefinition<?>> searchableDefinitions, boolean useDefsFromSuperclass) {
        if (CollectionPanelType.RESOURCE_SHADOW == collectionPanelType) {
            return;
        }
        if (!(itemDefinition instanceof PrismContainerDefinition)) {
            LOGGER.trace("Skipping collecting non container search item definitions. Parent definition not container definition.");
            return;
        }
        PrismContainerDefinition<C> containerDef = (PrismContainerDefinition<C>) itemDefinition;
        Class<C> typeClass = containerDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = getAvailableSearchableItems(typeClass);

            if (paths == null) {
                if (!useDefsFromSuperclass) {
                    break;
                }
                typeClass = (Class<C>) typeClass.getSuperclass();
                continue;
            }

            for (ItemPath path : paths) {
                ItemDefinition<?> def = containerDef.findItemDefinition(path);
                if (def != null) {
                    searchableDefinitions.put(path, def);
                }
            }

            if (!useDefsFromSuperclass) {
                break;
            }

            typeClass = (Class<C>) typeClass.getSuperclass();
        }
    }

    private List<ItemPath> getAvailableSearchableItems(Class<? extends Containerable> typeClass) {
        List<ItemPath> items = getSearchableItemsFor(typeClass, collectionPanelType);
        if (AuditEventRecordType.class.equals(typeClass)) {
            SystemConfigurationType systemConfigurationType;
            try {
                systemConfigurationType = modelServiceLocator.getModelInteractionService()
                        .getSystemConfiguration(new OperationResult("load_system_config"));
            } catch (SchemaException | ObjectNotFoundException e) {
                throw new SystemException(e);
            }
            if (systemConfigurationType != null && systemConfigurationType.getAudit() != null
                    && systemConfigurationType.getAudit().getEventRecording() != null &&
                    Boolean.TRUE.equals(systemConfigurationType.getAudit().getEventRecording().isRecordResourceOids())) {
                ArrayList<ItemPath> auditItems = new ArrayList<>(items);
                auditItems.add(ItemPath.create(AuditEventRecordType.F_RESOURCE_OID));
                items = auditItems;
            }
            if (!history) {
                ArrayList<ItemPath> allSearchableItems = new ArrayList<>(items);
                allSearchableItems.add(ItemPath.create(AuditEventRecordType.F_EVENT_STAGE));
                allSearchableItems.add(ItemPath.create(AuditEventRecordType.F_TARGET_REF));
                items = allSearchableItems;
            }
        }
        return items;
    }

    private void collectAttributesDefinitions(Map<ItemPath, ItemDefinition<?>> searchableDefinitions) {
        if (resourceObjectDefinition == null) {
            return;
        }

        if (CollectionPanelType.RESOURCE_SHADOW != collectionPanelType) {
            return;
        }

        for (ShadowSimpleAttributeDefinition def : resourceObjectDefinition.getSimpleAttributeDefinitions()) {
            searchableDefinitions.put(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)), def);
        }
    }

    private ItemName getAttributeName(ShadowSimpleAttributeDefinition def) {
        return def.getItemName();
    }

    private static boolean isNotContainerAndIsIndexed(ItemDefinition<?> def) {
        if (def instanceof PrismContainerDefinition) {
            return false;
        }
        return isIndexed(def);
    }

    private static boolean isIndexed(ItemDefinition def) {
        if (!(def instanceof PrismPropertyDefinition)) {
            return true;
        }

        PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) def;
        Boolean indexed = propertyDef.isIndexed();
        if (indexed == null) {
            return true;
        }

        return indexed;
    }

}

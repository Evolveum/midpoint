package com.evolveum.midpoint.gui.impl.component.search;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PredefinedSearchableItems {

    private static final Trace LOGGER = TraceManager.getTrace(PredefinedSearchableItems.class);

    private static final Map<Class<?>, List<ItemPath>> SEARCHABLE_OBJECTS = new HashMap<>();

    private static final Map<QName, List<ItemPath>> SEARCHABLE_ASSIGNMENT_ITEMS = new HashMap<>();

    private static final String DOT_CLASS = SearchFactory.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";

    static {
        SEARCHABLE_OBJECTS.put(ObjectType.class, Arrays.asList(
                ItemPath.create(ObjectType.F_NAME),
                ItemPath.create(ObjectType.F_LIFECYCLE_STATE),
                ItemPath.create(ObjectType.F_SUBTYPE),
                ItemPath.create(ObjectType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP),
                ItemPath.create(ObjectType.F_METADATA, MetadataType.F_MODIFY_TIMESTAMP)
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
                ItemPath.create(UserType.F_LOCALITY)
        ));
        SEARCHABLE_OBJECTS.put(RoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_NAME),
                ItemPath.create(RoleType.F_DISPLAY_NAME)
        ));
        SEARCHABLE_OBJECTS.put(ServiceType.class, Arrays.asList(
                ItemPath.create(ServiceType.F_NAME),
                ItemPath.create(RoleType.F_DISPLAY_NAME),
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
                ItemPath.create(AbstractRoleType.F_REQUESTABLE)
        ));
        SEARCHABLE_OBJECTS.put(OrgType.class, Arrays.asList(
                ItemPath.create(OrgType.F_DISPLAY_NAME),
                ItemPath.create(OrgType.F_COST_CENTER),
                ItemPath.create(OrgType.F_TENANT),
                ItemPath.create(OrgType.F_PARENT_ORG_REF),
                ItemPath.create(OrgType.F_LOCALITY)
        ));
//        SEARCHABLE_OBJECTS.put(GenericObjectType.class, Arrays.asList(
//                ItemPath.create(GenericObjectType.F_OBJECT_TYPE)
//        ));
        SEARCHABLE_OBJECTS.put(NodeType.class, Arrays.asList(
                ItemPath.create(NodeType.F_NODE_IDENTIFIER)
        ));
//        SEARCHABLE_OBJECTS.put(ReportType.class, Arrays.asList(
//                ItemPath.create(ReportType.F_NAME)
//        ));
        SEARCHABLE_OBJECTS.put(ShadowType.class, Arrays.asList(
                ItemPath.create(ShadowType.F_OBJECT_CLASS),
                ItemPath.create(ShadowType.F_RESOURCE_REF),
                ItemPath.create(ShadowType.F_DEAD),
                ItemPath.create(ShadowType.F_INTENT),
                ItemPath.create(ShadowType.F_KIND),
                ItemPath.create(ShadowType.F_EXISTS),
                ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)
        ));
        SEARCHABLE_OBJECTS.put(TaskType.class, Arrays.asList(
                ItemPath.create(TaskType.F_TASK_IDENTIFIER),
                ItemPath.create(TaskType.F_NODE),
                ItemPath.create(TaskType.F_CATEGORY),
                ItemPath.create(TaskType.F_RESULT_STATUS),
                ItemPath.create(TaskType.F_EXECUTION_STATE),
                ItemPath.create(TaskType.F_HANDLER_URI),
                ItemPath.create(TaskType.F_OBJECT_REF)

        ));

        SEARCHABLE_OBJECTS.put(AssignmentType.class, Arrays.asList(
                ItemPath.create(AssignmentType.F_TARGET_REF),
                ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF),
                ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS)
        ));

        SEARCHABLE_OBJECTS.put(CaseWorkItemType.class, Arrays.asList(
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
                ItemPath.create(CaseType.F_TASK_REF),
                ItemPath.create(CaseType.F_OBJECT_REF)
        ));

        SEARCHABLE_OBJECTS.put(ObjectPolicyConfigurationType.class, Arrays.asList(
                ItemPath.create(ObjectPolicyConfigurationType.F_SUBTYPE),
                ItemPath.create(ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF)
        ));

        SEARCHABLE_OBJECTS.put(AuditEventRecordType.class, Arrays.asList(
                ItemPath.create(AuditEventRecordType.F_TIMESTAMP),
                ItemPath.create(AuditEventRecordType.F_INITIATOR_REF),
                ItemPath.create(AuditEventRecordType.F_EVENT_STAGE),
                ItemPath.create(AuditEventRecordType.F_EVENT_TYPE),
                ItemPath.create(AuditEventRecordType.F_TARGET_REF),
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
    private static final Map<Class<?>, List<ItemPath>> FIXED_SEARCH_ITEMS = new HashMap<>();
    static {
        FIXED_SEARCH_ITEMS.put(ObjectType.class, Arrays.asList(
                ItemPath.create(ObjectType.F_NAME))
        );
        FIXED_SEARCH_ITEMS.put(UserType.class, Arrays.asList(
                ItemPath.create(UserType.F_GIVEN_NAME),
                ItemPath.create(UserType.F_FAMILY_NAME)
        ));
        FIXED_SEARCH_ITEMS.put(AbstractRoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_DISPLAY_NAME)
        ));
        FIXED_SEARCH_ITEMS.put(RoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_IDENTIFIER)
        ));
        FIXED_SEARCH_ITEMS.put(ServiceType.class, Arrays.asList(
                ItemPath.create(ServiceType.F_IDENTIFIER)
        ));
        FIXED_SEARCH_ITEMS.put(OrgType.class, Arrays.asList(
                ItemPath.create(OrgType.F_PARENT_ORG_REF)
        ));
        FIXED_SEARCH_ITEMS.put(AuditEventRecordType.class, Arrays.asList(
                ItemPath.create(AuditEventRecordType.F_TIMESTAMP)
        ));
        FIXED_SEARCH_ITEMS.put(ShadowType.class, Arrays.asList(
                ItemPath.create(ShadowType.F_RESOURCE_REF),
                ItemPath.create(ShadowType.F_OBJECT_CLASS)
        ));
    }

    public static List<ItemPath> getAvailableSearchableItems(Class<?> typeClass, ModelServiceLocator modelServiceLocator) {
        List<ItemPath> items = SEARCHABLE_OBJECTS.get(typeClass);
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
        }
        return items;
    }

    public static <C extends Containerable> boolean isFixedItem(Class<C> typeClass, ItemPath path) {

        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            if (FIXED_SEARCH_ITEMS.get(typeClass) != null &&
                    ItemPathCollectionsUtil.containsEquivalent(FIXED_SEARCH_ITEMS.get(typeClass), path)) {
                return true;
            }
            typeClass = (Class<C>) typeClass.getSuperclass();
        }

        return false;
    }

    public static <C extends Containerable> Map<ItemPath, ItemDefinition<?>> getAvailableSearchItems(Class<C> type, Collection<ItemPath> extensionPaths, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        PrismContainerDefinition<C> def = null;
        if (ObjectType.class.isAssignableFrom(type)) {
            def = findObjectDefinition((Class<? extends ObjectType>) type, coordinates, modelServiceLocator);
        } else {
            def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
        }

        return getSearchableDefinitionMap(def, extensionPaths, modelServiceLocator);
    }

    public static <C extends Containerable> Map<ItemPath, ItemDefinition<?>> getAvailableAssignmentSearchItems(QName assignmentTargetType, PrismContainerDefinition<AssignmentType> definitionOverride, Collection<ItemPath> extensionPaths, ModelServiceLocator modelServiceLocator) {
        Map<ItemPath, ItemDefinition<?>> searchableDefinitions = new HashMap<>();
        collectExtensionDefinitions(definitionOverride, extensionPaths, searchableDefinitions);

        collectionNonExtensionDefinitions(definitionOverride, searchableDefinitions, true, modelServiceLocator);

        //TODO assignment search items according to type
        return searchableDefinitions;
    }

    public static <T extends ObjectType> PrismObjectDefinition findObjectDefinition(
            Class<T> type, ResourceShadowCoordinates coordinates,
            ModelServiceLocator modelServiceLocator) {

        Task task = modelServiceLocator.createSimpleTask(LOAD_OBJECT_DEFINITION);
        OperationResult result = task.getResult();
        try {
            if (Modifier.isAbstract(type.getModifiers())) {
                SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
                return registry.findObjectDefinitionByCompileTimeClass(type);
            }
            PrismObject empty = modelServiceLocator.getPrismContext().createObject(type);

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

    public static <C extends Containerable> Map<ItemPath, ItemDefinition<?>> getSearchableDefinitionMap(
            PrismContainerDefinition<C> containerDef, Collection<ItemPath> extensionPaths, ModelServiceLocator modelServiceLocator) {
        return getSearchableDefinitionMap(containerDef, modelServiceLocator, extensionPaths, true);
    }

    public static <C extends Containerable> Map<ItemPath, ItemDefinition<?>> getSearchableDefinitionMap(
            PrismContainerDefinition<C> containerDef, ModelServiceLocator modelServiceLocator, Collection<ItemPath> extensionPaths, boolean useDefsFromSuperclass) {

        Map<ItemPath, ItemDefinition<?>> searchableDefinitions = new HashMap<>();
        collectExtensionDefinitions(containerDef, extensionPaths, searchableDefinitions);

        collectionNonExtensionDefinitions(containerDef, searchableDefinitions, useDefsFromSuperclass, modelServiceLocator);

        return searchableDefinitions;
    }

    @NotNull
    private static <C extends Containerable> void collectExtensionDefinitions(PrismContainerDefinition<C> containerDef, Collection<ItemPath> extensionPaths, Map<ItemPath, ItemDefinition<?>> searchableItems) {
        if (containerDef == null) {
            return;
        }

        for (ItemPath path : extensionPaths) {

            PrismContainerDefinition ext = containerDef.findContainerDefinition(path);
            if (ext == null || ext.getDefinitions() == null) {
                LOGGER.trace("No extension defined, shipping collecting extension search items");
                return;
            }
            Map<ItemPath, ItemDefinition<?>> extensionItems = ((List<ItemDefinition<?>>) ext.getDefinitions()).stream()
                    .filter(def -> isNotContainerAndIsIndexed(def))
                    .collect(Collectors.toMap(d -> ItemPath.create(path, d.getItemName()), d -> d));
            searchableItems.putAll(extensionItems);
        }
    }

    private static <C extends Containerable> void collectionNonExtensionDefinitions(PrismContainerDefinition<C> containerDef, Map<ItemPath, ItemDefinition<?>> searchableDefinitions, boolean useDefsFromSuperclass,  ModelServiceLocator modelServiceLocator) {
        Class<C> typeClass = containerDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = PredefinedSearchableItems.getAvailableSearchableItems(typeClass, modelServiceLocator);

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

        return indexed.booleanValue();
    }
}

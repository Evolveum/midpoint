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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.QNameUtil;

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
    private static final String DOT_CLASS = PredefinedSearchableItems.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";


    private static final Map<Class<?>, List<ItemPath>> SEARCHABLE_OBJECTS = new HashMap<>();
    private static final Map<PanelType, List<ItemPath>> SHADOW_SEARCHABLE_ITEMS = new HashMap<>();
    private static final Map<QName, List<ItemPath>> SEARCHABLE_ASSIGNMENT_ITEMS = new HashMap<>();

    private Class<? extends Containerable> type;
    private ResourceShadowCoordinates coordinates;
    private ModelServiceLocator modelServiceLocator;
    private PanelType panelType;
    private QName assignmentTargetType;
    private PrismContainerDefinition<? extends Containerable> containerDefinition;

    private ResourceObjectDefinition resourceObjectDefinition;

    private SearchContext additionalSearchContext;

    public PredefinedSearchableItems(Class<? extends Containerable> type, ModelServiceLocator modelServiceLocator) {
        this.type = type;
        this.modelServiceLocator = modelServiceLocator;
    }
    public PredefinedSearchableItems panelType(PanelType panelType) {
        this.panelType = panelType;
        return this;
    }
    public PredefinedSearchableItems assignmentTargetType(QName assignmentTargetType) {
        this.assignmentTargetType = assignmentTargetType;
        return this;
    }


    public PredefinedSearchableItems resourceObjectDefinition(ResourceObjectDefinition resourceObjectDefinition) {
        this.resourceObjectDefinition = resourceObjectDefinition;
        return this;
    }

    public PredefinedSearchableItems containerDefinition(PrismContainerDefinition<? extends Containerable> containerDefinition) {
        this.containerDefinition = containerDefinition;
        return this;
    }


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
        SEARCHABLE_OBJECTS.put(NodeType.class, Arrays.asList(
                ItemPath.create(NodeType.F_NODE_IDENTIFIER)
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
        SHADOW_SEARCHABLE_ITEMS.put(PanelType.PROJECTION_SHADOW,
                Arrays.asList(
                        ItemPath.create(ShadowType.F_OBJECT_CLASS),
                        ItemPath.create(ShadowType.F_RESOURCE_REF),
                        ItemPath.create(ShadowType.F_DEAD),
                        ItemPath.create(ShadowType.F_INTENT),
                        ItemPath.create(ShadowType.F_KIND),
                        ItemPath.create(ShadowType.F_EXISTS),
                        ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)));

        SHADOW_SEARCHABLE_ITEMS.put(PanelType.REPO_SHADOW,
                Arrays.asList(
                        ItemPath.create(ShadowType.F_DEAD),
                        ItemPath.create(ShadowType.F_INTENT),
                        ItemPath.create(ShadowType.F_KIND),
                        ItemPath.create(ShadowType.F_EXISTS),
                        ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)));

        SHADOW_SEARCHABLE_ITEMS.put(PanelType.DEBUG,
                Arrays.asList(
                        ItemPath.create(ShadowType.F_DEAD),
                        ItemPath.create(ShadowType.F_INTENT),
                        ItemPath.create(ShadowType.F_KIND),
                        ItemPath.create(ShadowType.F_EXISTS),
                        ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)));

        SHADOW_SEARCHABLE_ITEMS.put(PanelType.ASSOCIABLE_SHADOW,
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


    public static List<ItemPath> getSearchableItemsFor(Class<?> typeClass) {
        return getSearchableItemsFor(typeClass, PanelType.PROJECTION_SHADOW);
    }

    public static List<ItemPath> getSearchableItemsFor(Class<?> typeClass, PanelType shadowSearchType) {
        if (ShadowType.class.equals(typeClass)) {
            return SHADOW_SEARCHABLE_ITEMS.get(shadowSearchType);
        }

        return PredefinedSearchableItems.SEARCHABLE_OBJECTS.get(typeClass);
    }


    public enum PanelType {
        ROLE_MEMBER_GOVERNANCE(true, "roleGovernance", true, FocusType.COMPLEX_TYPE),
        ROLE_MEMBER_MEMBER(true, "roleMembers", true, FocusType.COMPLEX_TYPE),
        SERVICE_MEMBER_GOVERNANCE(true, "serviceGovernance", true, FocusType.COMPLEX_TYPE),
        SERVICE_MEMBER_MEMBER(true, "serviceMembers", true, FocusType.COMPLEX_TYPE),
        ARCHETYPE_MEMBER_GOVERNANCE(true, "archetypeGovernance", true, FocusType.COMPLEX_TYPE),
        ARCHETYPE_MEMBER_MEMBER(true, "archetypeMembers", true, AssignmentHolderType.COMPLEX_TYPE),
        ORG_MEMBER_GOVERNANCE(true, "orgGovernance", true, FocusType.COMPLEX_TYPE),
        ORG_MEMBER_MEMBER(true, "orgMembers", true, AssignmentType.COMPLEX_TYPE),
        MEMBER_ORGANIZATION(true, null, true, AssignmentHolderType.COMPLEX_TYPE),
        CARDS_GOVERNANCE(true, null, true, FocusType.COMPLEX_TYPE),
        MEMBER_WIZARD(true, null, false, UserType.COMPLEX_TYPE),
        RESOURCE_SHADOW(false, null, false, null),
        REPO_SHADOW(false, null, false, null),
        ASSOCIABLE_SHADOW(false, null, false, null),
        PROJECTION_SHADOW(false, null, false, null),
        DEBUG(false, null, false, null),
        ASSIGNABLE(false, null, false, null);

        private boolean memberPanel;
        private String panelInstance;

        private boolean isAllowAllTypeSearch;
        private QName typeForNull;

        PanelType(boolean memberPanel, String panelInstance, boolean isAllowAllTypeSearch, QName typeForNull) {
            this.memberPanel = memberPanel;
            this.panelInstance = panelInstance;
            this.isAllowAllTypeSearch = isAllowAllTypeSearch;
            this.typeForNull = typeForNull;
        }

        public boolean isMemberPanel() {
            return memberPanel;
        }

        public static PanelType getPanelType(String panelInstance) {
            if (panelInstance == null) {
                return null;
            }
            for (PanelType panelType : PanelType.values()) {
                if (panelInstance.equals(panelType.panelInstance)) {
                    return panelType;
                }
            }
            return null;
        }

        public QName getTypeForNull() {
            return typeForNull;
        }

        public boolean isAllowAllTypeSearch() {
            return isAllowAllTypeSearch;
        }
    }

    public Map<ItemPath, ItemDefinition<?>> createAvailableSearchItems() {

        Collection<ItemPath> extensionPaths = createExtensionPaths();

        Map<ItemPath, ItemDefinition<?>> searchableDefinitions = new HashMap<>();

        PrismContainerDefinition<? extends Containerable> containerDef = getDefinition();
        collectExtensionDefinitions(containerDef, extensionPaths, searchableDefinitions);

        collectionNonExtensionDefinitions(containerDef, searchableDefinitions, isUseSuperclassDefinition());

        collectAttributesDefinitions(searchableDefinitions);

        return searchableDefinitions;

    }

    private boolean isUseSuperclassDefinition() {
        return !ShadowType.class.equals(type) || PanelType.RESOURCE_SHADOW != panelType;

    }

    private PrismContainerDefinition<? extends Containerable> getDefinition() {
        if (containerDefinition != null) {
            return containerDefinition;
        }
        if (ObjectType.class.isAssignableFrom(type)) {
            return containerDefinition = findObjectDefinition();
        }

        return containerDefinition = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
    }

    private Collection<ItemPath> createExtensionPaths() {
        List<ItemPath> extensionPaths = new ArrayList<>();
        extensionPaths.add(ObjectType.F_EXTENSION);
        if (AssignmentType.class.equals(type)) {
            extensionPaths.add(ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(assignmentTargetType), ObjectType.F_EXTENSION));
        }
        return extensionPaths;
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
    private <C extends Containerable> void collectExtensionDefinitions(PrismContainerDefinition<C> containerDef, Collection<ItemPath> extensionPaths, Map<ItemPath, ItemDefinition<?>> searchableItems) {
        if (containerDef == null) {
            return;
        }

        if (PanelType.RESOURCE_SHADOW == panelType) {
            return;
        }

        for (ItemPath path : extensionPaths) {

            PrismContainerDefinition ext = containerDef.findContainerDefinition(path);
            if (ext == null) {
                LOGGER.trace("No extension defined, shipping collecting extension search items");
                return;
            }
            Map<ItemPath, ItemDefinition<?>> extensionItems = ((List<ItemDefinition<?>>) ext.getDefinitions()).stream()
                    .filter(PredefinedSearchableItems::isNotContainerAndIsIndexed)
                    .collect(Collectors.toMap(d -> ItemPath.create(path, d.getItemName()), d -> d));
            searchableItems.putAll(extensionItems);
        }
    }

    private <C extends Containerable> void collectionNonExtensionDefinitions(PrismContainerDefinition<C> containerDef, Map<ItemPath, ItemDefinition<?>> searchableDefinitions, boolean useDefsFromSuperclass) {
        if (PanelType.RESOURCE_SHADOW == panelType) {
            return;
        }
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
        List<ItemPath> items = getSearchableItemsFor(typeClass, panelType);//SEARCHABLE_OBJECTS.get(typeClass);
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

    private void collectAttributesDefinitions(Map<ItemPath, ItemDefinition<?>> searchableDefinitions) {
        if (resourceObjectDefinition == null) {
            return;
        }

        if (PanelType.RESOURCE_SHADOW != panelType) {
            return;
        }

        for (ResourceAttributeDefinition def : resourceObjectDefinition.getAttributeDefinitions()) {
            searchableDefinitions.put(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)), def);
        }
    }

    private ItemName getAttributeName(ResourceAttributeDefinition def) {
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

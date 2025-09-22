/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.constants;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
public enum ObjectTypes {
    ROLE_ANALYSIS_OUTLIER(SchemaConstants.C_ROLE_ANALYSIS_OUTLIER_TYPE, SchemaConstants.C_ROLE_ANALYSIS_OUTLIER,
            RoleAnalysisOutlierType.class, ObjectManager.MODEL, "roleAnalysisOutlier"),
    ROLE_ANALYSIS_SESSION(SchemaConstants.C_ROLE_ANALYSIS_SESSION_TYPE, SchemaConstants.C_ROLE_ANALYSIS_SESSION,
            RoleAnalysisSessionType.class, ObjectManager.MODEL, "roleAnalysisSession"),
    ROLE_ANALYSIS_CLUSTER(SchemaConstants.C_ROLE_ANALYSIS_CLUSTER_TYPE, SchemaConstants.C_ROLE_ANALYSIS_CLUSTER,
            RoleAnalysisClusterType.class, ObjectManager.MODEL, "roleAnalysisCluster"),

    CONNECTOR(SchemaConstants.C_CONNECTOR_TYPE, SchemaConstants.C_CONNECTOR,
            ConnectorType.class, ObjectManager.PROVISIONING, "connectors"),

    CONNECTOR_DEVELOPMENT(SchemaConstants.C_CONNECTOR_DEVELOPMENT_TYPE, SchemaConstants.C_CONNECTOR_DEVELOPMENT,
            ConnectorDevelopmentType.class, ObjectManager.MODEL, "connectorDevelopments"),

    CONNECTOR_HOST(SchemaConstants.C_CONNECTOR_HOST_TYPE,
            SchemaConstants.C_CONNECTOR_HOST, ConnectorHostType.class, ObjectManager.PROVISIONING, "connectorHosts"),

    GENERIC_OBJECT(SchemaConstants.C_GENERIC_OBJECT_TYPE,
            SchemaConstants.C_GENERIC_OBJECT, GenericObjectType.class, ObjectManager.MODEL, "genericObjects"),

    MARK(MarkType.COMPLEX_TYPE,
            SchemaConstantsGenerated.C_MARK, MarkType.class, ObjectManager.MODEL, "marks"),

    RESOURCE(SchemaConstants.C_RESOURCE_TYPE, SchemaConstants.C_RESOURCE,
            ResourceType.class, ObjectManager.PROVISIONING, "resources"),

    USER(SchemaConstants.C_USER_TYPE, SchemaConstants.C_USER, UserType.class,
            ObjectManager.MODEL, "users"),

    OBJECT_TEMPLATE(SchemaConstants.C_OBJECT_TEMPLATE_TYPE,
            SchemaConstants.C_OBJECT_TEMPLATE, ObjectTemplateType.class, ObjectManager.MODEL, "objectTemplates"),

    SYSTEM_CONFIGURATION(SchemaConstants.C_SYSTEM_CONFIGURATION_TYPE, SchemaConstants.C_SYSTEM_CONFIGURATION,
            SystemConfigurationType.class, ObjectManager.MODEL, "systemConfigurations"),

    TASK(SchemaConstants.C_TASK_TYPE, SchemaConstants.C_TASK, TaskType.class, ObjectManager.TASK_MANAGER, "tasks"),

    SHADOW(SchemaConstants.C_SHADOW_TYPE, SchemaConstants.C_SHADOW, ShadowType.class, ObjectManager.PROVISIONING, "shadows"),

    ROLE(RoleType.COMPLEX_TYPE, SchemaConstantsGenerated.C_ROLE, RoleType.class, ObjectManager.MODEL, "roles"),

    PASSWORD_POLICY(ValuePolicyType.COMPLEX_TYPE, SchemaConstantsGenerated.C_VALUE_POLICY, ValuePolicyType.class,
            ObjectManager.MODEL, "valuePolicies"),

    NODE(NodeType.COMPLEX_TYPE, SchemaConstantsGenerated.C_NODE, NodeType.class, ObjectManager.TASK_MANAGER, "nodes"),

    FORM(FormType.COMPLEX_TYPE, SchemaConstantsGenerated.C_FORM, FormType.class, ObjectManager.MODEL, "forms"),

    ORG(OrgType.COMPLEX_TYPE, SchemaConstantsGenerated.C_ORG, OrgType.class, ObjectManager.MODEL, "orgs"),

    ABSTRACT_ROLE(AbstractRoleType.COMPLEX_TYPE, SchemaConstants.C_ABSTRACT_ROLE, AbstractRoleType.class,
            ObjectManager.MODEL, "abstractRoles"),

    FOCUS_TYPE(FocusType.COMPLEX_TYPE, SchemaConstants.C_FOCUS, FocusType.class, ObjectManager.MODEL, "focus"),

    ASSIGNMENT_HOLDER_TYPE(AssignmentHolderType.COMPLEX_TYPE, SchemaConstantsGenerated.C_ASSIGNMENT_HOLDER,
            AssignmentHolderType.class, ObjectManager.MODEL, "assignmentHolders"),

    REPORT(ReportType.COMPLEX_TYPE, SchemaConstants.C_REPORT, ReportType.class, ObjectManager.MODEL, "reports"),

    REPORT_DATA(ReportDataType.COMPLEX_TYPE, SchemaConstants.C_REPORT_DATA, ReportDataType.class,
            ObjectManager.MODEL, "reportData"),

    SECURITY_POLICY(SecurityPolicyType.COMPLEX_TYPE, SchemaConstants.C_SECURITY_POLICY, SecurityPolicyType.class,
            ObjectManager.MODEL, "securityPolicies"),

    LOOKUP_TABLE(LookupTableType.COMPLEX_TYPE, SchemaConstantsGenerated.C_LOOKUP_TABLE, LookupTableType.class,
            ObjectManager.MODEL, "lookupTables"),

    ACCESS_CERTIFICATION_DEFINITION(AccessCertificationDefinitionType.COMPLEX_TYPE,
            SchemaConstantsGenerated.C_ACCESS_CERTIFICATION_DEFINITION, AccessCertificationDefinitionType.class,
            ObjectManager.MODEL, "accessCertificationDefinitions"),

    ACCESS_CERTIFICATION_CAMPAIGN(AccessCertificationCampaignType.COMPLEX_TYPE,
            SchemaConstantsGenerated.C_ACCESS_CERTIFICATION_CAMPAIGN, AccessCertificationCampaignType.class,
            ObjectManager.MODEL, "accessCertificationCampaigns"),

    SEQUENCE(SequenceType.COMPLEX_TYPE, SchemaConstantsGenerated.C_SEQUENCE, SequenceType.class, ObjectManager.MODEL,
            "sequences"),

    SERVICE(ServiceType.COMPLEX_TYPE, SchemaConstantsGenerated.C_SERVICE, ServiceType.class, ObjectManager.MODEL,
            "services"),

    CASE(CaseType.COMPLEX_TYPE, SchemaConstantsGenerated.C_CASE, CaseType.class, ObjectManager.MODEL,
            "cases"),

    FUNCTION_LIBRARY(FunctionLibraryType.COMPLEX_TYPE, SchemaConstantsGenerated.C_FUNCTION_LIBRARY, FunctionLibraryType.class, ObjectManager.MODEL,
            "functionLibraries"),

    OBJECT_COLLECTION(ObjectCollectionType.COMPLEX_TYPE, SchemaConstantsGenerated.C_OBJECT_COLLECTION, ObjectCollectionType.class, ObjectManager.MODEL,
            "objectCollections"),

    ARCHETYPE(ArchetypeType.COMPLEX_TYPE, SchemaConstantsGenerated.C_ARCHETYPE, ArchetypeType.class, ObjectManager.MODEL,
            "archetypes"),

    DASHBOARD(DashboardType.COMPLEX_TYPE, SchemaConstantsGenerated.C_DASHBOARD, DashboardType.class, ObjectManager.MODEL,
            "dashboards"),

    MESSAGE_TEMPLATE(
            MessageTemplateType.COMPLEX_TYPE, SchemaConstantsGenerated.C_MESSAGE_TEMPLATE, MessageTemplateType.class,
            ObjectManager.MODEL, "messageTemplates"),

    SIMULATION_RESULT(
            SimulationResultType.COMPLEX_TYPE, SchemaConstantsGenerated.C_SIMULATION_RESULT, SimulationResultType.class,
            ObjectManager.MODEL, "simulationResults"),

    POLICY(
            PolicyType.COMPLEX_TYPE, SchemaConstantsGenerated.C_POLICY, PolicyType.class,
            ObjectManager.MODEL, "policies"),

    SCHEMA(
            SchemaType.COMPLEX_TYPE, SchemaConstantsGenerated.C_SCHEMA, SchemaType.class,
            ObjectManager.MODEL, "schemas"),

    APPLICATION(
            ApplicationType.COMPLEX_TYPE, SchemaConstantsGenerated.C_APPLICATION, ApplicationType.class,
            ObjectManager.MODEL, "applications"),

    // this should be at end, because otherwise it presents itself as entry for all subtypes of ObjectType
    OBJECT(SchemaConstants.C_OBJECT_TYPE, SchemaConstants.C_OBJECT, ObjectType.class, ObjectManager.MODEL, "objects");

    /** Provides fast access to the super type of given type. */
    private static final Map<ObjectTypes, ObjectTypes> SUPER_TYPE_MAP = new HashMap<>();

    /** Provides fast access to all ancestors of given type (excluding the type itself). */
    private static final Map<ObjectTypes, Collection<ObjectTypes>> ANCESTOR_MAP = new HashMap<>();

    /** Provides fast access to all descendants of given type (excluding the type itself). */
    private static final Map<ObjectTypes, Collection<ObjectTypes>> DESCENDANT_MAP = new HashMap<>();

    public enum ObjectManager {
        PROVISIONING, TASK_MANAGER, MODEL, REPOSITORY
    }

    @NotNull private final QName type;

    /**
     * As of 2020-06, this is used only for ObjectTypeUtil#toShortString+getShortTypeName.
     */
    @NotNull private final QName elementName;
    @NotNull private final Class<? extends ObjectType> classDefinition;
    @NotNull private final ObjectManager objectManager;
    @NotNull private final String restType;

    ObjectTypes(@NotNull QName type, @NotNull QName elementName, @NotNull Class<? extends ObjectType> classDefinition,
            @NotNull ObjectManager objectManager, @NotNull String restType) {
        this.type = type;
        this.elementName = elementName;
        this.classDefinition = classDefinition;
        this.objectManager = objectManager;
        this.restType = restType;
    }

    public boolean isManagedByProvisioning() {
        return objectManager == ObjectManager.PROVISIONING;
    }

    public boolean isManagedByTaskManager() {
        return objectManager == ObjectManager.TASK_MANAGER;
    }

    public String getValue() {
        return type.getLocalPart();
    }

    @NotNull
    public QName getElementName() {
        return elementName;
    }

    @NotNull
    public QName getTypeQName() {
        return type;
    }

    @NotNull
    public <O extends ObjectType> Class<O> getClassDefinition() {
        //noinspection unchecked
        return (Class<O>) classDefinition;
    }

    @NotNull
    public String getRestType() {
        return restType;
    }

    @NotNull
    public String getObjectTypeUri() {
        return QNameUtil.qNameToUri(getTypeQName());
    }

    @NotNull
    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @NotNull
    public static ObjectTypes getObjectType(String objectType) {
        for (ObjectTypes type : values()) {
            if (type.getValue().equals(objectType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported object type " + objectType);
    }

    @Contract("null -> null; !null -> !null")
    public static ObjectTypes getObjectTypeFromTypeQName(QName typeQName) {
        if (typeQName == null) {
            return null;
        }

        ObjectTypes type = getObjectTypeFromTypeQNameIfKnown(typeQName);
        if (type != null) {
            return type;
        }

        throw new IllegalArgumentException("Unsupported object type qname " + typeQName);
    }

    public static ObjectTypes getObjectTypeFromTypeQNameIfKnown(QName typeQName) {
        if (typeQName == null) {
            return null;
        }
        // HACK WARNING! FIXME
        // UGLY HORRIBLE TERRIBLE AWFUL HACK FOLLOWS
        // The JAXB fails to correctly process QNames in default namespace (no prefix)
        // e.g it will not understand this: type="RoleType", even if default namespace
        // is set, it will parse it as null namespace.
        // Therefore substitute null namespace with common namespace
        if (typeQName.getNamespaceURI() == null || typeQName.getNamespaceURI().isEmpty()) {
            typeQName = new QName(SchemaConstants.NS_C, typeQName.getLocalPart());
        }
        // END OF UGLY HACK

        for (ObjectTypes type : values()) {
            if (QNameUtil.match(type.getTypeQName(), typeQName)) {
                return type;
            }
        }

        return null;
    }

    @NotNull
    public static ObjectTypes getObjectTypeFromUri(String objectTypeUri) {
        for (ObjectTypes type : values()) {
            if (type.getObjectTypeUri().equals(objectTypeUri)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported object type uri " + objectTypeUri);
    }

    @NotNull
    public static String getObjectTypeUri(String objectType) {
        return getObjectType(objectType).getObjectTypeUri();
    }

    public static Class<? extends ObjectType> getObjectTypeClass(String typeNameLocal) {
        for (ObjectTypes type : values()) {
            if (type.getValue().equals(typeNameLocal)) {
                return type.getClassDefinition();
            }
        }
        throw new IllegalArgumentException("Unsupported object type " + typeNameLocal);
    }

    @NotNull
    public static <O extends ObjectType> Class<O> getObjectTypeClass(QName typeName) {
        Class<O> rv = getObjectTypeClassIfKnown(typeName);
        if (rv == null) {
            throw new IllegalArgumentException("Unsupported object type " + typeName);
        }
        return rv;
    }

    public static <O extends ObjectType> Class<O> getObjectTypeClassIfKnown(QName typeName) {
        for (ObjectTypes type : values()) {
            if (QNameUtil.match(type.getTypeQName(), typeName)) {
                return type.getClassDefinition();
            }
        }
        return null;
    }

    @NotNull
    public static ObjectTypes getObjectType(@NotNull Class<? extends ObjectType> objectType) {
        ObjectTypes rv = getObjectTypeIfKnown(objectType);
        if (rv == null) {
            throw new IllegalArgumentException("Unsupported object type " + objectType);
        }
        return rv;
    }

    public static ObjectTypes getObjectTypeIfKnown(@NotNull Class<?> objectType) {
        for (ObjectTypes type : values()) {
            if (type.getClassDefinition().equals(objectType)) {
                return type;
            }
        }
        // No match. Try with superclass.
        Class<?> superclass = objectType.getSuperclass();
        if (superclass != null) {
            return getObjectTypeIfKnown(superclass);
        }
        return null;
    }

    public static boolean isManagedByProvisioning(ObjectType object) {
        Validate.notNull(object, "Object must not be null.");

        return isClassManagedByProvisioning(object.getClass());
    }

    public static boolean isClassManagedByProvisioning(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().isAssignableFrom(clazz)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static boolean isClassManagedByTaskManager(@NotNull Class<? extends ObjectType> clazz) {
        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().isAssignableFrom(clazz)) {
                return type.isManagedByTaskManager();
            }
        }
        return false;
    }

    public static boolean isObjectTypeManagedByProvisioning(Class<? extends ObjectType> objectType) {
        Validate.notNull(objectType, "Object type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().equals(objectType)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static boolean isObjectTypeManagedByProvisioning(String objectType) {
        Validate.notEmpty(objectType, "Object type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getTypeQName().getLocalPart().equals(objectType)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static ObjectManager getObjectManagerForClass(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().isAssignableFrom(clazz)) {
                return type.getObjectManager();
            }
        }

        return null;
    }

    public static QName getTypeQNameFromRestType(String restType) {
        Validate.notNull(restType, "Rest type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getRestType().equals(restType)) {
                return type.getTypeQName();
            }
        }

        throw new IllegalArgumentException("Not suitable class found for rest type: " + restType);
    }

    public static @NotNull Class<? extends ObjectType> getClassFromRestType(String restType) {
        Validate.notNull(restType, "Rest type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getRestType().equals(restType)) {
                return type.getClassDefinition();
            }
        }

        throw new IllegalArgumentException("Not suitable class found for rest type: " + restType);
    }

    public static String getRestTypeFromClass(Class<?> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().equals(clazz)) {
                return type.getRestType();
            }
        }

        throw new IllegalArgumentException("Not suitable rest type found for class: " + clazz);
    }

    public static List<Class<? extends ObjectType>> getAllObjectTypes() {
        List<Class<? extends ObjectType>> list = new ArrayList<>();
        for (ObjectTypes t : ObjectTypes.values()) {
            list.add(t.getClassDefinition());
        }

        return list;
    }

    /**
     * Makes sure the QNames are full representation of object types, e.g. that they include proper namespace.
     */
    public static List<QName> canonizeObjectTypes(List<QName> inputQNames) {
        if (inputQNames == null) {
            return null;
        }
        List<QName> outputQNames = new ArrayList<>(inputQNames.size());
        for (QName inputQname : inputQNames) {
            outputQNames.add(canonizeObjectType(inputQname));
        }
        return outputQNames;
    }

    /**
     * Makes sure the QName is full representation of object types, e.g. that it includes proper namespace.
     */
    public static QName canonizeObjectType(QName inputQName) {
        if (!StringUtils.isBlank(inputQName.getNamespaceURI())) {
            return inputQName;
        }
        return new QName(SchemaConstants.NS_C, inputQName.getLocalPart());
    }

    public @NotNull List<ObjectTypes> thisAndSupertypes() {
        List<ObjectTypes> rv = new ArrayList<>();
        rv.add(this);
        rv.addAll(ANCESTOR_MAP.get(this));
        return rv;
    }

    public @NotNull Collection<ObjectTypes> subtypes() {
        return Objects.requireNonNull(DESCENDANT_MAP.get(this));
    }

    static {
        for (ObjectTypes type : values()) {
            SUPER_TYPE_MAP.put(type, getObjectTypeIfKnown(type.classDefinition.getSuperclass()));
        }
        for (ObjectTypes type : values()) {
            ANCESTOR_MAP.put(type, computeAncestors(type));
        }
        for (ObjectTypes type : values()) {
            DESCENDANT_MAP.put(type, computeDescendants(type));
        }
    }

    private static @NotNull List<ObjectTypes> computeAncestors(ObjectTypes type) {
        var knownAncestors = new ArrayList<ObjectTypes>();
        for (;;) {
            ObjectTypes superType = SUPER_TYPE_MAP.get(type);
            if (superType == null) {
                return knownAncestors;
            } else {
                knownAncestors.add(superType);
                type = superType;
            }
        }
    }

    private static @NotNull Collection<ObjectTypes> computeDescendants(ObjectTypes type) {
        return Arrays.stream(values())
                .filter(candidateDescendant -> ANCESTOR_MAP.get(candidateDescendant).contains(type))
                .collect(Collectors.toSet());
    }
}

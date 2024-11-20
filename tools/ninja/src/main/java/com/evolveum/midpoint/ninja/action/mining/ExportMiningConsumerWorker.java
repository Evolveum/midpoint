/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import static com.evolveum.midpoint.common.RoleMiningExportUtils.*;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.RoleMiningExportUtils;
import com.evolveum.midpoint.ninja.action.worker.AbstractWriterConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ExportMiningConsumerWorker extends AbstractWriterConsumerWorker<ExportMiningOptions, FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(ExportMiningConsumerWorker.class);

    OperationResult operationResult = new OperationResult(DOT_CLASS + "searchObjectByCondition");

    private PrismSerializer<String> serializer;
    private int processedRoleIterator = 0;
    private int processedUserIterator = 0;
    private int processedOrgIterator = 0;
    private final SequentialAnonymizer defaultAttributeNameAnonymizer = new SequentialAnonymizer("default_attr");
    private final SequentialAnonymizer extensionAttributeNameAnonymizer = new SequentialAnonymizer("extension_attr");
    private AttributeValueAnonymizer attributeValuesAnonymizer;

    private boolean orgAllowed;
    private boolean attributesAllowed;
    private boolean firstObject = true;
    private boolean jsonFormat = false;

    private RoleMiningExportUtils.SecurityMode securityMode;
    private RoleMiningExportUtils.NameMode nameMode;

    private String encryptKey;
    private ObjectFilter filterRole;
    private ObjectFilter filterOrg;

    private static String applicationArchetypeOid;
    private static String businessArchetypeOid;
    private List<String> applicationRolePrefix;
    private List<String> applicationRoleSuffix;
    private List<String> businessRolePrefix;
    private List<String> businessRoleSuffix;


    private static final Set<ItemPath> ATTR_PATHS_USER = extractDefaultAttributePaths(UserType.COMPLEX_TYPE);
    private static final Set<ItemPath> ATTR_PATHS_ROLE = extractDefaultAttributePaths(RoleType.COMPLEX_TYPE);
    private static final Set<ItemPath> ATTR_PATHS_ORG = extractDefaultAttributePaths(OrgType.COMPLEX_TYPE);
    private static final String ARCHETYPE_REF_ATTRIBUTE_NAME = "archetypeRef";

    public ExportMiningConsumerWorker(NinjaContext context, ExportMiningOptions options, BlockingQueue<FocusType> queue,
            OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        loadFilters(options.getRoleFilter(), options.getOrgFilter());
        loadRoleCategoryIdentifiers();

        securityMode = options.getSecurityLevel();
        encryptKey = RoleMiningExportUtils.updateEncryptKey(securityMode);
        orgAllowed = options.isIncludeOrg();
        attributesAllowed = options.isIncludeAttributes();

        nameMode = options.getNameMode();

        attributeValuesAnonymizer = new AttributeValueAnonymizer(nameMode, encryptKey);

        SerializationOptions serializationOptions = SerializationOptions.createSerializeForExport()
                .serializeReferenceNames(true)
                .serializeForExport(true)
                .skipContainerIds(true);

        jsonFormat = options.getOutput().getName().endsWith(".json");
        if (jsonFormat) {
            serializer = context.getPrismContext().jsonSerializer().options(serializationOptions);
        } else {
            serializer = context.getPrismContext().xmlSerializer().options(serializationOptions);
        }
    }

    @Override
    protected String getProlog() {
        if (jsonFormat) {
            return NinjaUtils.JSON_OBJECTS_PREFIX;
        }
        return NinjaUtils.XML_OBJECTS_PREFIX;
    }

    @Override
    protected String getEpilog() {
        if (jsonFormat) {
            return NinjaUtils.JSON_OBJECTS_SUFFIX;
        }
        return NinjaUtils.XML_OBJECTS_SUFFIX;
    }

    @Override
    protected void write(Writer writer, @NotNull FocusType object) throws SchemaException, IOException {
        if (object.asPrismObject().isOfType(RoleType.class)) {
            RoleType role = getPreparedRoleObject(object);
            write(writer, role.asPrismContainerValue());
        } else if (object.asPrismObject().isOfType(UserType.class)) {
            UserType user = getPreparedUserObject(object);
            if (user.getAssignment() != null && !user.getAssignment().isEmpty()) {
                write(writer, user.asPrismContainerValue());
            }
        } else if (object.asPrismObject().isOfType(OrgType.class)) {
            OrgType org = getPreparedOrgObject(object);
            write(writer, org.asPrismContainerValue());
        }
    }

    private void write(Writer writer, PrismContainerValue<?> prismContainerValue) throws SchemaException, IOException {
        String xml = serializer.serialize(prismContainerValue);
        if (jsonFormat && !firstObject) {
            writer.write(",\n" + xml);
        } else {
            writer.write(xml);
        }
        firstObject = false;
    }

    @NotNull
    private OrgType getPreparedOrgObject(@NotNull FocusType object) {
        OrgType org = new OrgType();

        fillAttributes(object, org, ATTR_PATHS_ORG);
        fillActivation(object, org);

        org.setName(encryptOrgName(object.getName().toString(), processedOrgIterator++, nameMode, encryptKey));
        org.setOid(encryptedUUID(object.getOid(), securityMode, encryptKey));

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();

            if (targetRef == null) {
                continue;
            }

            String objectType = getObjectType(targetRef);
            String oid = targetRef.getOid();

            if (objectType == null || oid == null) {
                continue;
            }

            if (objectType.equals(OrgType.class.getSimpleName())
                    && filterAllowedOrg(oid)) {
                org.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, encryptKey));
            }
        }

        return org;
    }

    @NotNull
    private UserType getPreparedUserObject(@NotNull FocusType object) {
        UserType user = new UserType();
        fillAttributes(object, user, ATTR_PATHS_USER);
        fillActivation(object, user);

        List<AssignmentType> assignment = object.getAssignment();
        if (assignment == null || assignment.isEmpty()) {
            return new UserType();
        }

        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();

            if (targetRef == null) {
                continue;
            }

            String objectType = getObjectType(targetRef);
            String oid = targetRef.getOid();

            if (objectType == null || oid == null) {
                continue;
            }

            if (objectType.equals(RoleType.class.getSimpleName())
                    && filterAllowedRole(oid)) {
                user.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, encryptKey));
            }

            if (orgAllowed && objectType.equals(OrgType.class.getSimpleName()) && filterAllowedOrg(oid)) {
                user.getAssignment().add(encryptObjectReference(assignmentObject, securityMode, encryptKey));
            }

        }

        user.setName(encryptUserName(object.getName().toString(), processedUserIterator++, nameMode, encryptKey));
        user.setOid(encryptedUUID(object.getOid(), securityMode, encryptKey));

        return user;
    }

    @NotNull
    private RoleType getPreparedRoleObject(@NotNull FocusType object) {
        RoleType role = new RoleType();
        fillAttributes(object, role, ATTR_PATHS_ROLE);
        fillActivation(object, role);

        String roleName = object.getName().toString();
        PolyStringType encryptedName = encryptRoleName(roleName, processedRoleIterator++, nameMode, encryptKey);
        role.setName(encryptedName);
        role.setOid(encryptedUUID(object.getOid(), securityMode, encryptKey));

        String identifier = "";

        List<AssignmentType> inducement = ((RoleType) object).getInducement();

        for (AssignmentType inducementObject : inducement) {
            ObjectReferenceType targetRef = inducementObject.getTargetRef();

            if (targetRef == null) {
                continue;
            }

            String objectType = getObjectType(targetRef);
            String oid = targetRef.getOid();
            if (objectType == null || oid == null) {
                continue;
            }

            if (objectType.equals(RoleType.class.getSimpleName()) && filterAllowedRole(oid)) {
                role.getInducement().add(encryptObjectReference(inducementObject, securityMode, encryptKey));
            }
        }

        List<AssignmentType> assignment = object.getAssignment();
        for (AssignmentType assignmentObject : assignment) {
            ObjectReferenceType targetRef = assignmentObject.getTargetRef();

            if (targetRef == null) {
                continue;
            }

            String objectType = getObjectType(targetRef);
            String oid = targetRef.getOid();

            if (objectType == null || oid == null) {
                continue;
            }

            if (objectType.equals(ArchetypeType.class.getSimpleName())) {
                AssignmentType assignmentType = new AssignmentType();
                if (oid.equals(applicationArchetypeOid)) {
                    identifier = APPLICATION_ROLE_IDENTIFIER;
                    assignmentType.targetRef(targetRef);
                    role.getAssignment().add(assignmentType);
                } else if (oid.equals(businessArchetypeOid)) {
                    identifier = BUSINESS_ROLE_IDENTIFIER;
                    assignmentType.targetRef(targetRef);
                    role.getAssignment().add(assignmentType);
                }
            }

        }

        if (!identifier.isEmpty()) {
            role.setIdentifier(identifier);
        } else {
            String prefixCheckedIdentifier = determineRoleCategory(roleName, applicationRolePrefix, businessRolePrefix,
                    applicationRoleSuffix, businessRoleSuffix);
            if (prefixCheckedIdentifier != null) {
                role.setIdentifier(prefixCheckedIdentifier);
            }
        }

        return role;
    }

    private boolean filterAllowedOrg(String oid) {
        if (filterOrg == null) {
            return true;
        }

        ObjectQuery objectQuery = context.getPrismContext().queryFactory().createQuery(filterOrg);
        objectQuery.addFilter(context.getPrismContext().queryFor(OrgType.class).id(oid).buildFilter());
        try {
            return !context.getRepository().searchObjects(OrgType.class,
                    objectQuery, null, operationResult).isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Failed to search organization object. ", e);
        }
        return false;
    }

    private boolean filterAllowedRole(String oid) {
        if (filterRole == null) {
            return true;
        }

        ObjectQuery objectQuery = context.getPrismContext().queryFactory().createQuery(filterRole);
        objectQuery.addFilter(context.getPrismContext().queryFor(RoleType.class).id(oid).buildFilter());
        try {
            return !context.getRepository().searchObjects(RoleType.class,
                    objectQuery, null, operationResult).isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Failed to search role object. ", e);
        }
        return false;
    }

    private void loadFilters(FileReference roleFileReference, FileReference orgFileReference) {
        try {
            this.filterRole = NinjaUtils.createObjectFilter(roleFileReference, context, RoleType.class);
            this.filterOrg = NinjaUtils.createObjectFilter(orgFileReference, context, OrgType.class);
        } catch (IOException | SchemaException e) {
            LoggingUtils.logException(LOGGER, "Failed to crate object filter. ", e);
        }
    }

    private void loadRoleCategoryIdentifiers() {
        applicationArchetypeOid = options.getApplicationRoleArchetypeOid();
        businessArchetypeOid = options.getBusinessRoleArchetypeOid();

        this.applicationRolePrefix = options.getApplicationRolePrefix();
        this.applicationRoleSuffix = options.getApplicationRoleSuffix();
        this.businessRolePrefix = options.getBusinessRolePrefix();
        this.businessRoleSuffix = options.getBusinessRoleSuffix();
    }

    private @Nullable String getObjectType(@NotNull ObjectReferenceType targetRef) {
        QName type = targetRef.getType();

        if (type != null) {
            return type.getLocalPart();
        }

        return null;
    }

    private static Set<ItemPath> extractDefaultAttributePaths(QName type) {
        var def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByType(type);
        return extractAttributePaths(def);
    }


    private static Set<ItemPath> extractAttributePaths(PrismContainerDefinition<?> containerDef) {
        Set<ItemPath> paths = new HashSet<>();
        for (ItemDefinition<?> def : containerDef.getDefinitions()) {
            var isArchetypeRef = def.getItemName().toString().equals(ARCHETYPE_REF_ATTRIBUTE_NAME);
            if (!isArchetypeRef && (def.isOperational() || !def.isSingleValue())) {
                // unsupported types
                continue;
            }
            if (def instanceof PrismReferenceDefinition refDef) {
                paths.add(refDef.getItemName());
            }
            if (def instanceof PrismPropertyDefinition<?> propertyDef && RoleAnalysisAttributeDefUtils.isSupportedPropertyType(propertyDef.getTypeClass())) {
                paths.add(propertyDef.getItemName());
            }
        }
        return paths;
    }

    private Object anonymizeAttributeValue(Item<?, ?> item) {
        var def = item.getDefinition();
        var type = def.getTypeClass();
        var realValue = Objects.requireNonNull(item.getRealValue());

        if (def instanceof PrismReferenceDefinition) {
            var referenceValue = (ObjectReferenceType) realValue;
            return encryptObjectReference(referenceValue, securityMode, encryptKey);
        }

        // NOTE: do not encrypt ordinal values and booleans
        var shouldAnonymizeValue = !List.of(Integer.class, Double.class, Boolean.class).contains(type);

        if (shouldAnonymizeValue) {
            var attributeName = item.getDefinition().getItemName().toString();
            return attributeValuesAnonymizer.anonymize(attributeName, realValue.toString());
        }
        return realValue;
    }

    private void anonymizeAttribute(FocusType newObject, PrismContainer<?> itemContainer, ItemPath path, SequentialAnonymizer attributeNameAnonymizer) {
        Item<?, ?> item = itemContainer.findItem(path);
        if (item == null || item.getRealValue() == null) {
            return;
        }
        try {
            String originalAtributeName = item.getDefinition().getItemName().toString();
            String anonymizedAttributeName = attributeNameAnonymizer.anonymize(originalAtributeName);
            Object anonymizedAttributeValue = anonymizeAttributeValue(item);

            QName propertyName = new QName(item.getDefinition().getItemName().getNamespaceURI(), anonymizedAttributeName);
            PrismPropertyDefinition<Object> propertyDefinition = context
                    .getPrismContext()
                    .definitionFactory()
                    .newPropertyDefinition(propertyName, DOMUtil.XSD_STRING);
            PrismProperty<Object> anonymizedProperty = propertyDefinition.instantiate();
            anonymizedProperty.setRealValue(anonymizedAttributeValue);
            newObject.asPrismObject().addExtensionItem(anonymizedProperty);
        } catch (Exception e) {
            LOGGER.error("Failed to clone item for {} object {}. ", itemContainer.getClass(), item, e);
        }
    }

    private void fillAttributes(@NotNull FocusType origObject, @NotNull FocusType newObject, @NotNull Set<ItemPath> defaultAttributePaths) {
        if (!attributesAllowed) {
            return;
        }
        var origContainer = origObject.asPrismObject();
        newObject.extension(new ExtensionType());

        for (var path: defaultAttributePaths) {
            anonymizeAttribute(newObject, origContainer, path, defaultAttributeNameAnonymizer);
        }

        if (origContainer.getExtension() != null) {
            PrismContainerDefinition<?> definition = origContainer.getExtensionContainerValue().getDefinition();
            var extensionAttributePaths = extractAttributePaths(definition);
            for (var path: extensionAttributePaths) {
                anonymizeAttribute(newObject, origContainer.getExtension(), path, extensionAttributeNameAnonymizer);
            }
        }
    }

    private void fillActivation(@NotNull FocusType origObject, @NotNull FocusType newObject) {
        if (origObject.getActivation() == null) {
            return;
        }
        var activation = new ActivationType().effectiveStatus(origObject.getActivation().getEffectiveStatus());
        newObject.setActivation(activation);
    }

}

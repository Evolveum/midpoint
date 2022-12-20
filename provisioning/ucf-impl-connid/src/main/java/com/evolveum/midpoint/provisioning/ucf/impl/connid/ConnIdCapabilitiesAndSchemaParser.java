/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.schema.processor.ObjectFactory.createRawResourceAttributeDefinition;
import static com.evolveum.midpoint.schema.processor.ObjectFactory.createResourceSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;

import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Class that can parse ConnId capabilities and schema into midPoint format.
 *
 * It is also used to hold the parsed capabilities and schema.
 *
 * This is a "builder" that builds/converts the schema. As such it
 * hides all the intermediary parsing states. Therefore the {@link ConnectorInstance}
 * always has a consistent schema, even during reconfigure and fetch operations.
 * There is either old schema or new schema, but there is no partially-parsed schema.
 *
 * May be used either for parsing both capabilities and schema (see
 * {@link #retrieveResourceCapabilitiesAndSchema(List, OperationResult)}) or for parsing
 * capabilities only (see {@link #retrieveResourceCapabilities(OperationResult)}).
 *
 * @author Radovan Semancik
 */
class ConnIdCapabilitiesAndSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(ConnIdCapabilitiesAndSchemaParser.class);

    private static final String OP_GET_SUPPORTED_OPERATIONS = ConnectorFacade.class.getName() + ".getSupportedOperations";
    private static final String OP_SCHEMA = ConnectorFacade.class.getName() + ".schema";

    // INPUT fields
    private final ConnIdNameMapper connIdNameMapper; // null if schema parsing is not needed
    private final ConnectorFacade connIdConnectorFacade;
    private final String connectorHumanReadableName;

    // Internal
    private Set<Class<? extends APIOperation>> connIdSupportedOperations;

    // IN/OUT
    /**
     * Does the resource use "legacy schema" i.e. `pass:[__ACCOUNT__]` and `pass:[__GROUP__]` object class names?
     * This information may be configured or detected from pre-configured schema. See {@link #detectLegacySchema(Schema)}.
     */
    private Boolean legacySchema;

    // OUTPUT fields

    /**
     * Is the `SchemaApiOp` capability supported?
     */
    private boolean supportsSchema;

    /**
     * Resource schema is usually parsed from ConnId schema (when {@link #supportsSchema} is `true`).
     * But if the connector does not have schema capability then resource schema can be null.
     *
     * Note that this schema is raw, i.e. `schemaHandling` and the like are not present.
     */
    private ResourceSchema rawResourceSchema;

    /**
     * Parsed capabilities.
     */
    private final CapabilityCollectionType capabilities = new CapabilityCollectionType();

    /** When schema parsing is requested. */
    ConnIdCapabilitiesAndSchemaParser(
            @NotNull ConnIdNameMapper connIdNameMapper,
            ConnectorFacade connIdConnectorFacade,
            String connectorHumanReadableName) {
        this.connIdNameMapper = connIdNameMapper;
        this.connIdConnectorFacade = connIdConnectorFacade;
        this.connectorHumanReadableName = connectorHumanReadableName;
    }

    /** When schema parsing is not necessary. */
    ConnIdCapabilitiesAndSchemaParser(
            ConnectorFacade connIdConnectorFacade,
            String connectorHumanReadableName) {
        this.connIdNameMapper = null;
        this.connIdConnectorFacade = connIdConnectorFacade;
        this.connectorHumanReadableName = connectorHumanReadableName;
    }

    /**
     * Returns parsed resource schema or null if it was not available.
     *
     * The schema is raw.
     */
    ResourceSchema getRawResourceSchema() {
        return rawResourceSchema;
    }

    /**
     * Returns parsed capabilities or null if they were not available.
     */
    public @NotNull CapabilityCollectionType getCapabilities() {
        return capabilities;
    }

    /**
     * Returns true if the resource uses legacy object class names (e.g. `pass:[__ACCOUNT__]`),
     * false if not, and null if this fact could not be determined.
     */
    public Boolean getLegacySchema() {
        return legacySchema;
    }

    void setLegacySchema(Boolean legacySchema) {
        this.legacySchema = legacySchema;
    }

    /**
     * Retrieves and parses resource capabilities and schema.
     *
     * Schema is immutable after this method.
     */
    void retrieveResourceCapabilitiesAndSchema(List<QName> generateObjectClasses, OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException, SchemaException {

        fetchSupportedOperations(result);

        // sets supportsSchema flag
        processOperationCapabilities();

        if (supportsSchema) {

            Schema connIdSchema = fetchConnIdSchema(result);

            if (connIdSchema == null) {
                addBasicReadCapability();
            } else {
                parseResourceSchema(connIdSchema, generateObjectClasses);
            }
        }
    }

    /**
     * Retrieves and parses resource capabilities.
     */
    void retrieveResourceCapabilities(OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {
        fetchSupportedOperations(result);
        processOperationCapabilities();
    }

    private void fetchSupportedOperations(OperationResult parentResult)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {
        OperationResult result = parentResult.createSubresult(OP_GET_SUPPORTED_OPERATIONS);
        result.addContext("connector", connIdConnectorFacade.getClass());

        try {
            LOGGER.debug("Fetching supported connector operations from {}", connectorHumanReadableName);
            InternalMonitor.recordConnectorOperation("getSupportedOperations");

            connIdSupportedOperations = connIdConnectorFacade.getSupportedOperations();

            LOGGER.trace("Connector supported operations: {}", connIdSupportedOperations);
            result.recordSuccess();

        } catch (UnsupportedOperationException ex) {
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, ex.getMessage());
        } catch (Throwable ex) {
            Throwable midpointEx = ConnIdUtil.processConnIdException(ex, connectorHumanReadableName, result);
            result.recordFatalError(midpointEx.getMessage(), midpointEx);
            castAndThrowException(ex, midpointEx);
        } finally {
            result.close();
        }
    }

    /**
     * Casts arbitrary {@link Throwable} to a supported kind of exception.
     */
    private void castAndThrowException(Throwable originalException, Throwable convertedException)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {

        if (convertedException instanceof CommunicationException) {
            throw (CommunicationException) convertedException;
        } else if (convertedException instanceof ConfigurationException) {
            throw (ConfigurationException) convertedException;
        } else if (convertedException instanceof GenericFrameworkException) {
            throw (GenericFrameworkException) convertedException;
        } else if (convertedException instanceof RuntimeException) {
            throw (RuntimeException) convertedException;
        } else if (convertedException instanceof Error) {
            throw (Error) convertedException;
        } else {
            throw new SystemException("Got unexpected exception: " + originalException.getClass().getName()
                    + ": " + originalException.getMessage(), originalException);
        }
    }

    private Schema fetchConnIdSchema(OperationResult parentResult)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {
        // Connector operation cannot create result for itself, so we need to create result for it
        OperationResult result = parentResult.createSubresult(OP_SCHEMA);
        result.addContext("connector", connIdConnectorFacade.getClass());
        try {

            LOGGER.debug("Fetching schema from {}", connectorHumanReadableName);
            // Fetch the schema from the connector (which actually gets that from the resource).
            InternalMonitor.recordConnectorOperation("schema");
            // TODO have context present
            Schema connIdSchema = connIdConnectorFacade.schema();
            if (connIdSchema == null) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Null schema returned");
            } else {
                result.recordSuccess();
            }
            return connIdSchema;
        } catch (UnsupportedOperationException ex) {
            //recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
            // The connector does no support schema() operation.
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, ex.getMessage());
            return null;
        } catch (Throwable ex) {
            //recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
            // conditions.
            // Therefore this kind of heavy artillery is necessary.
            // ICF interface does not specify exceptions or other error
            // TODO maybe we can try to catch at least some specific exceptions
            Throwable midpointEx = ConnIdUtil.processConnIdException(ex, connectorHumanReadableName, result);
            result.recordFatalError(midpointEx.getMessage(), midpointEx);
            castAndThrowException(ex, midpointEx);
            throw new AssertionError("not here");
        } finally {
            result.close();
        }
    }

    /**
     * Create capabilities from supported connector operations
     */
    private void processOperationCapabilities() {

        if (connIdSupportedOperations.contains(SchemaApiOp.class)) {
            capabilities.setSchema(new SchemaCapabilityType());
            supportsSchema = true;
        } else {
            supportsSchema = false;
        }

        if (connIdSupportedOperations.contains(DiscoverConfigurationApiOp.class)) {
            capabilities.setDiscoverConfiguration(new DiscoverConfigurationCapabilityType());
        }

        if (connIdSupportedOperations.contains(SyncApiOp.class)) {
            capabilities.setLiveSync(new LiveSyncCapabilityType());
        }

        if (connIdSupportedOperations.contains(TestApiOp.class)) {
            capabilities.setTestConnection(new TestConnectionCapabilityType());
        }

        if (connIdSupportedOperations.contains(CreateApiOp.class)){
            capabilities.setCreate(new CreateCapabilityType());
        }

        // GetApiOp is processed later. We need supported options (from schema) to fully process it.

        if (connIdSupportedOperations.contains(UpdateDeltaApiOp.class)) {
            UpdateCapabilityType capUpdate = new UpdateCapabilityType();
            capUpdate.setDelta(true);
            // This is the default for all resources.
            // (Currently there is no way how to obtain it from the connector.)
            // It can be disabled manually.
            capUpdate.setAddRemoveAttributeValues(true);
            capabilities.setUpdate(capUpdate);
        } else if (connIdSupportedOperations.contains(UpdateApiOp.class)) {
            UpdateCapabilityType capUpdate = new UpdateCapabilityType();
            // This is the default for all resources.
            // (Currently there is no way how to obtain it from the connector.)
            // It can be disabled manually.
            capUpdate.setAddRemoveAttributeValues(true);
            capabilities.setUpdate(capUpdate);
        }

        if (connIdSupportedOperations.contains(DeleteApiOp.class)) {
            capabilities.setDelete(new DeleteCapabilityType());
        }

        if (connIdSupportedOperations.contains(ScriptOnResourceApiOp.class)
                || connIdSupportedOperations.contains(ScriptOnConnectorApiOp.class)) {
            ScriptCapabilityType capScript = new ScriptCapabilityType();
            if (connIdSupportedOperations.contains(ScriptOnResourceApiOp.class)) {
                ScriptCapabilityHostType host = new ScriptCapabilityHostType();
                host.setType(ProvisioningScriptHostType.RESOURCE);
                capScript.getHost().add(host);
                // language is unknown here
            }
            if (connIdSupportedOperations.contains(ScriptOnConnectorApiOp.class)) {
                ScriptCapabilityHostType host = new ScriptCapabilityHostType();
                host.setType(ProvisioningScriptHostType.CONNECTOR);
                capScript.getHost().add(host);
                // language is unknown here
            }
            capabilities.setScript(capScript);
        }
    }

    private void addBasicReadCapability() {
        // Still need to add "read" capability. This capability would be added during schema processing,
        // because it depends on schema options. But if there is no schema we need to add read capability
        // anyway. We do not want to end up with non-readable resource.
        if (connIdSupportedOperations.contains(GetApiOp.class) || connIdSupportedOperations.contains(SearchApiOp.class)) {
            capabilities.setRead(new ReadCapabilityType());
        }
    }

    /**
     * On exit, {@link #rawResourceSchema} is set and is immutable.
     */
    private void parseResourceSchema(@NotNull Schema connIdSchema, List<QName> objectClassesToGenerate) throws SchemaException {

        SpecialAttributes specialAttributes = new SpecialAttributes();
        rawResourceSchema = createResourceSchema();

        if (legacySchema == null) {
            legacySchema = detectLegacySchema(connIdSchema);
        }
        LOGGER.trace("Converting resource schema (legacy mode: {})", legacySchema);
        LOGGER.trace("Generating object classes: {}", objectClassesToGenerate);

        Set<ObjectClassInfo> objectClassInfoSet = connIdSchema.getObjectClassInfo();
        for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {
            parseObjectClass(objectClassInfo, objectClassesToGenerate, specialAttributes);
        }

        updateCapabilitiesFromSchema(connIdSchema, specialAttributes);

        rawResourceSchema.freeze();
    }

    private void parseObjectClass(
            @NotNull ObjectClassInfo objectClassInfo,
            List<QName> objectClassesToGenerate,
            @NotNull SpecialAttributes specialAttributes) throws SchemaException {

        assert connIdNameMapper != null : "accessing schema without mapper?";
        // "Flat" ConnId object class names needs to be mapped to QNames
        QName objectClassXsdName = connIdNameMapper.objectClassToQname(
                new ObjectClass(objectClassInfo.getType()), legacySchema);

        if (!shouldBeGenerated(objectClassesToGenerate, objectClassXsdName)) {
            LOGGER.trace("Skipping object class {} ({})", objectClassInfo.getType(), objectClassXsdName);
            return;
        }

        LOGGER.trace("Converting object class {} ({})", objectClassInfo.getType(), objectClassXsdName);

        MutableResourceObjectClassDefinition ocDef = ResourceObjectClassDefinitionImpl.raw(objectClassXsdName);
        // ocDef is added to the schema at the end

        // The __ACCOUNT__ objectclass in ConnId is a default account objectclass. So mark it appropriately.
        if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
            ocDef.setDefaultAccountDefinition(true);
        }

        RawResourceAttributeDefinition<?> uidDefinition = null;
        RawResourceAttributeDefinition<?> nameDefinition = null;
        boolean hasUidDefinition = false;

        int displayOrder = ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_START;
        // Let's iterate over all attributes in this object class ...
        Set<AttributeInfo> attributeInfoSet = objectClassInfo.getAttributeInfo();
        for (AttributeInfo attributeInfo : attributeInfoSet) {
            String icfName = attributeInfo.getName();

            boolean isSpecial = specialAttributes.updateWithAttribute(attributeInfo);
            if (isSpecial) {
                // Skip this attribute, capability is sufficient
                continue;
            }

            // __NAME__ and __UID__ are replaced by their native names, if possible
            String attributeNameToUse;
            if ((Name.NAME.equals(icfName) || Uid.NAME.equals(icfName)) && attributeInfo.getNativeName() != null) {
                attributeNameToUse = attributeInfo.getNativeName();
            } else {
                attributeNameToUse = icfName;
            }

            QName attrXsdName = connIdNameMapper.convertAttributeNameToQName(attributeNameToUse, ocDef);
            QName attrXsdType = connIdTypeToXsdType(attributeInfo);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("  attr conversion ConnId: {}({}) -> XSD: {}({})",
                        icfName, attributeInfo.getType().getSimpleName(),
                        PrettyPrinter.prettyPrint(attrXsdName), PrettyPrinter.prettyPrint(attrXsdType));
            }

            MutableRawResourceAttributeDefinition<?> attrDef =
                    createRawResourceAttributeDefinition(attrXsdName, attrXsdType);

            attrDef.setMatchingRuleQName(
                    connIdAttributeInfoToMatchingRule(attributeInfo));

            if (Name.NAME.equals(icfName)) {
                nameDefinition = attrDef;
                if (uidDefinition != null && attrXsdName.equals(uidDefinition.getItemName())) {
                    attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                    uidDefinition = attrDef;
                    hasUidDefinition = true;
                } else {
                    if (attributeInfo.getNativeName() == null) {
                        // Set a better display name for __NAME__. The "name" is s very overloaded term,
                        // so let's try to make things a bit clearer.
                        attrDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_NAME);
                    }
                    attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_ORDER);
                }

            } else if (Uid.NAME.equals(icfName)) {
                // UID can be the same as other attribute
                ResourceAttributeDefinition<?> existingDefinition = ocDef.findAttributeDefinition(attrXsdName);
                if (existingDefinition != null) {
                    hasUidDefinition = true;
                    uidDefinition = existingDefinition.spawnModifyingRaw(
                            def -> def.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER));
                    ocDef.replaceDefinition(attrXsdName, uidDefinition);
                    continue;
                } else {
                    uidDefinition = attrDef;
                    if (attributeInfo.getNativeName() == null) {
                        attrDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
                    }
                    attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                }

            } else {
                // Check conflict with UID definition
                if (uidDefinition != null && attrXsdName.equals(uidDefinition.getItemName())) {
                    attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                    uidDefinition = attrDef;
                    hasUidDefinition = true;
                } else {
                    attrDef.setDisplayOrder(displayOrder);
                    displayOrder += ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_INCREMENT;
                }
            }

            attrDef.setNativeAttributeName(attributeInfo.getNativeName());
            attrDef.setFrameworkAttributeName(icfName);

            processAttributeFlags(attributeInfo, attrDef);

            if (!Uid.NAME.equals(icfName)) {
                attrDef.freeze(); // This eliminates a clone operation during addition
                ocDef.add(attrDef);
            }
        }

        if (uidDefinition == null) {
            // Every object has UID in ConnId, therefore add a default definition if no other was specified
            MutableRawResourceAttributeDefinition<?> replacement =
                    createRawResourceAttributeDefinition(SchemaConstants.ICFS_UID, DOMUtil.XSD_STRING);
            // DO NOT make it mandatory. It must not be present on create hence it cannot be mandatory.
            replacement.setMinOccurs(0);
            replacement.setMaxOccurs(1);
            // Make it read-only
            replacement.setReadOnly();
            // Set a default display name
            replacement.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
            replacement.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
            // Uid is a primary identifier of every object (this is the ConnId way)
            uidDefinition = replacement;
        }
        if (!hasUidDefinition) {
            uidDefinition.freeze(); // This eliminates a clone operation during addition
            ocDef.add(uidDefinition);
        }
        ocDef.addPrimaryIdentifierName(uidDefinition.getItemName());
        if (nameDefinition != null && !uidDefinition.getItemName().equals(nameDefinition.getItemName())) {
            ocDef.addSecondaryIdentifierName(nameDefinition.getItemName());
        }

        // Add schema annotations
        ocDef.setNativeObjectClass(objectClassInfo.getType());
        if (nameDefinition != null) {
            ocDef.setDisplayNameAttributeName(nameDefinition.getItemName());
            ocDef.setNamingAttributeName(nameDefinition.getItemName());
        }
        ocDef.setAuxiliary(objectClassInfo.isAuxiliary());

        rawResourceSchema.toMutable().add(ocDef);

        LOGGER.trace("  ... converted object class {}: {}", objectClassInfo.getType(), ocDef);
    }

    /**
     * Process flags such as optional and multi-valued - convert them to attribute definition properties.
     */
    private void processAttributeFlags(AttributeInfo attributeInfo, MutableRawResourceAttributeDefinition<?> attrDef) {
        Set<Flags> flagsSet = attributeInfo.getFlags();

        // Default values
        attrDef.setMinOccurs(0);
        attrDef.setMaxOccurs(1);
        attrDef.setCanRead(true);
        attrDef.setCanAdd(true);
        attrDef.setCanModify(true);
        // "returned by default" is null if not specified

        for (Flags flags : flagsSet) {
            if (flags == Flags.REQUIRED) {
                attrDef.setMinOccurs(1);
            }
            if (flags == Flags.MULTIVALUED) {
                attrDef.setMaxOccurs(-1);
            }
            if (flags == Flags.NOT_READABLE) {
                attrDef.setCanRead(false);
            }
            if (flags == Flags.NOT_CREATABLE) {
                attrDef.setCanAdd(false);
            }
            if (flags == Flags.NOT_UPDATEABLE) {
                attrDef.setCanModify(false);
            }
            if (flags == Flags.NOT_RETURNED_BY_DEFAULT) {
                attrDef.setReturnedByDefault(false);
            }
        }
    }

    private void updateCapabilitiesFromSchema(
            @NotNull Schema connIdSchema,
            SpecialAttributes specialAttributes) {
        ActivationCapabilityType capAct = null;

        if (specialAttributes.enableAttributeInfo != null) {
            capAct = new ActivationCapabilityType();
            ActivationStatusCapabilityType capActStatus = new ActivationStatusCapabilityType();
            capAct.setStatus(capActStatus);
            if (!specialAttributes.enableAttributeInfo.isReturnedByDefault()) {
                capActStatus.setReturnedByDefault(false);
            }
        }

        if (specialAttributes.enableDateAttributeInfo != null) {
            if (capAct == null) {
                capAct = new ActivationCapabilityType();
            }
            ActivationValidityCapabilityType capValidFrom = new ActivationValidityCapabilityType();
            capAct.setValidFrom(capValidFrom);
            if (!specialAttributes.enableDateAttributeInfo.isReturnedByDefault()) {
                capValidFrom.setReturnedByDefault(false);
            }
        }

        if (specialAttributes.disableDateAttributeInfo != null) {
            if (capAct == null) {
                capAct = new ActivationCapabilityType();
            }
            ActivationValidityCapabilityType capValidTo = new ActivationValidityCapabilityType();
            capAct.setValidTo(capValidTo);
            if (!specialAttributes.disableDateAttributeInfo.isReturnedByDefault()) {
                capValidTo.setReturnedByDefault(false);
            }
        }

        if (specialAttributes.lockoutAttributeInfo != null) {
            if (capAct == null) {
                capAct = new ActivationCapabilityType();
            }
            ActivationLockoutStatusCapabilityType capActStatus = new ActivationLockoutStatusCapabilityType();
            capAct.setLockoutStatus(capActStatus);
            if (!specialAttributes.lockoutAttributeInfo.isReturnedByDefault()) {
                capActStatus.setReturnedByDefault(false);
            }
        }

        // TODO: activation and credentials should be per-objectclass capabilities
        if (capAct != null) {
            capabilities.setActivation(capAct);
        }

        if (specialAttributes.passwordAttributeInfo != null) {
            CredentialsCapabilityType capCred = new CredentialsCapabilityType();
            PasswordCapabilityType capPass = new PasswordCapabilityType();
            if (!specialAttributes.passwordAttributeInfo.isReturnedByDefault()) {
                capPass.setReturnedByDefault(false);
            }
            if (specialAttributes.passwordAttributeInfo.isReadable()) {
                capPass.setReadable(true);
            }
            capCred.setPassword(capPass);
            capabilities.setCredentials(capCred);
        }

        if (specialAttributes.auxiliaryObjectClassAttributeInfo != null) {
            capabilities.setAuxiliaryObjectClasses(new AuxiliaryObjectClassesCapabilityType());
        }

        boolean canPageSize = false;
        boolean canPageOffset = false;
        boolean canSort = false;
        boolean supportsReturnDefaultAttributes = false;
        for (OperationOptionInfo searchOption: connIdSchema.getSupportedOptionsByOperation(SearchApiOp.class)) {
            switch (searchOption.getName()) {
                case OperationOptions.OP_PAGE_SIZE:
                    canPageSize = true;
                    break;
                case OperationOptions.OP_PAGED_RESULTS_OFFSET:
                    canPageOffset = true;
                    break;
                case OperationOptions.OP_SORT_KEYS:
                    canSort = true;
                    break;
                case OperationOptions.OP_RETURN_DEFAULT_ATTRIBUTES:
                    supportsReturnDefaultAttributes = true;
                    break;
            }
        }
        if (canPageSize || canPageOffset || canSort) {
            capabilities.setPagedSearch(new PagedSearchCapabilityType());
        }

        if (connIdSupportedOperations.contains(GetApiOp.class) || connIdSupportedOperations.contains(SearchApiOp.class)) {
            ReadCapabilityType capRead = new ReadCapabilityType();
            capRead.setReturnDefaultAttributesOption(supportsReturnDefaultAttributes);
            capabilities.setRead(capRead);
        }
        if (connIdSupportedOperations.contains(UpdateDeltaApiOp.class)) {
            processUpdateOperationOptions(connIdSchema.getSupportedOptionsByOperation(UpdateDeltaApiOp.class));
        } else if (connIdSupportedOperations.contains(UpdateApiOp.class)) {
            processUpdateOperationOptions(connIdSchema.getSupportedOptionsByOperation(UpdateApiOp.class));
        }
    }

    private boolean detectLegacySchema(Schema icfSchema) {
        Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
        for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {
            if (objectClassInfo.is(ObjectClass.ACCOUNT_NAME) || objectClassInfo.is(ObjectClass.GROUP_NAME)) {
                LOGGER.trace("This is legacy schema");
                return true;
            }
        }
        return false;
    }

    private boolean shouldBeGenerated(List<QName> generateObjectClasses, QName objectClassXsdName) {
        if (generateObjectClasses == null || generateObjectClasses.isEmpty()) {
            return true;
        }

        for (QName objClassToGenerate : generateObjectClasses) {
            if (objClassToGenerate.equals(objectClassXsdName)) {
                return true;
            }
        }

        return false;
    }

    private QName connIdAttributeInfoToMatchingRule(AttributeInfo attributeInfo) {
        String connIdSubtype = attributeInfo.getSubtype();
        if (connIdSubtype == null) {
            return null;
        }
        if (AttributeInfo.Subtypes.STRING_CASE_IGNORE.toString().equals(connIdSubtype)) {
            return PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME;
        }
        if (AttributeInfo.Subtypes.STRING_LDAP_DN.toString().equals(connIdSubtype)) {
            return PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME;
        }
        if (AttributeInfo.Subtypes.STRING_XML.toString().equals(connIdSubtype)) {
            return PrismConstants.XML_MATCHING_RULE_NAME;
        }
        if (AttributeInfo.Subtypes.STRING_UUID.toString().equals(connIdSubtype)) {
            return PrismConstants.UUID_MATCHING_RULE_NAME;
        }
        LOGGER.debug("Unknown subtype {} defined for attribute {}, ignoring (no matching rule definition)", connIdSubtype, attributeInfo.getName());
        return null;
    }

    private void processUpdateOperationOptions(Set<OperationOptionInfo> supportedOptions) {
        boolean canRunAsUser = false;
        for (OperationOptionInfo searchOption: supportedOptions) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (searchOption.getName()) {
                case OperationOptions.OP_RUN_AS_USER:
                    canRunAsUser = true;
                    break;
                // TODO: run as password
            }
        }
        if (canRunAsUser) {
            capabilities.setRunAs(new RunAsCapabilityType());
        }
    }

    private static QName connIdTypeToXsdType(AttributeInfo attrInfo) throws SchemaException {
        if (Map.class.isAssignableFrom(attrInfo.getType())) {
            // ConnId type is "Map". We need more precise definition on midPoint side.
            String subtype = attrInfo.getSubtype();
            if (subtype == null) {
                throw new SchemaException("Attribute "+attrInfo.getName()+" defined as Map, but there is no subtype");
            }
            if (SchemaConstants.ICF_SUBTYPES_POLYSTRING_URI.equals(subtype)) {
                    return PolyStringType.COMPLEX_TYPE;
            } else {
                throw new SchemaException("Attribute "+attrInfo.getName()+" defined as Map, but there is unsupported subtype '"+subtype+"'");
            }
        }
        return connIdTypeToXsdType(attrInfo.getType(), false);
    }

    static QName connIdTypeToXsdType(Class<?> type, boolean isConfidential) {
        // For arrays we are only interested in the component type
        if (isMultivaluedType(type)) {
            type = type.getComponentType();
        }
        QName propXsdType;
        if (GuardedString.class.equals(type) ||
                (String.class.equals(type) && isConfidential)) {
            // GuardedString is a special case. It is a ICF-specific
            // type
            // implementing Potemkin-like security. Use a temporary
            // "nonsense" type for now, so this will fail in tests and
            // will be fixed later
//            propXsdType = SchemaConstants.T_PROTECTED_STRING_TYPE;
            propXsdType = ProtectedStringType.COMPLEX_TYPE;
        } else if (GuardedByteArray.class.equals(type) ||
                (Byte.class.equals(type) && isConfidential)) {
            // GuardedString is a special case. It is a ICF-specific
            // type
            // implementing Potemkin-like security. Use a temporary
            // "nonsense" type for now, so this will fail in tests and
            // will be fixed later
//            propXsdType = SchemaConstants.T_PROTECTED_BYTE_ARRAY_TYPE;
            propXsdType = ProtectedByteArrayType.COMPLEX_TYPE;
        } else {
            propXsdType = XsdTypeMapper.toXsdType(type);
        }
        return propXsdType;
    }

    static boolean isMultivaluedType(Class<?> type) {
        // We consider arrays to be multi-valued
        // ... unless it is byte[] or char[]
        return type.isArray() && !type.equals(byte[].class) && !type.equals(char[].class);
    }

    /**
     * Information about special attributes present in the schema. We use them to determine specific capabilities.
     */
    private static class SpecialAttributes {
        AttributeInfo passwordAttributeInfo;
        AttributeInfo enableAttributeInfo;
        AttributeInfo enableDateAttributeInfo;
        AttributeInfo disableDateAttributeInfo;
        AttributeInfo lockoutAttributeInfo;
        AttributeInfo auxiliaryObjectClassAttributeInfo;

        /**
         * Updates the current knowledge about special attributes with the currently parsed attribute.
         * Returns true if the match was found (i.e. current attribute is "special"), so the current attribute
         * should not be listed among normal ones.
         */
        boolean updateWithAttribute(@NotNull AttributeInfo attributeInfo) {
            String icfName = attributeInfo.getName();
            if (OperationalAttributes.PASSWORD_NAME.equals(icfName)) {
                passwordAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.ENABLE_NAME.equals(icfName)) {
                enableAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.ENABLE_DATE_NAME.equals(icfName)) {
                enableDateAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.DISABLE_DATE_NAME.equals(icfName)) {
                disableDateAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.LOCK_OUT_NAME.equals(icfName)) {
                lockoutAttributeInfo = attributeInfo;
                return true;
            }

            if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equals(icfName)) {
                auxiliaryObjectClassAttributeInfo = attributeInfo;
                return true;
            }

            return false;
        }
    }
}

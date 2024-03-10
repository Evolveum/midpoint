/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.connIdAttributeNameToUcf;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.connIdObjectClassNameToUcf;
import static com.evolveum.midpoint.schema.processor.ObjectFactory.createRawResourceAttributeDefinition;
import static com.evolveum.midpoint.schema.processor.ObjectFactory.createResourceSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.objects.*;
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
            ConnectorFacade connIdConnectorFacade,
            String connectorHumanReadableName) {
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
    Boolean getLegacySchema() {
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
    void retrieveResourceCapabilitiesAndSchema(@NotNull List<QName> objectClassesToGenerate, OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException, SchemaException {

        fetchSupportedOperations(result);

        // sets supportsSchema flag
        processOperationCapabilities();

        if (supportsSchema) {

            Schema connIdSchema = fetchConnIdSchema(result);

            if (connIdSchema == null) {
                addBasicReadCapability();
            } else {
                parseResourceSchema(connIdSchema, objectClassesToGenerate);
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
    private void parseResourceSchema(@NotNull Schema connIdSchema, @NotNull List<QName> objectClassesToGenerate)
            throws SchemaException {

        SpecialAttributes specialAttributes = new SpecialAttributes();
        rawResourceSchema = createResourceSchema();

        if (legacySchema == null) {
            legacySchema = detectLegacySchema(connIdSchema);
        }
        LOGGER.trace("Converting resource schema (legacy mode: {})", legacySchema);
        LOGGER.trace("Generating object classes: {}", objectClassesToGenerate);

        Set<ObjectClassInfo> objectClassInfoSet = connIdSchema.getObjectClassInfo();
        for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {
            // "Flat" ConnId object class names needs to be mapped to QNames
            var objectClassXsdName = connIdObjectClassNameToUcf(objectClassInfo.getType(), legacySchema);

            if (!shouldBeGenerated(objectClassesToGenerate, objectClassXsdName)) {
                LOGGER.trace("Skipping object class {} ({})", objectClassInfo.getType(), objectClassXsdName);
                return;
            }

            LOGGER.trace("Converting object class {} ({})", objectClassInfo.getType(), objectClassXsdName);

            new ObjectClassParser(objectClassXsdName, objectClassInfo, specialAttributes)
                    .parse();
        }

        updateCapabilitiesFromSchema(connIdSchema, specialAttributes);

        rawResourceSchema.freeze();
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

    private boolean shouldBeGenerated(@NotNull List<QName> generateObjectClasses, QName objectClassXsdName) {
        return generateObjectClasses.isEmpty()
                || generateObjectClasses.contains(objectClassXsdName);
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

    private static XsdTypeInformation connIdAttributeInfoToXsdTypeInfo(AttributeInfo attrInfo) throws SchemaException {
        if (Map.class.isAssignableFrom(attrInfo.getType())) {
            // ConnId type is "Map". We need more precise definition on midPoint side.
            String subtype = MiscUtil.requireNonNull(
                    attrInfo.getSubtype(),
                    () -> "Attribute " + attrInfo.getName() + " defined as Map, but there is no subtype");
            if (SchemaConstants.ICF_SUBTYPES_POLYSTRING_URI.equals(subtype)) {
                return new XsdTypeInformation(PolyStringType.COMPLEX_TYPE, false);
            } else {
                throw new SchemaException(
                        "Attribute %s defined as Map, but there is unsupported subtype '%s'".formatted(
                                attrInfo.getName(), subtype));
            }
        }
        return connIdTypeToXsdTypeInfo(attrInfo.getType(), false);
    }

    /**
     * Converts ConnId type (used in connector objects and configuration properties)
     * to a midPoint type (used in XSD schemas and beans).
     *
     * The string attributes are treated as xsd:string at this point. Later on, they may be converted to {@link PolyStringType},
     * if there are matching/normalization rules defined.
     */
    static XsdTypeInformation connIdTypeToXsdTypeInfo(Class<?> type, boolean isConfidential) {
        boolean multivalue;
        Class<?> componentType;
        // For multi-valued properties we are only interested in the component type.
        // We consider arrays to be multi-valued ... unless it is byte[] or char[]
        if (type.isArray() && !type.equals(byte[].class) && !type.equals(char[].class)) {
            multivalue = true;
            componentType = type.getComponentType();
        } else {
            multivalue = false;
            componentType = type;
        }
        QName xsdTypeName;
        if (GuardedString.class.equals(componentType)
                || String.class.equals(componentType) && isConfidential) {
            // GuardedString is a special case. It is a ICF-specific type implementing Potemkin-like security.
            xsdTypeName = ProtectedStringType.COMPLEX_TYPE;
        } else if (GuardedByteArray.class.equals(componentType)
                || Byte.class.equals(componentType) && isConfidential) {
            // GuardedByteArray is a special case. It is a ICF-specific type implementing Potemkin-like security.
            xsdTypeName = ProtectedByteArrayType.COMPLEX_TYPE;
        } else if (ConnectorObjectReference.class.equals(componentType)) {
            xsdTypeName = ShadowAssociationValueType.COMPLEX_TYPE;
        } else {
            xsdTypeName = XsdTypeMapper.toXsdType(componentType);
        }
        return new XsdTypeInformation(xsdTypeName, multivalue);
    }

    /** Parses single ConnId object class - if applicable. */
    private class ObjectClassParser {

        /** ConnId style definition. */
        @NotNull private final ObjectClassInfo connIdClassInfo;

        /** Resulting midPoint definition (raw). */
        @NotNull private final MutableResourceObjectClassDefinition mpClassDef;

        /** Collecting information about supported special attributes (should be per object class, but currently is global). */
        @NotNull private final SpecialAttributes specialAttributes;

        /** What display order will individual attributes have. */
        int currentAttributeDisplayOrder = ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_START;

        /**
         * The definition of UID attribute, if present, and not overlapping with a different one.
         * In the case of overlap, the other attribute takes precedence, and its definition is used here.
         */
        private MutableRawResourceAttributeDefinition<?> uidDefinition;

        /** True if UID is the same as NAME or another attribute, and therefore its definition was taken from it. */
        private boolean uidDefinitionTakenFromAnotherAttribute;

        /** The definition of NAME attribute, if present. */
        private MutableRawResourceAttributeDefinition<?> nameDefinition;

        ObjectClassParser(
                @NotNull QName objectClassXsdName,
                @NotNull ObjectClassInfo objectClassInfo,
                @NotNull SpecialAttributes specialAttributes) {

            this.connIdClassInfo = objectClassInfo;

            // added to resource schema at the end
            this.mpClassDef = ResourceObjectClassDefinitionImpl.raw(objectClassXsdName);

            this.specialAttributes = specialAttributes;
        }

        private void parse() throws SchemaException {

            // The __ACCOUNT__ objectclass in ConnId is a default account objectclass. So mark it appropriately.
            if (ObjectClass.ACCOUNT_NAME.equals(connIdClassInfo.getType())) {
                mpClassDef.setDefaultAccountDefinition(true);
            }

            for (AttributeInfo connIdAttrInfo : connIdClassInfo.getAttributeInfo()) {

                boolean isSpecial = specialAttributes.updateWithAttribute(connIdAttrInfo);
                if (isSpecial) {
                    continue; // Skip this attribute, capability is sufficient
                }

                var xsdAttrName = connIdAttributeNameToUcf(
                        connIdAttrInfo.getName(), connIdAttrInfo.getNativeName(), mpClassDef);

                // We are interested only in the name; multiplicity is driven by attribute flag
                var xsdAttrTypeName = connIdAttributeInfoToXsdTypeInfo(connIdAttrInfo).xsdTypeName();

                if (ShadowAssociationValueType.COMPLEX_TYPE.equals(xsdAttrTypeName)) {
                    parseAssociationInfo(xsdAttrName, connIdAttrInfo);
                } else {
                    parseAttributeInfo(xsdAttrName, xsdAttrTypeName, connIdAttrInfo);
                }
            }

            if (uidDefinition == null) {
                // Every object has UID in ConnId, therefore add a default definition if no other was specified.
                // Uid is a primary identifier of every object (this is the ConnId way).
                MutableRawResourceAttributeDefinition<?> replacement =
                        createRawResourceAttributeDefinition(SchemaConstants.ICFS_UID, DOMUtil.XSD_STRING);
                replacement.setMinOccurs(0); // It must not be present on create hence it cannot be mandatory.
                replacement.setMaxOccurs(1);
                replacement.setReadOnly();
                replacement.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
                replacement.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                uidDefinition = replacement;
            }
            if (uidDefinitionTakenFromAnotherAttribute) {
                // It was already added when that attribute was parsed.
            } else {
                mpClassDef.add(uidDefinition);
            }

            // Primary and secondary identifier information
            mpClassDef.addPrimaryIdentifierName(uidDefinition.getItemName());
            if (nameDefinition != null && !uidDefinition.getItemName().equals(nameDefinition.getItemName())) {
                mpClassDef.addSecondaryIdentifierName(nameDefinition.getItemName());
            }

            // Add other schema annotations
            mpClassDef.setNativeObjectClass(connIdClassInfo.getType());
            if (nameDefinition != null) {
                mpClassDef.setDisplayNameAttributeName(nameDefinition.getItemName());
                mpClassDef.setNamingAttributeName(nameDefinition.getItemName());
            }
            mpClassDef.setAuxiliary(connIdClassInfo.isAuxiliary());

            rawResourceSchema.toMutable().add(mpClassDef);

            LOGGER.trace("  ... converted object class {}: {}", connIdClassInfo.getType(), mpClassDef);
        }

        private void parseAttributeInfo(QName xsdAttrName, QName xsdAttrTypeName, AttributeInfo connIdAttrInfo) {
            String connIdAttrName = connIdAttrInfo.getName();

            LOGGER.trace("  attribute conversion: ConnId: {}({}) -> XSD: {}({})",
                    connIdAttrName, connIdAttrInfo.getType().getSimpleName(),
                    PrettyPrinter.prettyPrintLazily(xsdAttrName),
                    PrettyPrinter.prettyPrintLazily(xsdAttrTypeName));

            RawResourceAttributeDefinition<?> mpAttrDef = createRawResourceAttributeDefinition(xsdAttrName, xsdAttrTypeName);

            mpAttrDef.setMatchingRuleQName(
                    connIdAttributeInfoToMatchingRule(connIdAttrInfo));

            if (Uid.NAME.equals(connIdAttrName)) {
                // UID can be the same as a different attribute (maybe NAME, maybe some other). We take that one as authoritative.
                //noinspection rawtypes
                var existingDefinition = (MutableRawResourceAttributeDefinition) mpClassDef.findAttributeDefinition(xsdAttrName);
                if (existingDefinition != null) {
                    LOGGER.trace("Using other attribute definition for UID: {}", existingDefinition);
                    existingDefinition.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                    uidDefinition = existingDefinition;
                    uidDefinitionTakenFromAnotherAttribute = true;
                    return; // done with this attribute
                } else {
                    uidDefinition = mpAttrDef;
                    if (connIdAttrInfo.getNativeName() == null) {
                        mpAttrDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
                    }
                    mpAttrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                }
            } else {
                if (Name.NAME.equals(connIdAttrName)) {
                    nameDefinition = mpAttrDef;
                }
                // Check for a conflict with UID definition. This definition will take precedence of the one from UID.
                // (But beware, we may not have come across UID definition yet. The code above cares for that.)
                if (uidDefinition != null && xsdAttrName.equals(uidDefinition.getItemName())) {
                    LOGGER.trace("Using other attribute definition for UID: {}", mpAttrDef);
                    mpAttrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                    uidDefinition = mpAttrDef;
                    uidDefinitionTakenFromAnotherAttribute = true;
                } else {
                    // Set the display order + display name appropriately
                    if (Name.NAME.equals(connIdAttrName)) {
                        if (connIdAttrInfo.getNativeName() == null) {
                            // Set a better display name for __NAME__. The "name" is s very overloaded term,
                            // so let's try to make things a bit clearer.
                            mpAttrDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_NAME);
                        }
                        mpAttrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_ORDER);
                    } else {
                        mpAttrDef.setDisplayOrder(currentAttributeDisplayOrder);
                        currentAttributeDisplayOrder += ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_INCREMENT;
                    }
                }

            }

            processAttributePrismInfo(connIdAttrInfo, mpAttrDef);
            processAttributeUcfInfo(connIdAttrInfo, mpAttrDef);

            if (!Uid.NAME.equals(connIdAttrName)) {
                mpClassDef.add(mpAttrDef);
            } else {
                // UID is added after parsing all attributes
            }
        }

        private void parseAssociationInfo(QName xsdAssocName, AttributeInfo connIdAttrInfo) {

            String connIdAssocName = connIdAttrInfo.getName();
            LOGGER.trace("  association conversion: ConnId: {} -> XSD: {}",
                    connIdAssocName, PrettyPrinter.prettyPrintLazily(xsdAssocName));

            var raw = new RawShadowAssociationDefinition(xsdAssocName);
            processAttributePrismInfo(connIdAttrInfo, raw);
            processAttributeUcfInfo(connIdAttrInfo, raw);

            mpClassDef.addAssociationDefinition(
                    ShadowAssociationDefinition.fromRaw(raw, null));
        }

        /** Sets the "prism" part of the definition. */
        private void processAttributePrismInfo(AttributeInfo attributeInfo, ResourceItemPrismDefinition.Mutable mpDef) {
            mpDef.setMinOccurs(attributeInfo.isRequired() ? 1 : 0);
            mpDef.setMaxOccurs(attributeInfo.isMultiValued() ? -1 : 1);
            mpDef.setCanRead(attributeInfo.isReadable());
            mpDef.setCanAdd(attributeInfo.isCreateable());
            mpDef.setCanModify(attributeInfo.isUpdateable());
        }

        /** Sets to "UCF" part of the definition. */
        private void processAttributeUcfInfo(AttributeInfo attributeInfo, ResourceItemUcfDefinition.Mutable mpDef) {
            mpDef.setNativeAttributeName(attributeInfo.getNativeName());
            mpDef.setFrameworkAttributeName(attributeInfo.getName());
            mpDef.setReturnedByDefault(
                    attributeInfo.isReturnedByDefault());
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
            LOGGER.debug("Unknown subtype {} defined for attribute {}, ignoring (no matching rule definition)",
                    connIdSubtype, attributeInfo.getName());
            return null;
        }
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

    /** Information about XSD type, obtained from ConnId type (technically, Java class). */
    record XsdTypeInformation(@NotNull QName xsdTypeName, boolean multivalued) {

        public int getMaxOccurs() {
            return multivalued ? -1 : 1;
        }
    }
}

/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.DeleteApiOp;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.SchemaApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.api.operations.UpdateDeltaApiOp;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.MutableObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.MutableResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AuxiliaryObjectClassesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.RunAsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.TestConnectionCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Class that can parse ConnId capabilities and schema into midPoint format.
 * It is also used to hold the parsed capabilities and schema.
 * This is a "builder" that builds/converts the schema. As such it
 * hides all the intermediary parsing states. Therefore the ConnectorInstace
 * always has a consistent schema, even during reconfigure and fetch operations.
 * There is either old schema or new schema, but there is no partially-parsed schema.
 *
 * @author Radovan Semancik
 *
 */
public class ConnIdCapabilitiesAndSchemaParser {

    private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY
            = new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();
    private static final Trace LOGGER = TraceManager.getTrace(ConnIdCapabilitiesAndSchemaParser.class);

    // INPUT fields
    private PrismContext prismContext;
    private String resourceSchemaNamespace;
    private ConnIdNameMapper connIdNameMapper;
    private ConnectorFacade connIdConnectorFacade;
    private String connectorHumanReadableName;

    // Internal
    private Set<Class<? extends APIOperation>> connIdSupportedOperations;

    // IN/OUT: this may be configured or detected from pre-configured schema
    private Boolean legacySchema = null;

    // OUTPUT fields
    /**
     * Resource schema is usually parsed from ConnId schema.
     * But if the connector does not have schema capability then resource schema can be null.
     */
    private boolean supportsSchema;
    private ResourceSchema resourceSchema = null;
    private Collection<Object> capabilities = new ArrayList<>();

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public String getResourceSchemaNamespace() {
        return resourceSchemaNamespace;
    }

    public void setResourceSchemaNamespace(String resourceSchemaNamespace) {
        this.resourceSchemaNamespace = resourceSchemaNamespace;
    }

    public ConnIdNameMapper getConnIdNameMapper() {
        return connIdNameMapper;
    }

    public void setConnIdNameMapper(ConnIdNameMapper connIdNameMapper) {
        this.connIdNameMapper = connIdNameMapper;
    }

    public ConnectorFacade getConnIdConnectorFacade() {
        return connIdConnectorFacade;
    }

    public void setConnIdConnectorFacade(ConnectorFacade connIdConnectorFacade) {
        this.connIdConnectorFacade = connIdConnectorFacade;
    }

    public String getConnectorHumanReadableName() {
        return connectorHumanReadableName;
    }

    public void setConnectorHumanReadableName(String connectorHumanReadableName) {
        this.connectorHumanReadableName = connectorHumanReadableName;
    }

    public ResourceSchema getResourceSchema() {
        return resourceSchema;
    }

    public Collection<Object> getCapabilities() {
        return capabilities;
    }

    public Boolean getLegacySchema() {
        return legacySchema;
    }

    public void setLegacySchema(Boolean legacySchema) {
        this.legacySchema = legacySchema;
    }

    public void retrieveResourceCapabilitiesAndSchema(List<QName> generateObjectClasses, OperationResult parentResult) throws CommunicationException, ConfigurationException, GenericFrameworkException, SchemaException {

        fetchSupportedOperations(parentResult);

        processOperationCapabilities(parentResult);

        if (supportsSchema) {

            org.identityconnectors.framework.common.objects.Schema connIdSchema = fetchConnIdSchema(parentResult);

            if (connIdSchema == null) {
                addBasicReadCapability();
            } else {
                parseResourceSchema(connIdSchema, generateObjectClasses);
            }
        }
    }

    private void fetchSupportedOperations(OperationResult parentResult) throws CommunicationException, ConfigurationException, GenericFrameworkException {
        OperationResult connIdSchemaResult = parentResult.createSubresult(ConnectorFacade.class.getName() + ".getSupportedOperations");
        connIdSchemaResult.addContext("connector", connIdConnectorFacade.getClass());

        try {
            LOGGER.debug("Fetching supported connector operations from {}", connectorHumanReadableName);
            InternalMonitor.recordConnectorOperation("getSupportedOperations");

            connIdSupportedOperations = connIdConnectorFacade.getSupportedOperations();

            LOGGER.trace("Connector supported operations: {}", connIdSupportedOperations);
            connIdSchemaResult.recordSuccess();

        } catch (UnsupportedOperationException ex) {
            connIdSchemaResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, ex.getMessage());
            return;
        } catch (Throwable ex) {
            Throwable midpointEx = ConnIdUtil.processConnIdException(ex, connectorHumanReadableName, connIdSchemaResult);

            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof ConfigurationException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (ConfigurationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof RuntimeException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (Error) midpointEx;
            } else {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

    }

    private org.identityconnectors.framework.common.objects.Schema fetchConnIdSchema(OperationResult parentResult) throws CommunicationException, ConfigurationException, GenericFrameworkException {
        // Connector operation cannot create result for itself, so we need to create result for it
        OperationResult connIdSchemaResult = parentResult.createSubresult(ConnectorFacade.class.getName() + ".schema");
        connIdSchemaResult.addContext("connector", connIdConnectorFacade.getClass());
        org.identityconnectors.framework.common.objects.Schema connIdSchema;
        try {

            LOGGER.debug("Fetching schema from {}", connectorHumanReadableName);
            // Fetch the schema from the connector (which actually gets that
            // from the resource).
            InternalMonitor.recordConnectorOperation("schema");
            // TODO have context present
            connIdSchema = connIdConnectorFacade.schema();

            if (connIdSchema == null) {
                connIdSchemaResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Null schema returned");
            } else {
                connIdSchemaResult.recordSuccess();
            }
        } catch (UnsupportedOperationException ex) {
            //recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
            // The connector does no support schema() operation.
            connIdSchemaResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, ex.getMessage());
            return null;
        } catch (Throwable ex) {
            //recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
            // conditions.
            // Therefore this kind of heavy artillery is necessary.
            // ICF interface does not specify exceptions or other error
            // TODO maybe we can try to catch at least some specific exceptions
            Throwable midpointEx = ConnIdUtil.processConnIdException(ex, connectorHumanReadableName, connIdSchemaResult);

            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof ConfigurationException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (ConfigurationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof RuntimeException) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw (Error) midpointEx;
            } else {
                connIdSchemaResult.recordFatalError(midpointEx.getMessage(), midpointEx);
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        return connIdSchema;
    }

    /**
     * Create capabilities from supported connector operations
     */
    private void processOperationCapabilities(OperationResult parentResult) {

        supportsSchema = false;
        if (connIdSupportedOperations.contains(SchemaApiOp.class)) {
            SchemaCapabilityType capSchema = new SchemaCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createSchema(capSchema));
            supportsSchema = true;
        }

        if (connIdSupportedOperations.contains(SyncApiOp.class)) {
            LiveSyncCapabilityType capSync = new LiveSyncCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createLiveSync(capSync));
        }

        if (connIdSupportedOperations.contains(TestApiOp.class)) {
            TestConnectionCapabilityType capTest = new TestConnectionCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createTestConnection(capTest));
        }

        if (connIdSupportedOperations.contains(CreateApiOp.class)){
            CreateCapabilityType capCreate = new CreateCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createCreate(capCreate));
        }

        // GetApiOp is processed later. We need supported options (from schema) to fully process it.

        if (connIdSupportedOperations.contains(UpdateDeltaApiOp.class)) {
            UpdateCapabilityType capUpdate = new UpdateCapabilityType();
            capUpdate.setDelta(true);
            // This is the default for all resources.
            // (Currently there is no way how to obtain it from the connector.)
            // It can be disabled manually.
            capUpdate.setAddRemoveAttributeValues(true);
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createUpdate(capUpdate));
        } else if (connIdSupportedOperations.contains(UpdateApiOp.class)) {
            UpdateCapabilityType capUpdate = new UpdateCapabilityType();
            // This is the default for all resources.
            // (Currently there is no way how to obtain it from the connector.)
            // It can be disabled manually.
            capUpdate.setAddRemoveAttributeValues(true);
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createUpdate(capUpdate));
        }

        if (connIdSupportedOperations.contains(DeleteApiOp.class)){
            DeleteCapabilityType capDelete = new DeleteCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createDelete(capDelete));
        }

        if (connIdSupportedOperations.contains(ScriptOnResourceApiOp.class)
                || connIdSupportedOperations.contains(ScriptOnConnectorApiOp.class)) {
            ScriptCapabilityType capScript = new ScriptCapabilityType();
            if (connIdSupportedOperations.contains(ScriptOnResourceApiOp.class)) {
                Host host = new Host();
                host.setType(ProvisioningScriptHostType.RESOURCE);
                capScript.getHost().add(host);
                // language is unknown here
            }
            if (connIdSupportedOperations.contains(ScriptOnConnectorApiOp.class)) {
                Host host = new Host();
                host.setType(ProvisioningScriptHostType.CONNECTOR);
                capScript.getHost().add(host);
                // language is unknown here
            }
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createScript(capScript));
        }

    }

    private void addBasicReadCapability() {
        // Still need to add "read" capability. This capability would be added during schema processing,
        // because it depends on schema options. But if there is no schema we need to add read capability
        // anyway. We do not want to end up with non-readable resource.
        if (connIdSupportedOperations.contains(GetApiOp.class) || connIdSupportedOperations.contains(SearchApiOp.class)) {
            ReadCapabilityType capRead = new ReadCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createRead(capRead));
        }
    }

    private void parseResourceSchema(org.identityconnectors.framework.common.objects.Schema connIdSchema, List<QName> generateObjectClasses) throws SchemaException {

        AttributeInfo passwordAttributeInfo = null;
        AttributeInfo enableAttributeInfo = null;
        AttributeInfo enableDateAttributeInfo = null;
        AttributeInfo disableDateAttributeInfo = null;
        AttributeInfo lockoutAttributeInfo = null;
        AttributeInfo auxiliaryObjectClasseAttributeInfo = null;

        // New instance of midPoint schema object
        resourceSchema = ObjectFactory.createResourceSchema(resourceSchemaNamespace, prismContext);

        if (legacySchema == null) {
            legacySchema = detectLegacySchema(connIdSchema);
        }
        LOGGER.trace("Converting resource schema (legacy mode: {})", legacySchema);
        LOGGER.trace("Generating object classes: {}", generateObjectClasses);

        Set<ObjectClassInfo> objectClassInfoSet = connIdSchema.getObjectClassInfo();
        // Let's convert every objectclass in the ConnId schema ...
        for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {

            // "Flat" ConnId object class names needs to be mapped to QNames
            QName objectClassXsdName = connIdNameMapper.objectClassToQname(new ObjectClass(objectClassInfo.getType()), resourceSchemaNamespace, legacySchema);

            if (!shouldBeGenerated(generateObjectClasses, objectClassXsdName)){
                LOGGER.trace("Skipping object class {} ({})", objectClassInfo.getType(), objectClassXsdName);
                continue;
            }

            LOGGER.trace("Converting object class {} ({})", objectClassInfo.getType(), objectClassXsdName);

            // ResourceObjectDefinition is a midPpoint way how to represent an
            // object class.
            // The important thing here is the last "type" parameter
            // (objectClassXsdName). The rest is more-or-less cosmetics.
            MutableObjectClassComplexTypeDefinition ocDef = resourceSchema.toMutable().createObjectClassDefinition(objectClassXsdName);

            // The __ACCOUNT__ objectclass in ConnId is a default account
            // objectclass. So mark it appropriately.
            if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
                ocDef.setKind(ShadowKindType.ACCOUNT);
                ocDef.setDefaultInAKind(true);
            }

            ResourceAttributeDefinition<String> uidDefinition = null;
            ResourceAttributeDefinition<String> nameDefinition = null;
            boolean hasUidDefinition = false;

            int displayOrder = ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_START;
            // Let's iterate over all attributes in this object class ...
            Set<AttributeInfo> attributeInfoSet = objectClassInfo.getAttributeInfo();
            for (AttributeInfo attributeInfo : attributeInfoSet) {
                String icfName = attributeInfo.getName();

                if (OperationalAttributes.PASSWORD_NAME.equals(icfName)) {
                    // This attribute will not go into the schema
                    // instead a "password" capability is used
                    passwordAttributeInfo = attributeInfo;
                    // Skip this attribute, capability is sufficient
                    continue;
                }

                if (OperationalAttributes.ENABLE_NAME.equals(icfName)) {
                    enableAttributeInfo = attributeInfo;
                    // Skip this attribute, capability is sufficient
                    continue;
                }

                if (OperationalAttributes.ENABLE_DATE_NAME.equals(icfName)) {
                    enableDateAttributeInfo = attributeInfo;
                    // Skip this attribute, capability is sufficient
                    continue;
                }

                if (OperationalAttributes.DISABLE_DATE_NAME.equals(icfName)) {
                    disableDateAttributeInfo = attributeInfo;
                    // Skip this attribute, capability is sufficient
                    continue;
                }

                if (OperationalAttributes.LOCK_OUT_NAME.equals(icfName)) {
                    lockoutAttributeInfo = attributeInfo;
                    // Skip this attribute, capability is sufficient
                    continue;
                }

                if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equals(icfName)) {
                    auxiliaryObjectClasseAttributeInfo = attributeInfo;
                    // Skip this attribute, capability is sufficient
                    continue;
                }

                String processedAttributeName = icfName;
                if ((Name.NAME.equals(icfName) || Uid.NAME.equals(icfName)) && attributeInfo.getNativeName() != null ) {
                    processedAttributeName = attributeInfo.getNativeName();
                }

                QName attrXsdName = connIdNameMapper.convertAttributeNameToQName(processedAttributeName, ocDef);
                QName attrXsdType = connIdTypeToXsdType(attributeInfo, false);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("  attr conversion ConnId: {}({}) -> XSD: {}({})",
                            icfName, attributeInfo.getType().getSimpleName(),
                            PrettyPrinter.prettyPrint(attrXsdName), PrettyPrinter.prettyPrint(attrXsdType));
                }

                // Create ResourceObjectAttributeDefinition, which is midPoint
                // way how to express attribute schema.
                MutableResourceAttributeDefinition attrDef = ObjectFactory.createResourceAttributeDefinition(
                        attrXsdName, attrXsdType, prismContext);

                attrDef.setMatchingRuleQName(connIdAttributeInfoToMatchingRule(attributeInfo));

                if (Name.NAME.equals(icfName)) {
                    nameDefinition = attrDef;
                    if (uidDefinition != null && attrXsdName.equals(uidDefinition.getItemName())) {
                        attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                        uidDefinition = attrDef;
                        hasUidDefinition = true;
                    } else {
                        if (attributeInfo.getNativeName() == null) {
                            // Set a better display name for __NAME__. The "name" is s very
                            // overloaded term, so let's try to make things
                            // a bit clearer
                            attrDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_NAME);
                        }
                        attrDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_ORDER);
                    }

                } else if (Uid.NAME.equals(icfName)) {
                    // UID can be the same as other attribute
                    ResourceAttributeDefinition existingDefinition = ocDef.findAttributeDefinition(attrXsdName);
                    if (existingDefinition != null) {
                        hasUidDefinition = true;
                        existingDefinition.toMutable().setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                        uidDefinition = existingDefinition;
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

                // Now we are going to process flags such as optional and multi-valued
                Set<Flags> flagsSet = attributeInfo.getFlags();

                attrDef.setMinOccurs(0);
                attrDef.setMaxOccurs(1);
                boolean canCreate = true;
                boolean canUpdate = true;
                boolean canRead = true;

                for (Flags flags : flagsSet) {
                    if (flags == Flags.REQUIRED) {
                        attrDef.setMinOccurs(1);
                    }
                    if (flags == Flags.MULTIVALUED) {
                        attrDef.setMaxOccurs(-1);
                    }
                    if (flags == Flags.NOT_CREATABLE) {
                        canCreate = false;
                    }
                    if (flags == Flags.NOT_READABLE) {
                        canRead = false;
                    }
                    if (flags == Flags.NOT_UPDATEABLE) {
                        canUpdate = false;
                    }
                    if (flags == Flags.NOT_RETURNED_BY_DEFAULT) {
                        attrDef.setReturnedByDefault(false);
                    }
                }

                attrDef.setCanAdd(canCreate);
                attrDef.setCanModify(canUpdate);
                attrDef.setCanRead(canRead);

                if (!Uid.NAME.equals(icfName)) {
                    ocDef.add(attrDef);
                }
            }

            if (uidDefinition == null) {
                // Every object has UID in ConnId, therefore add a default definition if no other was specified
                uidDefinition = ObjectFactory.createResourceAttributeDefinition(SchemaConstants.ICFS_UID, DOMUtil.XSD_STRING, prismContext);
                // DO NOT make it mandatory. It must not be present on create hence it cannot be mandatory.
                uidDefinition.toMutable().setMinOccurs(0);
                uidDefinition.toMutable().setMaxOccurs(1);
                // Make it read-only
                uidDefinition.toMutable().setReadOnly();
                // Set a default display name
                uidDefinition.toMutable().setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
                uidDefinition.toMutable().setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                // Uid is a primary identifier of every object (this is the ConnId way)
            }
            if (!hasUidDefinition) {
                ocDef.toMutable().add(uidDefinition);
            }
            ocDef.toMutable().addPrimaryIdentifier(uidDefinition);
            if (uidDefinition != nameDefinition) {
                ocDef.toMutable().addSecondaryIdentifier(nameDefinition);
            }

            // Add schema annotations
            ocDef.toMutable().setNativeObjectClass(objectClassInfo.getType());
            ocDef.toMutable().setDisplayNameAttribute(nameDefinition.getItemName());
            ocDef.toMutable().setNamingAttribute(nameDefinition.getItemName());
            ocDef.toMutable().setAuxiliary(objectClassInfo.isAuxiliary());

            LOGGER.trace("  ... converted object class {}: {}", objectClassInfo.getType(), ocDef);
        }



        ActivationCapabilityType capAct = null;

        if (enableAttributeInfo != null) {
            if (capAct == null) {
                capAct = new ActivationCapabilityType();
            }
            ActivationStatusCapabilityType capActStatus = new ActivationStatusCapabilityType();
            capAct.setStatus(capActStatus);
            if (!enableAttributeInfo.isReturnedByDefault()) {
                capActStatus.setReturnedByDefault(false);
            }
        }

        if (enableDateAttributeInfo != null) {
            if (capAct == null) {
                capAct = new ActivationCapabilityType();
            }
            ActivationValidityCapabilityType capValidFrom = new ActivationValidityCapabilityType();
            capAct.setValidFrom(capValidFrom);
            if (!enableDateAttributeInfo.isReturnedByDefault()) {
                capValidFrom.setReturnedByDefault(false);
            }
        }

        if (disableDateAttributeInfo != null) {
            if (capAct == null) {
                capAct = new ActivationCapabilityType();
            }
            ActivationValidityCapabilityType capValidTo = new ActivationValidityCapabilityType();
            capAct.setValidTo(capValidTo);
            if (!disableDateAttributeInfo.isReturnedByDefault()) {
                capValidTo.setReturnedByDefault(false);
            }
        }

        if (lockoutAttributeInfo != null) {
            if (capAct == null) {
                capAct = new ActivationCapabilityType();
            }
            ActivationLockoutStatusCapabilityType capActStatus = new ActivationLockoutStatusCapabilityType();
            capAct.setLockoutStatus(capActStatus);
            if (!lockoutAttributeInfo.isReturnedByDefault()) {
                capActStatus.setReturnedByDefault(false);
            }
        }

        // TODO: activation and credentials should be per-objectclass capabilities
        if (capAct != null) {
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createActivation(capAct));
        }

        if (passwordAttributeInfo != null) {
            CredentialsCapabilityType capCred = new CredentialsCapabilityType();
            PasswordCapabilityType capPass = new PasswordCapabilityType();
            if (!passwordAttributeInfo.isReturnedByDefault()) {
                capPass.setReturnedByDefault(false);
            }
            if (passwordAttributeInfo.isReadable()) {
                capPass.setReadable(true);
            }
            capCred.setPassword(capPass);
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createCredentials(capCred));
        }

        if (auxiliaryObjectClasseAttributeInfo != null) {
            AuxiliaryObjectClassesCapabilityType capAux = new AuxiliaryObjectClassesCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createAuxiliaryObjectClasses(capAux));
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
            PagedSearchCapabilityType capPage = new PagedSearchCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createPagedSearch(capPage));
        }

        if (connIdSupportedOperations.contains(GetApiOp.class) || connIdSupportedOperations.contains(SearchApiOp.class)) {
            ReadCapabilityType capRead = new ReadCapabilityType();
            capRead.setReturnDefaultAttributesOption(supportsReturnDefaultAttributes);
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createRead(capRead));
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
        if (generateObjectClasses == null || generateObjectClasses.isEmpty()){
            return true;
        }

        for (QName objClassToGenerate : generateObjectClasses){
            if (objClassToGenerate.equals(objectClassXsdName)){
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
            switch (searchOption.getName()) {
                case OperationOptions.OP_RUN_AS_USER:
                    canRunAsUser = true;
                    break;
                // TODO: run as password
            }
        }
        if (canRunAsUser) {
            RunAsCapabilityType capRunAs = new RunAsCapabilityType();
            capabilities.add(CAPABILITY_OBJECT_FACTORY.createRunAs(capRunAs));
        }
    }

    public static QName connIdTypeToXsdType(AttributeInfo attrInfo, boolean isConfidential) throws SchemaException {
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
        return connIdTypeToXsdType(attrInfo.getType(), isConfidential);
    }

    public static QName connIdTypeToXsdType(Class<?> type, boolean isConfidential) {
        // For arrays we are only interested in the component type
        if (isMultivaluedType(type)) {
            type = type.getComponentType();
        }
        QName propXsdType = null;
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

    public static boolean isMultivaluedType(Class<?> type) {
        // We consider arrays to be multi-valued
        // ... unless it is byte[] or char[]
        return type.isArray() && !type.equals(byte[].class) && !type.equals(char[].class);
    }

}

/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.*;

/**
 * @author semancik
 */
public abstract class AbstractModificationConverter implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractModificationConverter.class);

    private Collection<Operation> changes;
    private ConnectorType connectorType;
    private ResourceSchema resourceSchema;
    private ResourceObjectDefinition objectDefinition;
    private String connectorDescription;
    private ConnectorOperationOptions options;

    private Protector protector;

    public Collection<Operation> getChanges() {
        return changes;
    }

    public void setChanges(Collection<Operation> changes) {
        this.changes = changes;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(ConnectorType connectorType) {
        this.connectorType = connectorType;
    }

    public ResourceSchema getResourceSchema() {
        return resourceSchema;
    }

    public void setResourceSchema(ResourceSchema resourceSchema) {
        this.resourceSchema = resourceSchema;
    }

    public ResourceObjectDefinition getObjectDefinition() {
        return objectDefinition;
    }

    public void setObjectDefinition(ResourceObjectDefinition objectDefinition) {
        this.objectDefinition = objectDefinition;
    }

    public String getConnectorDescription() {
        return connectorDescription;
    }

    public void setConnectorDescription(String connectorDescription) {
        this.connectorDescription = connectorDescription;
    }

    public Protector getProtector() {
        return protector;
    }

    public void setProtector(Protector protector) {
        this.protector = protector;
    }

    public ConnectorOperationOptions getOptions() {
        return options;
    }

    public void setOptions(ConnectorOperationOptions options) {
        this.options = options;
    }

    /**
     * Convenience using the default value converter.
     */
    protected <T> void collect(String connIdAttrName, PropertyDelta<T> delta, PlusMinusZero isInModifiedAuxilaryClass) throws SchemaException {
        collect(connIdAttrName, delta, isInModifiedAuxilaryClass, this::covertAttributeValuesToConnId);
    }

    protected abstract <T> void collect(String connIdAttrName, PropertyDelta<T> delta, PlusMinusZero isInModifiedAuxilaryClass, CollectorValuesConverter<T> valuesConverter) throws SchemaException;

    /**
     * Simplified collect for single-valued attribute (e.g. activation).
     */
    protected abstract <T> void collectReplace(String connIdAttrName, T connIdAttrValue) throws SchemaException;

    private void collectReplaceXMLGregorianCalendar(String connIdAttrName, XMLGregorianCalendar xmlCal) throws SchemaException {
        collectReplace(connIdAttrName, xmlCal != null ? XmlTypeConverter.toMillis(xmlCal) : null);
    }

    public void convert() throws SchemaException {

        PropertyDelta<QName> auxiliaryObjectClassDelta = determineAuxilaryObjectClassDelta(changes);

        ResourceObjectDefinition structuralObjectClassDefinition = resourceSchema.findDefinitionForObjectClass(objectDefinition.getTypeName());
        if (structuralObjectClassDefinition == null) {
            throw new SchemaException("No definition of structural object class " + objectDefinition.getTypeName() + " in " + connectorDescription);
        }
        Map<QName, ResourceObjectDefinition> auxiliaryObjectClassMap = new HashMap<>();
        if (auxiliaryObjectClassDelta != null) {
            // Auxiliary object class change means modification of __AUXILIARY_OBJECT_CLASS__ attribute
            collect(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME, auxiliaryObjectClassDelta, null,
                    (pvals, midPointAttributeName) ->
                            covertAuxiliaryObjectClassValuesToConnId(pvals, auxiliaryObjectClassMap));
        }

        for (Operation operation : changes) {
            if (operation instanceof PropertyModificationOperation) {
                PropertyModificationOperation change = (PropertyModificationOperation) operation;
                PropertyDelta<?> delta = change.getPropertyDelta();

                if (delta.getParentPath().equivalent(ShadowType.F_ATTRIBUTES)) {
                    if (delta.getDefinition() == null || !(delta.getDefinition() instanceof ShadowSimpleAttributeDefinition)) {
                        ShadowSimpleAttributeDefinition def = objectDefinition
                                .findSimpleAttributeDefinition(delta.getElementName());
                        if (def == null) {
                            String message = "No definition for attribute " + delta.getElementName() + " used in modification delta";
                            throw new SchemaException(message);
                        }
                        try {
                            delta.applyDefinition(def);
                        } catch (SchemaException e) {
                            throw e;
                        }
                    }
                    PlusMinusZero isInModifiedAuxilaryClass = null;
                    ShadowSimpleAttributeDefinition<?> structAttrDef = structuralObjectClassDefinition.findSimpleAttributeDefinition(delta.getElementName());
                    // if this attribute is also in the structural object class. It does not matter if it is in
                    // aux object class, we cannot add/remove it with the object class unless it is normally requested
                    if (structAttrDef == null) {
                        if (auxiliaryObjectClassDelta != null && auxiliaryObjectClassDelta.isDelete()) {
                            // We need to change all the deltas of all the attributes that belong
                            // to the removed auxiliary object class from REPLACE to DELETE. The change of
                            // auxiliary object class and the change of the attributes must be done in
                            // one operation. Otherwise we get schema error. And as auxiliary object class
                            // is removed, the attributes must be removed as well.
                            for (PrismPropertyValue<QName> auxPval : auxiliaryObjectClassDelta.getValuesToDelete()) {
                                ResourceObjectDefinition auxDef = auxiliaryObjectClassMap.get(auxPval.getValue());
                                ShadowSimpleAttributeDefinition<?> attrDef = auxDef.findSimpleAttributeDefinition(delta.getElementName());
                                if (attrDef != null) {
                                    // means: is in removed auxiliary class
                                    isInModifiedAuxilaryClass = PlusMinusZero.MINUS;
                                    break;
                                }
                            }
                        }
                        if (auxiliaryObjectClassDelta != null && auxiliaryObjectClassDelta.isAdd()) {
                            // We need to change all the deltas of all the attributes that belong
                            // to the new auxiliary object class from REPLACE to ADD. The change of
                            // auxiliary object class and the change of the attributes must be done in
                            // one operation. Otherwise we get schema error. And as auxiliary object class
                            // is added, the attributes must be added as well.
                            for (PrismPropertyValue<QName> auxPval : auxiliaryObjectClassDelta.getValuesToAdd()) {
                                ResourceObjectDefinition auxOcDef = auxiliaryObjectClassMap.get(auxPval.getValue());
                                ShadowSimpleAttributeDefinition<?> auxAttrDef = auxOcDef.findSimpleAttributeDefinition(delta.getElementName());
                                if (auxAttrDef != null) {
                                    // means: is in added auxiliary class
                                    isInModifiedAuxilaryClass = PlusMinusZero.PLUS;
                                    break;
                                }
                            }
                        }
                    }

                    // Change in (ordinary) attributes. Transform to the ConnId attributes.
                    String connIdAttrName = ucfAttributeNameToConnId(delta, objectDefinition);
                    collect(connIdAttrName, delta, isInModifiedAuxilaryClass);

                } else if (delta.getParentPath().equivalent(ShadowType.F_ACTIVATION)) {
                    convertFromActivation(delta);
                } else if (delta.getParentPath().equivalent(SchemaConstants.PATH_PASSWORD)) {
                    convertFromPassword((PropertyDelta<ProtectedStringType>) delta);
                } else if (delta.getPath().equivalent(ShadowType.F_AUXILIARY_OBJECT_CLASS)) {
                    // already processed
                } else {
                    throw new SchemaException("Change of unknown attribute " + delta.getPath());
                }

            } else {
                throw new IllegalArgumentException("Unknown operation type " + operation.getClass().getName()
                        + ": " + operation);
            }

        }

    }

    private void convertFromActivation(PropertyDelta<?> propDelta) throws SchemaException {
        if (propDelta.getElementName().equals(ActivationType.F_ADMINISTRATIVE_STATUS)) {
            ActivationStatusType status = getPropertyNewValue(propDelta, ActivationStatusType.class);
            if (status == null) {
                collectReplace(OperationalAttributes.ENABLE_NAME, null);
            } else {
                collectReplace(OperationalAttributes.ENABLE_NAME, ActivationStatusType.ENABLED.equals(status));
            }

        } else if (propDelta.getElementName().equals(ActivationType.F_VALID_FROM)) {
            XMLGregorianCalendar xmlCal = getPropertyNewValue(propDelta, XMLGregorianCalendar.class);
            collectReplaceXMLGregorianCalendar(OperationalAttributes.ENABLE_DATE_NAME, xmlCal);

        } else if (propDelta.getElementName().equals(ActivationType.F_VALID_TO)) {
            XMLGregorianCalendar xmlCal = getPropertyNewValue(propDelta, XMLGregorianCalendar.class);
            collectReplaceXMLGregorianCalendar(OperationalAttributes.DISABLE_DATE_NAME, xmlCal);

        } else if (propDelta.getElementName().equals(ActivationType.F_LOCKOUT_STATUS)) {
            LockoutStatusType status = getPropertyNewValue(propDelta, LockoutStatusType.class);//propDelta.getPropertyNew().getValue(LockoutStatusType.class).getValue();
            collectReplace(OperationalAttributes.LOCK_OUT_NAME, !LockoutStatusType.NORMAL.equals(status));
        } else {
            throw new SchemaException("Got unknown activation attribute delta " + propDelta.getElementName());
        }

    }

    private void convertFromPassword(PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
        if (passwordDelta == null) {
            throw new IllegalArgumentException("No password was provided");
        }

        QName elementName = passwordDelta.getElementName();
        if (StringUtils.isBlank(elementName.getNamespaceURI())) {
            if (!QNameUtil.match(elementName, PasswordType.F_VALUE)) {
                return;
            }
        } else if (!passwordDelta.getElementName().equals(PasswordType.F_VALUE)) {
            return;
        }
        collectPassword(passwordDelta);
    }

    protected void collectPassword(PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
        PrismProperty<ProtectedStringType> newPassword = passwordDelta.getPropertyNewMatchingPath();
        if (newPassword == null || newPassword.isEmpty()) {
            // This is the case of setting no password. E.g. removing existing password
            LOGGER.debug("Setting null password.");
            collectReplace(OperationalAttributes.PASSWORD_NAME, null);
        } else if (newPassword.getRealValue().canGetCleartext()) {
            // We have password and we can get a cleartext value of the password. This is normal case
            GuardedString guardedPassword = passwordToGuardedString(newPassword.getRealValue(), "new password");
            collectReplace(OperationalAttributes.PASSWORD_NAME, guardedPassword);
        } else {
            // We have password, but we cannot get a cleartext value. Just to nothing.
            LOGGER.debug("We would like to set password, but we do not have cleartext value. Skipping the operation.");
        }
    }

    protected GuardedString passwordToGuardedString(ProtectedStringType ps, String propertyName) {
        return ConnIdUtil.toGuardedString(ps, propertyName, protector);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> T getPropertyNewValue(PropertyDelta propertyDelta, Class<T> clazz) throws SchemaException {
        PrismProperty<PrismPropertyValue<T>> prop = propertyDelta.getPropertyNewMatchingPath();
        if (prop == null) {
            return null;
        }
        PrismPropertyValue<T> propValue = prop.getValue(clazz);

        if (propValue == null) {
            return null;
        }

        return propValue.getValue();
    }

    protected <T> List<Object> covertAttributeValuesToConnId(Collection<PrismPropertyValue<T>> pvals, QName midPointAttributeName) throws SchemaException {
        List<Object> connIdVals = new ArrayList<>(pvals.size());
        for (PrismPropertyValue<T> pval : pvals) {
            connIdVals.add(covertAttributeValueToConnId(pval, midPointAttributeName));
        }
        return connIdVals;
    }

    protected <T> Object covertAttributeValueToConnId(PrismPropertyValue<T> pval, QName midPointAttributeName) throws SchemaException {
        return ConnIdUtil.convertValueToConnId(pval, protector, midPointAttributeName);
    }

    private <T> List<Object> covertAuxiliaryObjectClassValuesToConnId(
            Collection<PrismPropertyValue<QName>> pvals,
            Map<QName, ResourceObjectDefinition> auxiliaryObjectClassMap) throws SchemaException {
        List<Object> connIdVals = new ArrayList<>(pvals.size());
        for (PrismPropertyValue<QName> pval : pvals) {
            QName auxQName = pval.getValue();
            auxiliaryObjectClassMap.put(auxQName, resourceSchema.findDefinitionForObjectClassRequired(auxQName));
            ObjectClass icfOc = ucfObjectClassNameToConnId(pval.getValue(), false);
            connIdVals.add(icfOc.getObjectClassValue());
        }
        return connIdVals;
    }

    private PropertyDelta<QName> determineAuxilaryObjectClassDelta(Collection<Operation> changes) {
        PropertyDelta<QName> auxiliaryObjectClassDelta = null;

        for (Operation operation : changes) {
            if (operation == null) {
                IllegalArgumentException e = new IllegalArgumentException("Null operation in modifyObject");
                throw e;
            }
            if (operation instanceof PropertyModificationOperation) {
                PropertyDelta<?> delta = ((PropertyModificationOperation) operation).getPropertyDelta();
                if (delta.getPath().equivalent(ShadowType.F_AUXILIARY_OBJECT_CLASS)) {
                    auxiliaryObjectClassDelta = (PropertyDelta<QName>) delta;
                }
            }
        }

        return auxiliaryObjectClassDelta;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpOutput(sb, indent);
        return sb.toString();
    }

    protected abstract void debugDumpOutput(StringBuilder sb, int indent);

}

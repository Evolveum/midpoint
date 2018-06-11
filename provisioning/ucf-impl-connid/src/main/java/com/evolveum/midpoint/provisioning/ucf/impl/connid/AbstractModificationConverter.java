/**
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PasswordChangeOperation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public abstract class AbstractModificationConverter implements DebugDumpable {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractModificationConverter.class);
	
	private Collection<Operation> changes;
	private ConnectorType connectorType;
	private ResourceSchema resourceSchema;
	private String resourceSchemaNamespace;
	private ObjectClassComplexTypeDefinition objectClassDef;
	private String connectorDescription;
	
	private ConnIdNameMapper connIdNameMapper;
	private Protector protector;
	
	private List<Operation> additionalOperations = new ArrayList<>();
	
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

	public String getResourceSchemaNamespace() {
		return resourceSchemaNamespace;
	}

	public void setResourceSchemaNamespace(String resourceSchemaNamespace) {
		this.resourceSchemaNamespace = resourceSchemaNamespace;
	}

	public ObjectClassComplexTypeDefinition getObjectClassDef() {
		return objectClassDef;
	}

	public void setObjectClassDef(ObjectClassComplexTypeDefinition objectClassDef) {
		this.objectClassDef = objectClassDef;
	}

	public String getConnectorDescription() {
		return connectorDescription;
	}

	public void setConnectorDescription(String connectorDescription) {
		this.connectorDescription = connectorDescription;
	}

	public ConnIdNameMapper getConnIdNameMapper() {
		return connIdNameMapper;
	}

	public void setConnIdNameMapper(ConnIdNameMapper connIdNameMapper) {
		this.connIdNameMapper = connIdNameMapper;
	}

	public Protector getProtector() {
		return protector;
	}

	public void setProtector(Protector protector) {
		this.protector = protector;
	}
	
	public List<Operation> getAdditionalOperations() {
		return additionalOperations;
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
		
		ObjectClassComplexTypeDefinition structuralObjectClassDefinition = resourceSchema.findObjectClassDefinition(objectClassDef.getTypeName());
		if (structuralObjectClassDefinition == null) {
			throw new SchemaException("No definition of structural object class "+objectClassDef.getTypeName()+" in "+connectorDescription);
		}
		Map<QName,ObjectClassComplexTypeDefinition> auxiliaryObjectClassMap = new HashMap<>();
		if (auxiliaryObjectClassDelta != null) {
			// Auxiliary object class change means modification of __AUXILIARY_OBJECT_CLASS__ attribute
			collect(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME, auxiliaryObjectClassDelta, null,
					(pvals,midPointAttributeName) -> covertAuxiliaryObjectClassValuesToConnId(pvals, midPointAttributeName, auxiliaryObjectClassMap));
		}
		
		PropertyDelta<ProtectedStringType> passwordDelta = null;
		PasswordChangeOperation passwordChangeOperation = null;
		
		for (Operation operation : changes) {
			if (operation instanceof PropertyModificationOperation) {
				PropertyModificationOperation change = (PropertyModificationOperation) operation;
				PropertyDelta<?> delta = change.getPropertyDelta();

				if (delta.getParentPath().equivalent(new ItemPath(ShadowType.F_ATTRIBUTES))) {
					if (delta.getDefinition() == null || !(delta.getDefinition() instanceof ResourceAttributeDefinition)) {
						ResourceAttributeDefinition def = objectClassDef
								.findAttributeDefinition(delta.getElementName());
						if (def == null) {
							String message = "No definition for attribute "+delta.getElementName()+" used in modification delta";
							throw new SchemaException(message);
						}
						try {
							delta.applyDefinition(def);
						} catch (SchemaException e) {
							throw e;
						}
					}
					PlusMinusZero isInModifiedAuxilaryClass = null;
					ResourceAttributeDefinition<Object> structAttrDef = structuralObjectClassDefinition.findAttributeDefinition(delta.getElementName());
					// if this attribute is also in the structural object class. It does not matter if it is in
					// aux object class, we cannot add/remove it with the object class unless it is normally requested
					if (structAttrDef == null) {
						if (auxiliaryObjectClassDelta != null && auxiliaryObjectClassDelta.isDelete()) {
							// We need to change all the deltas of all the attributes that belong
							// to the removed auxiliary object class from REPLACE to DELETE. The change of
							// auxiliary object class and the change of the attributes must be done in
							// one operation. Otherwise we get schema error. And as auxiliary object class
							// is removed, the attributes must be removed as well.
							for (PrismPropertyValue<QName> auxPval: auxiliaryObjectClassDelta.getValuesToDelete()) {
								ObjectClassComplexTypeDefinition auxDef = auxiliaryObjectClassMap.get(auxPval.getValue());
								ResourceAttributeDefinition<Object> attrDef = auxDef.findAttributeDefinition(delta.getElementName());
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
							for (PrismPropertyValue<QName> auxPval: auxiliaryObjectClassDelta.getValuesToAdd()) {
								ObjectClassComplexTypeDefinition auxOcDef = auxiliaryObjectClassMap.get(auxPval.getValue());
								ResourceAttributeDefinition<Object> auxAttrDef = auxOcDef.findAttributeDefinition(delta.getElementName());
								if (auxAttrDef != null) {
									// means: is in added auxiliary class
									isInModifiedAuxilaryClass = PlusMinusZero.PLUS;
									break;
								}
							}
						}
					}
										
					// Change in (ordinary) attributes. Transform to the ConnId attributes.
					String connIdAttrName = connIdNameMapper.convertAttributeNameToConnId(delta, objectClassDef);
					collect(connIdAttrName, delta, isInModifiedAuxilaryClass);
					
				} else if (delta.getParentPath().equivalent(new ItemPath(ShadowType.F_ACTIVATION))) {
					convertFromActivation(delta);
				} else if (delta.getParentPath().equivalent(
						new ItemPath(new ItemPath(ShadowType.F_CREDENTIALS), CredentialsType.F_PASSWORD))) {
					convertFromPassword((PropertyDelta<ProtectedStringType>) delta);
				} else if (delta.getPath().equivalent(new ItemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS))) {
					// already processed
				} else {
					throw new SchemaException("Change of unknown attribute " + delta.getPath());
				}

			} else if (operation instanceof PasswordChangeOperation) {
				passwordChangeOperation = (PasswordChangeOperation) operation;
				// TODO: check for multiple occurrences and fail

			} else if (operation instanceof ExecuteProvisioningScriptOperation) {
				ExecuteProvisioningScriptOperation scriptOperation = (ExecuteProvisioningScriptOperation) operation;
				additionalOperations.add(scriptOperation);

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
		PrismProperty<ProtectedStringType> newPassword = passwordDelta.getPropertyNewMatchingPath();
		if (newPassword == null || newPassword.isEmpty()) {
			// This is the case of setting no password. E.g. removing existing password
			LOGGER.debug("Setting null password.");
			collectReplace(OperationalAttributes.PASSWORD_NAME, null);
		} else if (newPassword.getRealValue().canGetCleartext()) {
			// We have password and we can get a cleartext value of the passowrd. This is normal case
			GuardedString guardedPassword = ConnIdUtil.toGuardedString(newPassword.getRealValue(), "new password", protector);
			collectReplace(OperationalAttributes.PASSWORD_NAME, guardedPassword);
		} else {
			// We have password, but we cannot get a cleartext value. Just to nothing.
			LOGGER.debug("We would like to set password, but we do not have cleartext value. Skipping the opearation.");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T getPropertyNewValue(PropertyDelta propertyDelta, Class<T> clazz) throws SchemaException {
		PrismProperty<PrismPropertyValue<T>> prop = propertyDelta.getPropertyNewMatchingPath();
		if (prop == null){
			return null;
		}
		PrismPropertyValue<T> propValue = prop.getValue(clazz);

		if (propValue == null){
			return null;
		}

		return propValue.getValue();
	}

	protected <T> List<Object> covertAttributeValuesToConnId(Collection<PrismPropertyValue<T>> pvals, QName midPointAttributeName) throws SchemaException {
		List<Object> connIdVals = new ArrayList<>(pvals.size());
		for (PrismPropertyValue<T> pval: pvals) {
			connIdVals.add(covertAttributeValueToConnId(pval, midPointAttributeName));
		}
		return connIdVals;
	}
	
	protected <T> Object covertAttributeValueToConnId(PrismPropertyValue<T> pval, QName midPointAttributeName) throws SchemaException {
		return ConnIdUtil.convertValueToIcf(pval, protector, midPointAttributeName);
	}
	
	private <T> List<Object> covertAuxiliaryObjectClassValuesToConnId(Collection<PrismPropertyValue<QName>> pvals, QName midPointAttributeName, Map<QName,ObjectClassComplexTypeDefinition> auxiliaryObjectClassMap) throws SchemaException {
		List<Object> connIdVals = new ArrayList<>(pvals.size());
		for (PrismPropertyValue<QName> pval: pvals) {
			QName auxQName = pval.getValue();
			ObjectClassComplexTypeDefinition auxDef = resourceSchema.findObjectClassDefinition(auxQName);
			if (auxDef == null) {
				throw new SchemaException("Auxiliary object class "+auxQName+" not found in the schema");
			}
			auxiliaryObjectClassMap.put(auxQName, auxDef);
			ObjectClass icfOc = connIdNameMapper.objectClassToIcf(pval.getValue(), resourceSchemaNamespace, connectorType, false);
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
				PropertyDelta<?> delta = ((PropertyModificationOperation)operation).getPropertyDelta();
				if (delta.getPath().equivalent(new ItemPath(ShadowType.F_AUXILIARY_OBJECT_CLASS))) {
					auxiliaryObjectClassDelta = (PropertyDelta<QName>) delta;
				}
			}
		}
		
		return auxiliaryObjectClassDelta;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilder(this.getClass(), indent);
		debugDumpOutput(sb, indent);
		DebugUtil.debugDumpWithLabel(sb, "additionalOperations", additionalOperations, indent + 1);
		return sb.toString();
	}

	protected abstract void debugDumpOutput(StringBuilder sb, int indent);
	
}

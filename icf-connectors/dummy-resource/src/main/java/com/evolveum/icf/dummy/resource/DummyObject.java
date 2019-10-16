/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author Radovan Semancik
 *
 */
public abstract class DummyObject implements DebugDumpable {

	private String id;
//	private int internalId = -1;
	private String name;
	private Map<String,Set<Object>> attributes = new HashMap<>();
	private Boolean enabled = true;
	private Date validFrom = null;
	private Date validTo = null;
	private String lastModifier;
	protected DummyResource resource;

	private final Set<String> auxiliaryObjectClassNames = new HashSet<>();

	private BreakMode modifyBreakMode = null;

	public DummyObject() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DummyObject(String name) {
		this.name = name;
	}

	public DummyResource getResource() {
		return resource;
	}

	public void setResource(DummyResource resource) {
		this.resource = resource;
	}

	public String getName() {
		return name;
	}

	public void setName(String username) {
		this.name = username;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		this.enabled = enabled;
		recordModify("_ENABLED", null, null, Arrays.asList(enabled));
	}

	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(Date validFrom) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		this.validFrom = validFrom;
		recordModify("_VALID_FROM", null, null, Arrays.asList(validFrom));
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(Date validTo) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		this.validTo = validTo;
		recordModify("_VALID_TO", null, null, Arrays.asList(validTo));
	}

	public String getLastModifier() {
		return lastModifier;
	}

	public void setLastModifier(String lastModifier) {
		this.lastModifier = lastModifier;
	}

	public BreakMode getModifyBreakMode() {
		return modifyBreakMode;
	}

	public void setModifyBreakMode(BreakMode modifyBreakMode) {
		this.modifyBreakMode = modifyBreakMode;
	}

	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}

	public <T> Set<T> getAttributeValues(String attrName, Class<T> type) {
		Set<Object> values = attributes.get(attrName);
		return (Set)values;
	}

	public <T> T getAttributeValue(String attrName, Class<T> type) {
		Set<T> values = getAttributeValues(attrName, type);
		if (values == null || values.isEmpty()) {
			return null;
		}
		if (values.size()>1) {
			throw new IllegalArgumentException("Attempt to fetch single value from a multi-valued attribute "+attrName);
		}
		return values.iterator().next();
	}

	public String getAttributeValue(String attrName) {
		return getAttributeValue(attrName,String.class);
	}

	public void replaceAttributeValue(String name, Object value) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		Collection<Object> values = new ArrayList<>(1);
		values.add(value);
		replaceAttributeValues(name, values);
	}

	public void replaceAttributeValues(String name, Collection<Object> values) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<>();
			attributes.put(name, currentValues);
		} else {
			currentValues.clear();
		}
		currentValues.addAll(values);
		checkSchema(name, values, "replace");
		recordModify(name, null, null, values);
	}

	public void replaceAttributeValues(String name, Object... values) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<>();
			attributes.put(name, currentValues);
		} else {
			currentValues.clear();
		}
		List<Object> valuesList = Arrays.asList(values);
		currentValues.addAll(valuesList);
		checkSchema(name, valuesList, "replace");
		if (valuesList.isEmpty()) {
			attributes.remove(name);
		}
		recordModify(name, null, null, Arrays.asList(values));
	}

	public void addAttributeValue(String name, Object value) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		Collection<Object> values = new ArrayList<>(1);
		values.add(value);
		addAttributeValues(name, values);
	}

	public <T> void addAttributeValues(String name, Collection<T> valuesToAdd) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<>();
			attributes.put(name, currentValues);
		}
		for(T valueToAdd: valuesToAdd) {
			addAttributeValue(name, currentValues, valueToAdd);
		}
		recordModify(name, valuesToAdd, null, null);
	}

	public void addAttributeValues(String name, String... valuesToAdd) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<>();
			attributes.put(name, currentValues);
		}
		for (Object valueToAdd: valuesToAdd) {
			addAttributeValue(name, currentValues, valueToAdd);
		}
		recordModify(name, Arrays.asList(valuesToAdd), null, null);
	}

	private void addAttributeValue(String attrName, Set<Object> currentValues, Object valueToAdd) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		if (resource != null && !resource.isTolerateDuplicateValues()) {
			for (Object currentValue: currentValues) {
				if (currentValue.equals(valueToAdd)) {
					throw new IllegalArgumentException("The value '"+valueToAdd+"' of attribute '"+attrName+"' conflicts with existing value: Attempt to add value that already exists");
				}
				if (resource.isCaseIgnoreValues() && (valueToAdd instanceof String)) {
					if (StringUtils.equalsIgnoreCase((String)currentValue, (String)valueToAdd)) {
						throw new IllegalArgumentException("The value '"+valueToAdd+"' of attribute '"+attrName+"' conflicts with existing value: Attempt to add value that already exists");
					}
				}
			}
		}

		if (resource != null && resource.isMonsterization() && DummyResource.VALUE_MONSTER.equals(valueToAdd)) {
			Iterator<Object> iterator = currentValues.iterator();
			while (iterator.hasNext()) {
				if (DummyResource.VALUE_COOKIE.equals(iterator.next())) {
					iterator.remove();
				}
			}
		}

		Set<Object> valuesToCheck = new HashSet<>();
		valuesToCheck.addAll(currentValues);
		valuesToCheck.add(valueToAdd);
		checkSchema(attrName, valuesToCheck, "add");

		currentValues.add(valueToAdd);
	}

	public void removeAttributeValue(String name, Object value) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		Collection<Object> values = new ArrayList<>();
		values.add(value);
		removeAttributeValues(name, values);
	}

	public <T> void removeAttributeValues(String name, Collection<T> values) throws SchemaViolationException, ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
		checkModifyBreak();
		delayOperation();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<>();
			attributes.put(name, currentValues);
		}

		Set<Object> valuesToCheck = new HashSet<>();
		valuesToCheck.addAll(currentValues);
		valuesToCheck.removeAll(values);
		checkSchema(name, valuesToCheck, "remove");

		Iterator<Object> iterator = currentValues.iterator();
		boolean foundMember = false;

		if (name.equals(DummyGroup.ATTR_MEMBERS_NAME) && !resource.isTolerateDuplicateValues()){
			checkIfExist(values, currentValues);
		}

		while(iterator.hasNext()) {
			Object currentValue = iterator.next();
			boolean found = false;
			for (Object value: values) {
				if (resource.isCaseIgnoreValues() && currentValue instanceof String && value instanceof String) {
					if (StringUtils.equalsIgnoreCase((String)currentValue, (String)value)) {
						found = true;
						break;
					}
				} else {
					if (currentValue.equals(value)) {
						found = true;
						break;
					}
				}
			}
			if (found) {
				iterator.remove();
			}

		}

		recordModify(name, null, values, null);
	}

	public Set<String> getAuxiliaryObjectClassNames() {
		return auxiliaryObjectClassNames;
	}

	public void addAuxiliaryObjectClassName(String name) {
		auxiliaryObjectClassNames.add(name);
	}

	public void replaceAuxiliaryObjectClassNames(List<?> values) {
		auxiliaryObjectClassNames.clear();
		addAuxiliaryObjectClassNames(values);
	}

	public void deleteAuxiliaryObjectClassNames(List<?> values) {
		for (Object value : values) {
			auxiliaryObjectClassNames.remove(String.valueOf(value));
		}
	}

	public void addAuxiliaryObjectClassNames(List<?> values) {
		for (Object value : values) {
			auxiliaryObjectClassNames.add(String.valueOf(value));
		}
	}

	private <T> void checkIfExist(Collection<T> valuesToDelete, Set<Object> currentValues) throws SchemaViolationException{
		for (T valueToDelete : valuesToDelete) {
			boolean found = false;
			for (Object currentValue : currentValues) {
				if (resource.isCaseIgnoreValues() && currentValue instanceof String && valueToDelete instanceof String) {
					if (StringUtils.equalsIgnoreCase((String)currentValue, (String)valueToDelete)) {
						found = true;
						break;
					}
				} else {
					if (currentValue.equals(valueToDelete)) {
						found = true;
						break;
					}
				}
			}

			if (!found){
				throw new SchemaViolationException("no such member: " + valueToDelete + " in " + currentValues);
			}
		}
	}

	protected void checkModifyBreak() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		if (resource == null) {
			return;
		}
		BreakMode modifyBreakMode = this.modifyBreakMode;
		if (modifyBreakMode == null) {
			modifyBreakMode = resource.getModifyBreakMode();
		}
		if (modifyBreakMode == BreakMode.NONE) {
			// go on
		} else if (modifyBreakMode == BreakMode.NETWORK) {
			throw new ConnectException("Network error (simulated error)");
		} else if (modifyBreakMode == BreakMode.IO) {
			throw new FileNotFoundException("IO error (simulated error)");
		} else if (modifyBreakMode == BreakMode.SCHEMA) {
			throw new SchemaViolationException("Schema violation (simulated error)");
		} else if (modifyBreakMode == BreakMode.CONFLICT) {
			throw new ConflictException("Conflict (simulated error)");
		} else if (modifyBreakMode == BreakMode.GENERIC) {
			// The connector will react with generic exception
			throw new IllegalArgumentException("Generic error (simulated error)");
		} else if (modifyBreakMode == BreakMode.RUNTIME) {
			// The connector will just pass this up
			throw new IllegalStateException("Generic error (simulated error)");
		} else if (modifyBreakMode == BreakMode.UNSUPPORTED) {
			throw new UnsupportedOperationException("Not supported (simulated error)");
		} else {
			// This is a real error. Use this strange thing to make sure it passes up
			throw new RuntimeException("Unknown schema break mode "+modifyBreakMode);
		}
	}

	private <T> void recordModify(String attributeName, Collection<T> valuesAdded, Collection<T> valuesDeleted, Collection<T> valuesReplaced) {
		if (resource != null) {
			resource.recordModify(this, attributeName, valuesAdded, valuesDeleted, valuesReplaced);
		}
	}

	private void delayOperation() throws InterruptedException {
		if (resource != null) {
			resource.delayOperation();
		}
	}

	protected void checkSchema(String attrName, Collection<Object> values, String operationName) throws SchemaViolationException {
		if (resource == null || !resource.isEnforceSchema()) {
			return;
		}
		DummyObjectClass accountObjectClass;
		try {
			accountObjectClass = getObjectClass();
		} catch (Exception e) {
			// No not enforce schema if the schema is broken (simulated)
			return;
		}
		if (accountObjectClass == null) {
			// Nothing to check
			return;
		}
		DummyAttributeDefinition attributeDefinition = getAttributeDefinition(attrName);
		if (attributeDefinition == null) {
			throw new SchemaViolationException("Attribute "+attrName+" is not defined in resource schema");
		}
		if (attributeDefinition.isRequired() && (values == null || values.isEmpty())) {
			throw new SchemaViolationException(operationName + " of required attribute "+attrName+" results in no values");
		}
		if (!attributeDefinition.isMulti() && values != null && values.size() > 1) {
			throw new SchemaViolationException(operationName + " of single-valued attribute "+attrName+" results in "+values.size()+" values");
		}
	}

	public DummyAttributeDefinition getAttributeDefinition(String attrName) {
		DummyAttributeDefinition def = getObjectClassNoExceptions().getAttributeDefinition(attrName);
		if (def != null) {
			return def;
		}
		for (String auxClassName : getAuxiliaryObjectClassNames()) {
			DummyObjectClass auxObjectClass = resource.getAuxiliaryObjectClassMap().get(auxClassName);
			if (auxObjectClass == null) {
				throw new IllegalStateException("Auxiliary object class " + auxClassName + " couldn't be found");
			}
			def = auxObjectClass.getAttributeDefinition(attrName);
			if (def != null) {
				return def;
			}
		}
		return null;
	}

	abstract protected DummyObjectClass getObjectClass() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException;

	abstract protected DummyObjectClass getObjectClassNoExceptions();

	public abstract String getShortTypeName();

	@Override
	public String toString() {
		return getClass().getSimpleName()+"(" + toStringContent() + ")";
	}

	protected String toStringContent() {
		return "name=" + name + ", attributes=" + attributes + ", enabled=" + enabled;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(getClass().getSimpleName());
		sb.append(": ").append(name);
		if (id != null && !id.equals(name)) {
			sb.append(" (").append(id).append(")");
		}
		if (!auxiliaryObjectClassNames.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabelToString(sb, "Auxiliary object classes", auxiliaryObjectClassNames, indent + 1);
		}
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToString(sb, "Enabled", enabled, indent + 1);
		if (validFrom != null || validTo != null) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Validity", indent + 1);
			sb.append(" ").append(PrettyPrinter.prettyPrint(validFrom)).append(" - ").append(PrettyPrinter.prettyPrint(validTo));
		}
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "lastModifier", lastModifier, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Attributes", attributes, indent + 1);
		extendDebugDump(sb, indent);
		return sb.toString();
	}

	protected void extendDebugDump(StringBuilder sb, int indent) {
		// Nothing to do
	}

	public boolean isReturnedByDefault(String attrName) {
		final DummyAttributeDefinition attributeDefinition = getAttributeDefinition(attrName);
		if (attributeDefinition != null) {
			return attributeDefinition.isReturnedByDefault();
		} else {
			System.out.println("Warning: attribute " + attrName + " is not defined in " + this);
			return false;
		}
	}
}

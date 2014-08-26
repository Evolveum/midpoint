/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.icf.dummy.resource;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
	private String name;
	private Map<String,Set<Object>> attributes = new HashMap<String, Set<Object>>();
	private boolean enabled = true;
	private Date validFrom = null;
	private Date validTo = null;
	protected DummyResource resource;
	
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
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) throws ConnectException, FileNotFoundException {
		checkModifyBreak();
		this.enabled = enabled;
	}

	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(Date validFrom) throws ConnectException, FileNotFoundException {
		checkModifyBreak();
		this.validFrom = validFrom;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(Date validTo) throws ConnectException, FileNotFoundException {
		checkModifyBreak();
		this.validTo = validTo;
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

	public void replaceAttributeValue(String name, Object value) throws SchemaViolationException, ConnectException, FileNotFoundException {
		Collection<Object> values = new ArrayList<Object>(1);
		values.add(value);
		replaceAttributeValues(name, values);
	}
	
	public void replaceAttributeValues(String name, Collection<Object> values) throws SchemaViolationException, ConnectException, FileNotFoundException {
		checkModifyBreak();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		} else {
			currentValues.clear();
		}
		currentValues.addAll(values);
		checkSchema(name, values, "relace");
		recordModify();
	}
	
	public void replaceAttributeValues(String name, Object... values) throws SchemaViolationException, ConnectException, FileNotFoundException {
		checkModifyBreak();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		} else {
			currentValues.clear();
		}
		List<Object> valuesList = Arrays.asList(values);
		currentValues.addAll(valuesList);
		checkSchema(name, valuesList, "relace");
		recordModify();
	}
	
	public void addAttributeValue(String name, Object value) throws SchemaViolationException, ConnectException, FileNotFoundException {
		Collection<Object> values = new ArrayList<Object>(1);
		values.add(value);
		addAttributeValues(name, values);
	}

	public void addAttributeValues(String name, Collection<Object> valuesToAdd) throws SchemaViolationException, ConnectException, FileNotFoundException {
		checkModifyBreak();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		}
		for(Object valueToAdd: valuesToAdd) {
			addAttributeValue(name, currentValues, valueToAdd);
		}
		recordModify();
	}
	
	public void addAttributeValues(String name, String... valuesToAdd) throws SchemaViolationException, ConnectException, FileNotFoundException {
		checkModifyBreak();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		}
		for (Object valueToAdd: valuesToAdd) {
			addAttributeValue(name, currentValues, valueToAdd);
		}
		recordModify();
	}
	
	private void addAttributeValue(String attrName, Set<Object> currentValues, Object valueToAdd) throws SchemaViolationException, ConnectException, FileNotFoundException {
		checkModifyBreak();
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
		
		Set<Object> valuesToCheck = new HashSet<Object>();
		valuesToCheck.addAll(currentValues);
		valuesToCheck.add(valueToAdd);
		checkSchema(attrName, valuesToCheck, "add");
		
		currentValues.add(valueToAdd);
	}
	
	public void removeAttributeValue(String name, Object value) throws SchemaViolationException, ConnectException, FileNotFoundException {
		Collection<Object> values = new ArrayList<Object>();
		values.add(value);
		removeAttributeValues(name, values);
	}

	public void removeAttributeValues(String name, Collection<Object> values) throws SchemaViolationException, ConnectException, FileNotFoundException {
		checkModifyBreak();
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		}
		
		Set<Object> valuesToCheck = new HashSet<Object>();
		valuesToCheck.addAll(currentValues);
		valuesToCheck.removeAll(values);
		checkSchema(name, valuesToCheck, "remove");
		
		currentValues.removeAll(values);
		recordModify();
	}

	private void checkModifyBreak() throws ConnectException, FileNotFoundException {
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

	private void recordModify() {
		if (resource != null) {
			resource.recordModify(this);
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
		DummyAttributeDefinition attributeDefinition = accountObjectClass.getAttributeDefinition(attrName);
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

	abstract protected DummyObjectClass getObjectClass() throws ConnectException, FileNotFoundException;

	public abstract String getShortTypeName();
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"(" + toStringContent() + ")";
	}
	
	protected String toStringContent() {
		return "username=" + name + ", attributes=" + attributes + ", enabled=" + enabled;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("DummyAccount: ").append(name).append("\n");
		DebugUtil.debugDumpWithLabelToString(sb, "Enabled", enabled, indent + 1);
		if (validFrom != null || validTo != null) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Validity", indent + 1);
			sb.append(" ").append(PrettyPrinter.prettyPrint(validFrom)).append(" - ").append(PrettyPrinter.prettyPrint(validTo));
		}
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Attributes", attributes, indent + 1);
		extendDebugDump(sb, indent);
		return sb.toString();
	}

	protected void extendDebugDump(StringBuilder sb, int indent) {
		// Nothing to do
	}
	
}

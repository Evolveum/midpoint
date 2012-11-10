/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.icf.dummy.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Radovan Semancik
 *
 */
public class DummyAccount {
	
	private String username;
	private Map<String,Set<Object>> attributes;
	private boolean enabled;
	private String password;
	private DummyResource resource;

	public DummyAccount() {
		attributes = new HashMap<String, Set<Object>>();
		enabled = true;
		password = null;
	}

	public DummyAccount(String username) {
		this.username = username;
		attributes = new HashMap<String, Set<Object>>();
		enabled = true;
		password = null;
	}
	
	public DummyResource getResource() {
		return resource;
	}

	public void setResource(DummyResource resource) {
		this.resource = resource;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

	public void replaceAttributeValue(String name, Object value) throws SchemaViolationException {
		Collection<Object> values = new ArrayList<Object>(1);
		values.add(value);
		replaceAttributeValues(name, values);
	}
	
	public void replaceAttributeValues(String name, Collection<Object> values) throws SchemaViolationException {
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
	
	public void addAttributeValue(String name, Object value) throws SchemaViolationException {
		Collection<Object> values = new ArrayList<Object>(1);
		values.add(value);
		addAttributeValues(name, values);
	}

	public void addAttributeValues(String name, Collection<Object> valuesToAdd) throws SchemaViolationException {
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
	
	public void addAttributeValues(String name, String... valuesToAdd) throws SchemaViolationException {
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
	
	private void addAttributeValue(String attrName, Set<Object> currentValues, Object valueToAdd) throws SchemaViolationException {
		if (resource != null && !resource.isTolerateDuplicateValues()) {
			for (Object currentValue: currentValues) {
				if (currentValue.equals(valueToAdd)) {
					throw new IllegalArgumentException("The value '"+valueToAdd+"' conflicts with existing value");
				}
			}
		}
		
		Set<Object> valuesToCheck = new HashSet<Object>();
		valuesToCheck.addAll(currentValues);
		valuesToCheck.add(valueToAdd);
		checkSchema(attrName, valuesToCheck, "add");
		
		currentValues.add(valueToAdd);
	}

	public void removeAttributeValues(String name, Collection<Object> values) throws SchemaViolationException {
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

	private void recordModify() {
		if (resource != null) {
			resource.recordModify(this);
		}
	}

	private void checkSchema(String attrName, Collection<Object> values, String operationName) throws SchemaViolationException {
		if (resource == null || !resource.isEnforceSchema()) {
			return;
		}
		DummyObjectClass accountObjectClass = resource.getAccountObjectClass();
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

	
	@Override
	public String toString() {
		return "DummyAccount(username=" + username + ", attributes=" + attributes + ", enabled=" + enabled
				+ ", password=" + password + ")";
	}
	
}

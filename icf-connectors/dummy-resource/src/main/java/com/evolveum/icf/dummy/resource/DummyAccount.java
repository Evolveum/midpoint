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

	public void replaceAttributeValues(String name, Collection<Object> values) {
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		} else {
			currentValues.clear();
		}
		currentValues.addAll(values);
	}

	public void addAttributeValues(String name, Collection<Object> values) {
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		}
		currentValues.addAll(values);
	}

	public void removeAttributeValues(String name, Collection<Object> values) {
		Set<Object> currentValues = attributes.get(name);
		if (currentValues == null) {
			currentValues = new HashSet<Object>();
			attributes.put(name, currentValues);
		}
		currentValues.removeAll(values);
	}
	
}

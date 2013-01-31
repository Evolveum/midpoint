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

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource for use with dummy ICF connector.
 * 
 * This is a simple Java object that pretends to be a resource. It has accounts and
 * account schema. It has operations to manipulate accounts, execute scripts and so on
 * almost like a real resource. The purpose is to simulate a real resource with avery 
 * little overhead.
 * 
 * The resource is a singleton, therefore the resource instance can be shared by
 * the connector and the test code. The usual story is like this:
 * 
 * 1) test class fetches first instance of the resource (getInstance). This will cause
 * loading of the resource class in the test (parent) classloader.
 * 
 * 2) test class configures the connector (e.g. schema) usually by calling the populateWithDefaultSchema() method.
 * 
 * 3) test class initializes IDM. This will cause connector initialization. The connector will fetch
 * the instance of dummy resource. As it was loaded by the parent classloader, it will get the same instance
 * as the test class.
 * 
 * 4) test class invokes IDM operation. That will invoke connector and change the resource.
 * 
 * 5) test class will access resource directly to see if the operation went OK.
 * 
 * The dummy resource is a separate package (JAR) from the dummy connector. Connector has its own
 * classloader. If the resource would be the same package as connector, it will get loaded by the
 * connector classloader regardless whether it is already loaded by the parent classloader.
 * 
 * @author Radovan Semancik
 *
 */
public class DummyResource {

	private Map<String,DummyAccount> accounts;
	private List<ScriptHistoryEntry> scriptHistory;
	private DummyObjectClass accountObjectClass;
	private DummySyncStyle syncStyle;
	private List<DummyDelta> deltas;
	private int latestSyncToken;
	private boolean tolerateDuplicateValues = false;
	private boolean enforceSchema = true;
	
	private BreakMode schemaBreakMode = BreakMode.NONE;
	
	// Following two properties are just copied from the connector
	// configuration and can be checked later. They are otherwise
	// completely useless.
	private String uselessString;
	private String uselessGuardedString;
	
	private static Map<String, DummyResource> instances = new HashMap<String, DummyResource>();
	
	DummyResource() {
		accounts = new ConcurrentHashMap<String, DummyAccount>();
		scriptHistory = new ArrayList<ScriptHistoryEntry>();
		accountObjectClass = new DummyObjectClass();
		syncStyle = DummySyncStyle.NONE;
		deltas = new ArrayList<DummyDelta>();
		latestSyncToken = 0;
	}
	
	/**
	 * Clears everything, just like the resouce was just created.
	 */
	public void reset() {
		accounts.clear();
		scriptHistory.clear();
		accountObjectClass = new DummyObjectClass();
		syncStyle = DummySyncStyle.NONE;
		deltas.clear();
		latestSyncToken = 0;
	}
	
	public static DummyResource getInstance() {
		return getInstance(null);
	}
	
	public static DummyResource getInstance(String instanceName) {
		DummyResource instance = instances.get(instanceName);
		if (instance == null) {
			instance = new DummyResource();
			instances.put(instanceName, instance);
		}
		return instance;
	}
	
	public boolean isTolerateDuplicateValues() {
		return tolerateDuplicateValues;
	}

	public void setTolerateDuplicateValues(boolean tolerateDuplicateValues) {
		this.tolerateDuplicateValues = tolerateDuplicateValues;
	}

	public boolean isEnforceSchema() {
		return enforceSchema;
	}

	public void setEnforceSchema(boolean enforceSchema) {
		this.enforceSchema = enforceSchema;
	}

	public BreakMode getSchemaBreakMode() {
		return schemaBreakMode;
	}

	public void setSchemaBreakMode(BreakMode schemaBreakMode) {
		this.schemaBreakMode = schemaBreakMode;
	}

	public String getUselessString() {
		return uselessString;
	}

	public void setUselessString(String uselessString) {
		this.uselessString = uselessString;
	}

	public String getUselessGuardedString() {
		return uselessGuardedString;
	}

	public void setUselessGuardedString(String uselessGuardedString) {
		this.uselessGuardedString = uselessGuardedString;
	}

	public DummyObjectClass getAccountObjectClass() throws ConnectException, FileNotFoundException {
		if (schemaBreakMode == BreakMode.NONE) {
			return accountObjectClass;
		} else if (schemaBreakMode == BreakMode.NETWORK) {
			throw new ConnectException("The schema is not available (simulated error)");
		} else if (schemaBreakMode == BreakMode.IO) {
			throw new FileNotFoundException("The schema file not found (simulated error)");
		} else if (schemaBreakMode == BreakMode.GENERIC) {
			// The connector will react with generic exception
			throw new IllegalArgumentException("Generic error fetching schema (simulated error)");
		} else if (schemaBreakMode == BreakMode.RUNTIME) {
			// The connector will just pass this up
			throw new IllegalStateException("Generic error fetching schema (simulated error)");
		} else if (schemaBreakMode == BreakMode.UNSUPPORTED) {
			throw new UnsupportedOperationException("Schema is not supported (simulated error)");
		} else {
			// This is a real error. Use this strange thing to make sure it passes up
			throw new RuntimeException("Unknown schema break mode "+schemaBreakMode);
		}
		
	}

	public Collection<DummyAccount> listAccounts() {
		return accounts.values();
	}
	
	public DummyAccount getAccountByUsername(String username) {
		return accounts.get(username);
	}
	
	public String addAccount(DummyAccount newAccount) throws ObjectAlreadyExistsException {
		String id = newAccount.getUsername();
		if (accounts.containsKey(id)) {
			throw new ObjectAlreadyExistsException("Account with identifier "+id+" already exists");
		}
		
		newAccount.setResource(this);
		accounts.put(id, newAccount);
		
		if (syncStyle != DummySyncStyle.NONE) {
			int syncToken = nextSyncToken();
			DummyDelta delta = new DummyDelta(syncToken, id, DummyDeltaType.ADD);
			deltas.add(delta);
		}
		
		return id;
	}
	
	public void deleteAccount(String id) throws ObjectDoesNotExistException {
		if (accounts.containsKey(id)) {
			accounts.remove(id);
		} else {
			throw new ObjectDoesNotExistException("Account with identifier "+id+" does not exist");
		}
		
		if (syncStyle != DummySyncStyle.NONE) {
			int syncToken = nextSyncToken();
			DummyDelta delta = new DummyDelta(syncToken, id, DummyDeltaType.DELETE);
			deltas.add(delta);
		}
	}
	
	void recordModify(DummyAccount account) {
		if (syncStyle != DummySyncStyle.NONE) {
			int syncToken = nextSyncToken();
			DummyDelta delta = new DummyDelta(syncToken, account.getUsername(), DummyDeltaType.MODIFY);
			deltas.add(delta);
		}
	}

	/**
	 * Returns script history ordered chronologically (oldest first).
	 * @return script history
	 */
	public List<ScriptHistoryEntry> getScriptHistory() {
		return scriptHistory;
	}
	
	/**
	 * Clears the script history.
	 */
	public void purgeScriptHistory() {
		scriptHistory.clear();
	}
	
	/**
	 * Pretend to run script on the resource.
	 * The script is actually not executed, it is only recorded in the script history
	 * and can be fetched by getScriptHistory().
	 * 
	 * @param scriptCode code of the script
	 */
	public void runScript(String language, String scriptCode, Map<String, Object> params) {
		scriptHistory.add(new ScriptHistoryEntry(language, scriptCode, params));
	}
	
	/**
	 * Populates the resource with some kind of "default" schema. This is a schema that should suit
	 * majority of basic test cases.
	 */
	public void populateWithDefaultSchema() {
		accountObjectClass.clear();
		accountObjectClass.addAttributeDefinition("fullname", String.class, true, false);
		accountObjectClass.addAttributeDefinition("description", String.class, false, false);
		accountObjectClass.addAttributeDefinition("interests", String.class, false, true);
	}

	public DummySyncStyle getSyncStyle() {
		return syncStyle;
	}

	public void setSyncStyle(DummySyncStyle syncStyle) {
		this.syncStyle = syncStyle;
	}

	private synchronized int nextSyncToken() {
		return ++latestSyncToken;
	}

	public int getLatestSyncToken() {
		return latestSyncToken;
	}
	
	public List<DummyDelta> getDeltasSince(int syncToken) {
		List<DummyDelta> result = new ArrayList<DummyDelta>();
		for (DummyDelta delta: deltas) {
			if (delta.getSyncToken() > syncToken) {
				result.add(delta);
			}
		}
		return result;
	}
	
	public String dump() {
		StringBuilder sb = new StringBuilder(toString());
		sb.append("\nAccounts:");
		for (Entry<String, DummyAccount> entry: accounts.entrySet()) {
			sb.append("\n  ");
			sb.append(entry.getKey());
			sb.append(": ");
			sb.append(entry.getValue());
		}
		sb.append("\nDeltas:");
		for (DummyDelta delta: deltas) {
			sb.append("\n  ");
			sb.append(delta);
		}
		sb.append("\nLatest token:").append(latestSyncToken);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "DummyResource("+accounts.size()+" accounts)";
	}
	
}
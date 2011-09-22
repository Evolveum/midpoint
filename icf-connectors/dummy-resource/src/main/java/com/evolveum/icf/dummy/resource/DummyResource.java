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
import java.util.List;
import java.util.Map;

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
	private List<String> scriptHistory;
	private DummyObjectClass accountObjectClass;
	
	private static DummyResource instance = null;
	
	DummyResource() {
		accounts = new HashMap<String, DummyAccount>();
		scriptHistory = new ArrayList<String>();
		accountObjectClass = new DummyObjectClass();
	}
	
	public static DummyResource getInstance() {
		if (instance == null) {
			instance = new DummyResource();
		}
		return instance;
	}
	
	public DummyObjectClass getAccountObjectClass() {
		return accountObjectClass;
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
		accounts.put(id, newAccount);
		return id;
	}
	
	public void deleteAccount(String id) throws ObjectDoesNotExistException {
		if (accounts.containsKey(id)) {
			accounts.remove(id);
		} else {
			throw new ObjectDoesNotExistException("Account with identifier "+id+" does not exist");
		}
	}

	/**
	 * Returns script history ordered chronologically (oldest first).
	 * @return script history
	 */
	public List<String> getScriptHistory() {
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
	public void runScript(String scriptCode) {
		scriptHistory.add(scriptCode);
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
	
}
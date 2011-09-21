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
package com.evolveum.icf.dummy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Radovan Semancik
 *
 */
public class DummyResource {

	private Map<String,DummyAccount> accounts;
	private List<String> scriptHistory;
	
	private static DummyResource instance = null;
	
	private DummyResource() {
		accounts = new HashMap<String, DummyAccount>();
		scriptHistory = new ArrayList<String>();
	}
	
	public static DummyResource getInstance() {
		if (instance == null) {
			instance = new DummyResource();
		}
		return instance;
	}
	
	public Collection<DummyAccount> listAccounts() {
		return accounts.values();
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

	public List<String> getScriptHistory() {
		return scriptHistory;
	}
	
	public void purgeScriptHistory() {
		scriptHistory.clear();
	}
	
	public void runScript(String scriptCode) {
		scriptHistory.add(scriptCode);
	}
	
}

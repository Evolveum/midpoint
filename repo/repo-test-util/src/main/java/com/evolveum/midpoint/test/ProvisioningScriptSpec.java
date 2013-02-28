/**
 * Copyright (c) 2013 Evolveum
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
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 *
 */
public class ProvisioningScriptSpec {
	
	private String code;
	private Map<String,Object> args = new HashMap<String, Object>();
	private String language;

	public ProvisioningScriptSpec(String code) {
		super();
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Map<String, Object> getArgs() {
		return args;
	}

	public void setArgs(Map<String, Object> args) {
		this.args = args;
	}
	
	public void addArgSingle(String name, String val) {
		args.put(name, val);
	}
	
	public void addArgMulti(String name, String... val) {
		args.put(name, Arrays.asList(val));
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
	
	

}

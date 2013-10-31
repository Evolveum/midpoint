/*
 * Copyright (c) 2013 Evolveum
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

package com.evolveum.midpoint.provisioning.ucf.impl;

/**
 * This enum contains ICF operational attributes that are used in ICF but are not defined there.
 * The operational attributes are in form __SOME_NAME__.
 * 
 * NOTE: This attributes also needs to be defined in the resource-schema XSD!
 *
 */
public enum SecretIcfOperationalAttributes {

	DESCRIPTION("__DESCRIPTION__"),
	GROUPS("__GROUPS__"),
	LAST_LOGIN_DATE("__LAST_LOGIN_DATE__");
	
	private String name;
	
	private SecretIcfOperationalAttributes(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}

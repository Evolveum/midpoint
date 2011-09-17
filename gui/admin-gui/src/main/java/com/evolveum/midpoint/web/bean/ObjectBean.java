/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.bean;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author lazyman
 * 
 */
public class ObjectBean implements Serializable {

	private static final long serialVersionUID = 7834659389991367819L;
	private String name;
	private String oid;
	private String description;
	
	public ObjectBean(String oid, String name) {
		this(oid, name, null);
	}

	public ObjectBean(String oid, String name, String description) {
		if (StringUtils.isEmpty(oid)) {
			throw new IllegalArgumentException("Oid can't be null.");
		}
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Name can't be null.");
		}
		this.oid = oid;
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getOid() {
		return oid;
	}

	public String getDescription() {
		return description;
	}
}

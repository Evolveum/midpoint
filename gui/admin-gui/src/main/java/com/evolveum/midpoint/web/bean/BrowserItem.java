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

/**
 * 
 * @author lazyman
 * 
 */
public class BrowserItem implements Serializable {

	private static final long serialVersionUID = -1239170551953243552L;
	private String oid;
	private String name;
	private String type;

	public BrowserItem(String oid, String name, String type) {
		this.oid = oid;
		this.name = name;
		this.type = type;
	}

	public String getOid() {
		return oid;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}
}

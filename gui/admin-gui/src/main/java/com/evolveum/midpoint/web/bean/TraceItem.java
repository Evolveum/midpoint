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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.bean;

import java.io.Serializable;

/**
 * 
 * @author Vilo Repan
 */
public class TraceItem implements Serializable {

	private static final long serialVersionUID = 8346151495344702621L;
	private int id;
	private String packageName;
	private int level;

	public TraceItem(int id) {
		this(id, null, 0);
	}

	public TraceItem(int id, String packageName, int level) {
		this.id = id;
		this.packageName = packageName;
		this.level = level;
	}

	public int getId() {
		return id;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
}

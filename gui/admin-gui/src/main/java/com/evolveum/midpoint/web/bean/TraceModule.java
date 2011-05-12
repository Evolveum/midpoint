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
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author lazyman
 */
public class TraceModule implements Serializable {

	private static final long serialVersionUID = -6575035946287918109L;
	private int level;
	private String name;
	private String logPattern;
	private List<TraceItem> items;

	public TraceModule(int level, String logPattern, String name) {
		this.level = level;
		this.logPattern = logPattern;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<TraceItem> getItems() {
		if (items == null) {
			items = new ArrayList<TraceItem>();
		}

		return items;
	}

	public int getItemsSize() {
		return getItems().size();
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getLogPattern() {
		return logPattern;
	}

	public void setLogPattern(String logPattern) {
		this.logPattern = logPattern;
	}
}

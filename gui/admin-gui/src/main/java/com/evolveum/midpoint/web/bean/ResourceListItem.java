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

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author lazyman
 * 
 */
public class ResourceListItem extends SelectableBean {

	private static final long serialVersionUID = 8252381052377454312L;
	private String oid;
	private String name;
	private String type;
	private String version;
	private ResourceState state;
	private ResourceSync sync;
	private int progress;

	public ResourceListItem(String oid, String name, String type, String version) {
		if (StringUtils.isEmpty(oid)) {
			throw new IllegalArgumentException("Oid must not be null.");
		}
		this.oid = oid;
		this.name = name;
		this.type = type;
		this.version = version;
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

	public String getVersion() {
		return version;
	}

	public ResourceStatus getOverallStatus() {
		if (state == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return state.getOverall();
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public ResourceState getState() {
		if (state == null) {
			state = new ResourceState();
		}
		return state;
	}

	public ResourceSync getSync() {
		if (sync == null) {
			sync = new ResourceSync();
		}
		return sync;
	}
}

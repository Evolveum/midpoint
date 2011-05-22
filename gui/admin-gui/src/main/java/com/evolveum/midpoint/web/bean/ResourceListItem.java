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
public class ResourceListItem implements Serializable {

	private static final long serialVersionUID = 8252381052377454312L;

	public enum ConnectionStatus {
		SUCCESS, WARNING, ERROR, NOT_TESTED;
	}

	private boolean selected = false;
	private String oid;
	private String name;
	private String type;
	private String version;
	private ConnectionStatus status = ConnectionStatus.NOT_TESTED;
	private int progress;

	public ResourceListItem(String oid, String name, String type, String version) {
		this.oid = oid;
		this.name = name;
		this.type = type;
		this.version = version;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
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

	public ConnectionStatus getStatus() {
		return status;
	}

	public void setStatus(ConnectionStatus status) {
		this.status = status;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}
}

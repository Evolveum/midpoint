/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.web.component.orgStruct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;

/**
 * @author mserbak
 */
public class NodeDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private String oid;
	private boolean loaded;
	private NodeDto parent;
	private String displayName;
	private NodeType type = NodeType.MEMBER;
	private List<NodeType> listTypes;
	private boolean editable;

	private List<NodeDto> nodes = new ArrayList<NodeDto>();

	public NodeDto(NodeDto parent, String displayName, String oid, NodeType type) {
		this.parent = parent;
		this.displayName = displayName;
		this.oid = oid;
		if (type != null) {
			this.type = type;
			addTypeToListTypes(type);
		}
	}

	public NodeType getType() {
		return listTypes.get(0);
	}

	public NodeDto getParent() {
		return parent;
	}
	
	public void setParent(NodeDto parent) {
		this.parent = parent;
	}
	
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getOid() {
		return oid;
	}
	
	public List<NodeType> getListTypes() {
		return listTypes;
	}
	
	public void addTypeToListTypes(NodeType type) {
		if(listTypes == null) {
			listTypes = new ArrayList<NodeType>();
		}
		listTypes.add(type);
	}

	public List<NodeDto> getNodes() {
		return Collections.unmodifiableList(nodes);
	}
	
	public void setNodes(List<NodeDto> nodes) {
		this.nodes = nodes;
	}
	
	public void addNode(NodeDto node) {
		nodes.add(node);
	}
	
	public NodeDto getNodeFromOid(String oid) {
		for (NodeDto node : nodes) {
			if(node.getOid().equals(oid)) {
				return node;
			}
		}
		return null;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((oid == null) ? 0 : oid.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeDto other = (NodeDto) obj;
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	
}

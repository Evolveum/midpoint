/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.web.component.orgStruct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;

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

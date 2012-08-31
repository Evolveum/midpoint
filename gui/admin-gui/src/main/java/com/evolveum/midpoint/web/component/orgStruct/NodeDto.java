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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author mserbak
 */
public class NodeDto {

	private static final long serialVersionUID = 1L;

	private String id;
	private String oid;
	private boolean loaded;
	private NodeDto parent;
	private NodeType type = NodeType.USER;

	private List<NodeDto> nodes = new ArrayList<NodeDto>();

	public NodeDto(String id, String oid, NodeType type) {
		this.id = id;
		this.oid = oid;
		if (type != null) {
			this.type = type;
		}
	}

	public NodeDto(NodeDto parent, String name, String oid) {
		this(parent, name, oid, null);
	}

	public NodeDto(NodeDto parent, String name, String oid, NodeType type) {
		this(name, oid, type);
		this.parent = parent;
		this.parent.nodes.add(this);
	}

	public NodeType getType() {
		return type;
	}
	
	public void setType(NodeType type) {
		this.type = type;
	}

	public NodeDto getParent() {
		return parent;
	}

	public String getId() {
		return id;
	}

	public String getOid() {
		return oid;
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

	@Override
	public String toString() {
		return id;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}
}

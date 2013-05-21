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
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import wickettree.ITreeProvider;
import wickettree.util.IntermediateTreeProvider;

import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author mserbak
 */
public class OrgStructProvider implements ITreeProvider<NodeDto> {
	private boolean intermediate;
	private List<NodeDto> roots = new ArrayList<NodeDto>();
	List<NodeDto> nodes = new ArrayList<NodeDto>();

	public OrgStructProvider(IModel<OrgStructDto> model) {
		this(false);
		initNodes(model);
	}

	private void initNodes(IModel<OrgStructDto> model) {
		OrgStructDto orgStruct = model.getObject();
		roots.add((NodeDto)orgStruct.getOrgUnitDtoList().get(0));
	}

	public OrgStructProvider(boolean intermediate) {
		this.intermediate = intermediate;
	}

	public Iterator<NodeDto> getRoots() {
		return roots.iterator();
	}

	public boolean hasChildren(NodeDto node) {
		return node.getType().equals(NodeType.FOLDER);
	}

	public Iterator<NodeDto> getChildren(final NodeDto node) {
		if (intermediate) {
			if (!node.isLoaded()) {
				asynchronuous(new Runnable() {
					public void run() {
						node.setLoaded(true);
					}
				});

				// mark children intermediate
				return IntermediateTreeProvider.intermediate(Collections.<NodeDto> emptyList().iterator());
			}
		}

		return node.getNodes().iterator();
	}

	private void asynchronuous(Runnable runnable) {
		runnable.run();
	}

	public void resetLoaded() {
		for (NodeDto node : roots) {
			resetLoaded(node);
		}
	}

	private static void resetLoaded(NodeDto node) {
		node.setLoaded(false);

		for (NodeDto child : node.getNodes()) {
			resetLoaded(child);
		}
	}

	public IModel<NodeDto> model(NodeDto node) {
		return new NodeModel(node);
	}

	public NodeDto get(NodeDto nodeDto) {
		return get(roots, nodeDto);
	}

	private NodeDto get(List<NodeDto> nodes, NodeDto nodeDto) {
		for (NodeDto node : nodes) {
			if (node.equals(nodeDto)) {
				return node;
			}

			NodeDto temp = get(node.getNodes(), nodeDto);
			if (temp != null) {
				return temp;
			}
		}

		return null;
	}

	private class NodeModel extends LoadableDetachableModel<NodeDto> {
		private static final long serialVersionUID = 1L;

		private NodeDto nodeDto;

		public NodeModel(NodeDto node) {
			super(node);

			this.nodeDto = node;
		}

		@Override
		protected NodeDto load() {
			return get(nodeDto);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NodeModel) {
				return ((NodeModel) obj).nodeDto.equals(this.nodeDto);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return nodeDto.hashCode();
		}
	}

	@Override
	public void detach() {
	}
}

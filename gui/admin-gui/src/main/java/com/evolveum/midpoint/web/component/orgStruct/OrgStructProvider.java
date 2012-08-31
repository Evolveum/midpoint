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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

import wickettree.ITreeProvider;
import wickettree.util.IntermediateTreeProvider;

/**
 * @author mserbak
 */
public class OrgStructProvider implements ITreeProvider<NodeDto> {
	private boolean intermediate;
	private List<NodeDto> roots = new ArrayList<NodeDto>();
	List<NodeDto> nodes = new ArrayList<NodeDto>();
	private NodeDto root = null;
	private IModel<OrgStructDto> model;

	public OrgStructProvider(IModel<OrgStructDto> model) {
		this(false);
		this.model = model;
		initNodes(model);
		// roots.addAll(initNodes(model));
	}

	public OrgStructProvider() {
		this(false);
	}

	private void initNodes(IModel<OrgStructDto> model) {
		OrgStructDto orgStruct = model.getObject();

		for (OrgType orgUnit : orgStruct.getOrgUnitList()) {
			if (root == null) {
				root = new NodeDto(orgUnit.getDisplayName().toString(), orgUnit.getOid(), NodeType.FOLDER);
				continue;
			}
			createOrgUnit(orgUnit);
		}
		
		for (UserType user : orgStruct.getUserList()) {
			createUserUnit(user);
		}
		
		roots.add(root);

		// NodeDto sub1 = new NodeDto("Subdir 1", "");
		// nodes.add(sub1);
		//
		// NodeDto sub2 = new NodeDto("Subdir 2", "");
		// {
		// new NodeDto(sub2, "ABA", "");
		// new NodeDto(sub2, "Subdir 2_1", "");
		// }
		// nodes.add(sub2);
	}

	private void createOrgUnit(OrgType unit) {
		List<NodeDto> list = new ArrayList<NodeDto>();
		for (NodeDto parent : getParentFromOrgRef(unit.getOrgRef())) {
			// TODO: what if orgUnit has more that one orgRef ????

			NodeDto org = new NodeDto(parent, unit.getDisplayName().toString(), unit.getOid(),
					NodeType.FOLDER);
			nodes.add(org);
		}
	}
	
	private void createUserUnit(UserType unit) {
		List<NodeDto> list = new ArrayList<NodeDto>();
		for (NodeDto parent : getParentFromOrgRef(unit.getOrgRef())) {
			// TODO: what if user has more that one orgRef ????
			
			new NodeDto(parent, unit.getFullName().toString(), unit.getOid(), parent.getType());
		}
	}

	/**
	 * @param intermediate
	 *            are intermediate children allowed.
	 */
	public OrgStructProvider(boolean intermediate) {
		this.intermediate = intermediate;
	}

	/**
	 * Nothing to do.
	 */
	public void detach() {
	}

	public Iterator<NodeDto> getRoots() {
		return roots.iterator();
	}

	public boolean hasChildren(NodeDto node) {
		return node.getParent() == null || !node.getNodes().isEmpty();
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

	private List<NodeDto> getParentFromOrgRef(List<ObjectReferenceType> orgRefList) {
		List<NodeDto> list = new ArrayList<NodeDto>();
		for (ObjectReferenceType orgRef : orgRefList) {
			if (root.getOid().equals(orgRef.getOid())) {
				list.add(root);
				continue;
			}
			for (NodeDto node : nodes) {
				if (node.getOid().equals(orgRef.getOid())) {
					NodeType type = getRelation(orgRef);
					if(type != null) {
						//node.setType(type);
						list.add(node);
					} else {
						list.add(node);
					}
				}
			}
		}
		return list;
	}
	
	private NodeType getRelation(ObjectReferenceType orgRef) {
		if(orgRef.getRelation() == null) {
			return null;
		}
		String relation = orgRef.getRelation().getLocalPart();
		
		if(relation.equals("manager")) {
			return NodeType.BOSS;
		} else if(relation.equals("member")) {
			return NodeType.MANAGER;
		} else {
			return NodeType.USER;
		}
	}

	/**
	 * We're cheating here - the given runnable is run immediately.
	 */
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

	/**
	 * Creates a {@link NodeModel}.
	 */
	public IModel<NodeDto> model(NodeDto node) {
		return new NodeModel(node);
	}

	/**
	 * Get a {@link NodeDto} by its id.
	 */
	public NodeDto get(String id) {
		return get(roots, id);
	}

	private NodeDto get(List<NodeDto> nodes, String id) {
		for (NodeDto node : nodes) {
			if (node.getId().equals(id)) {
				return node;
			}

			NodeDto temp = get(node.getNodes(), id);
			if (temp != null) {
				return temp;
			}
		}

		return null;
	}

	/**
	 * A {@link Model} which uses an id to load its {@link NodeDto}.
	 * 
	 * If {@link NodeDto}s were {@link Serializable} you could just use a
	 * standard {@link Model}.
	 * 
	 * @see #equals(Object)
	 * @see #hashCode()
	 */
	private class NodeModel extends LoadableDetachableModel<NodeDto> {
		private static final long serialVersionUID = 1L;

		private String id;

		public NodeModel(NodeDto node) {
			super(node);

			id = node.getId();
		}

		@Override
		protected NodeDto load() {
			return get(id);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NodeModel) {
				return ((NodeModel) obj).id.equals(this.id);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}
}

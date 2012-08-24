/*
 * Copyright 2009 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * A provider of {@link NodeDto}s.
 * 
 * For simplicity all foos are kept as class members, in a real world scenario
 * these would be fetched from a database. If {@link NodeDto}s were
 * {@link Serializable} you could of course just keep references in instance
 * variables.
 * 
 * @see #model(NodeDto)
 * 
 * @author Sven Meier
 */
public class NodeProvider implements ITreeProvider<NodeDto> {

	private static final long serialVersionUID = 1L;

	/**
	 * All root {@link NodeDto}s.
	 */
	private static List<NodeDto> roots = new ArrayList<NodeDto>();

	/**
	 * Initialize roots.
	 */
	static {
		NodeDto root = new NodeDto("Root Dir");
		{
			NodeDto sub1 = new NodeDto(root, "Subdir 1");
			{
				new NodeDto(sub1, "Janko Hrasko (joslcarl)", NodeType.BOSS);
				new NodeDto(sub1, "Janko Hrasko (joslcarl)");
			}
			NodeDto sub2 = new NodeDto(root, "Subdir 2");
			{
				new NodeDto(sub2, "ABA");
				NodeDto sub2_1 = new NodeDto(sub2, "Subdir 2_1");
				{
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)", NodeType.BOSS);
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)", NodeType.MANAGER);
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)", NodeType.MANAGER);
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)", NodeType.MANAGER);
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)");
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)");
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)");
					new NodeDto(sub2_1, "Josua L. Carlton (joslcarl)");
				}
				
				NodeDto sub2_2 = new NodeDto(sub2, "Subdir 2_2");
				{
					NodeDto sub2_2_1 = new NodeDto(sub2_2, "Subdir 2_2_1");
					{
						new NodeDto(sub2_2_1, "Jozko Mrkvicka (janci)", NodeType.BOSS);
						new NodeDto(sub2_2_1, "Jozko Mrkvicka (janci)", NodeType.MANAGER);
						new NodeDto(sub2_2_1, "Jozko Mrkvicka (janci)", NodeType.MANAGER);
					}
					new NodeDto(sub2_2, "Jana Janova (jana)", NodeType.BOSS);
					new NodeDto(sub2_2, "Jana Janova (jana)", NodeType.MANAGER);
					new NodeDto(sub2_2, "Jana Janova (jana)");
					new NodeDto(sub2_2, "Jana Janova (jana)");
				}
				new NodeDto(sub2, "Lajci Dlhy (jDlhy)", NodeType.BOSS);
				new NodeDto(sub2, "Lajci Dlhy (jDlhy)");
			}

		}
		roots.add(root);
	}

	private boolean intermediate;

	public NodeProvider() {
		this(false);
	}

	/**
	 * @param intermediate
	 *            are intermediate children allowed.
	 */
	public NodeProvider(boolean intermediate) {
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

	public boolean hasChildren(NodeDto foo) {
		return foo.getParent() == null || !foo.getFoos().isEmpty();
	}

	public Iterator<NodeDto> getChildren(final NodeDto foo) {
		if (intermediate) {
			if (!foo.isLoaded()) {
				asynchronuous(new Runnable() {
					public void run() {
						foo.setLoaded(true);
					}
				});

				// mark children intermediate
				return IntermediateTreeProvider.intermediate(Collections.<NodeDto> emptyList().iterator());
			}
		}

		return foo.getFoos().iterator();
	}

	/**
	 * We're cheating here - the given runnable is run immediately.
	 */
	private void asynchronuous(Runnable runnable) {
		runnable.run();
	}

	public static void resetLoaded() {
		for (NodeDto foo : roots) {
			resetLoaded(foo);
		}
	}

	private static void resetLoaded(NodeDto foo) {
		foo.setLoaded(false);

		for (NodeDto child : foo.getFoos()) {
			resetLoaded(child);
		}
	}

	/**
	 * Creates a {@link FooModel}.
	 */
	public IModel<NodeDto> model(NodeDto foo) {
		return new FooModel(foo);
	}

	/**
	 * Get a {@link NodeDto} by its id.
	 */
	public static NodeDto get(String id) {
		return get(roots, id);
	}

	private static NodeDto get(List<NodeDto> foos, String id) {
		for (NodeDto foo : foos) {
			if (foo.getId().equals(id)) {
				return foo;
			}

			NodeDto temp = get(foo.getFoos(), id);
			if (temp != null) {
				return temp;
			}
		}

		return null;
	}

	/**
	 * A {@link Model} which uses an id to load its {@link NodeDto}.
	 * 
	 * If {@link NodeDto}s were {@link Serializable} you could just use a standard
	 * {@link Model}.
	 * 
	 * @see #equals(Object)
	 * @see #hashCode()
	 */
	private static class FooModel extends LoadableDetachableModel<NodeDto> {
		private static final long serialVersionUID = 1L;

		private String id;

		public FooModel(NodeDto foo) {
			super(foo);

			id = foo.getId();
		}

		@Override
		protected NodeDto load() {
			return get(id);
		}

		/**
		 * Important! Models must be identifyable by their contained object.
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof FooModel) {
				return ((FooModel) obj).id.equals(this.id);
			}
			return false;
		}

		/**
		 * Important! Models must be identifyable by their contained object.
		 */
		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}
}

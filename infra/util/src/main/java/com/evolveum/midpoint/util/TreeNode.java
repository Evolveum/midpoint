/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class TreeNode<T> implements DebugDumpable {

	private final List<TreeNode<T>> children = new ArrayList<>();
	private TreeNode<T> parent;
	private T userObject;

	public TreeNode() {
	}

	public TreeNode(T o) {
		userObject = o;
	}

	public List<TreeNode<T>> getChildren() {
		return children;
	}

	public TreeNode<T> getParent() {
		return parent;
	}

	public void add(TreeNode<T> newChild) {
		if (children.contains(newChild)) {
			return;
		}
		if (newChild.parent != null) {
			newChild.parent.remove(newChild);
		}
		children.add(newChild);
		newChild.parent = this;
	}

	private void remove(TreeNode<T> child) {
		children.remove(child);
		child.parent = null;
	}

	public T getUserObject() {
		return userObject;
	}

	public void setUserObject(T userObject) {
		this.userObject = userObject;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(DebugUtil.debugDump(userObject, indent));
		for (TreeNode<T> child : children) {
			sb.append("\n");
			sb.append(child.debugDump(indent + 1));
		}
		return sb.toString();
	}
}

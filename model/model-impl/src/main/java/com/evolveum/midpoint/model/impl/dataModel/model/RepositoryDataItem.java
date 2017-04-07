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

package com.evolveum.midpoint.model.impl.dataModel.model;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class RepositoryDataItem extends DataItem {

	@NotNull protected final QName typeName;
	@NotNull protected final ItemPath itemPath;

	private PrismObjectDefinition<?> objectDefinition;

	public RepositoryDataItem(@NotNull QName typeName, @NotNull ItemPath itemPath) {
		this.typeName = typeName;
		if (itemPath.isEmpty()) {
			throw new IllegalArgumentException("Empty item path");
		}
		this.itemPath = itemPath;
	}

	@NotNull
	public QName getTypeName() {
		return typeName;
	}

	@NotNull
	public ItemPath getItemPath() {
		return itemPath;
	}

	public PrismObjectDefinition<?> getObjectDefinition() {
		return objectDefinition;
	}

	public void setObjectDefinition(PrismObjectDefinition<?> objectDefinition) {
		this.objectDefinition = objectDefinition;
	}

	public boolean matches(@NotNull QName typeName, @NotNull ItemPath path) {
		return QNameUtil.match(this.typeName, typeName) && itemPath.equivalent(path);
	}

}

/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author mederly
 */
@Ignore
@Entity
//@IdClass(ROExtStringId.class)
@Table(name = "m_ext_item")
public class RExtItem {

	private Integer id;
	//	private boolean dynamic;
	private String name;
	private String type;
	private RItemKind kind;

	public Key toKey() {
		return new Key(name, type, kind);
	}

	public static class Key {
		public final String name;
		public final String type;
		public final RItemKind kind;

		public Key(String name, String type, RItemKind kind) {
			this.name = name;
			this.type = type;
			this.kind = kind;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Key))
				return false;
			Key key = (Key) o;
			return Objects.equals(name, key.name) &&
					Objects.equals(type, key.type) &&
					kind == key.kind;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, type, kind);
		}

		@Override
		public String toString() {
			return "Key{" +
					"name='" + name + '\'' +
					", type='" + type + '\'' +
					", kind=" + kind +
					'}';
		}
	}

	// required by hibernate
	@SuppressWarnings("unused")
	public RExtItem() {
	}

	private RExtItem(Key key) {
		this.name = key.name;
		this.type = key.type;
		this.kind = key.kind;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}

	@Column(name = "itemName", length = RUtil.COLUMN_LENGTH_QNAME)
	public String getName() {
		return name;
	}

	@Column(name = "itemType", length = RUtil.COLUMN_LENGTH_QNAME)        // to avoid collisions with reserved words
	public String getType() {
		return type;
	}

	@Enumerated(EnumType.ORDINAL)
	public RItemKind getKind() {
		return kind;
	}

	//	/**
	//	 * @return true if this property has dynamic definition
	//	 */
	//	@Column(name = "dynamicDef")
	//	public boolean isDynamic() {
	//		return dynamic;
	//	}

	public void setId(int id) {
		this.id = id;
	}

	public void setKind(RItemKind kind) {
		this.kind = kind;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static RExtItem.Key createKeyFromDefinition(ItemDefinition<?> definition) {
		String name = RUtil.qnameToString(definition.getName());
		String type = RUtil.qnameToString(definition.getTypeName());
		RItemKind kind = RItemKind.getTypeFromItemDefinitionClass(definition.getClass());
		return new Key(name, type, kind);
	}

	public static RExtItem createFromDefinition(ItemDefinition<?> definition) {
		return new RExtItem(createKeyFromDefinition(definition));
	}

	@Override
	public String toString() {
		return "RExtItem{" +
				"id=" + id +
				", name='" + name + '\'' +
				", type='" + type + '\'' +
				", kind=" + kind +
				'}';
	}

	//	public void setDynamic(boolean dynamic) {
	//		this.dynamic = dynamic;
	//	}
}

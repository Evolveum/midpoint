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

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtLongId;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author lazyman
 */
@Entity
//@IdClass(ROExtLongId.class)
@Table(name = "m_object_ext_long",
        indexes = { // TODO indices
                @Index(name = "iExtensionLong", columnList = "ownerType, item_id, longValue"),
                @Index(name = "iExtensionLongDef", columnList = "owner_oid, ownerType")})
public class ROExtLong extends ROExtBase implements ROExtValue {

    private Long value;

    public ROExtLong() {
    }

    public ROExtLong(Long value) {
        this.value = value;
    }

	@Id
	@MapsId("owner")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(foreignKey = @ForeignKey(name = "fk_object_ext_long_owner"))
	@NotQueryable
	public RObject getOwner() {
		return super.getOwner();
	}

	//@Id
//	@Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
//	public String getOwnerOid() {
//		return super.getOwnerOid();
//	}

	@Id
	@Column(name = "ownerType")
	@Enumerated(EnumType.ORDINAL)
	public RObjectExtensionType getOwnerType() {
		return super.getOwnerType();
	}

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(foreignKey = @ForeignKey(name = "fk_object_ext_long_item"))
	public RExtItem getItem() {
		return super.getItem();
	}

//	@Id
//	@Column(name = "item_id", insertable = false, updatable = false)
//	public Long getItemId() {
//		return super.getItemId();
//	}

//	@Override
//	public void setItemId(Long itemId) {
//		super.setItemId(itemId);
//	}

	@Id
	@Column(name = "longValue")
    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ROExtLong))
			return false;
		if (!super.equals(o))
			return false;
		ROExtLong roExtLong = (ROExtLong) o;
		return Objects.equals(value, roExtLong.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), value);
	}
}

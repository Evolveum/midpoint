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
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtBooleanId;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.MidPointIdProvidingSingleTableEntityPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author lazyman
 */
@Entity
//@IdClass(ROExtBooleanId.class)
@Table(name = "m_object_ext_boolean",
        indexes = { // TODO indices
		        @Index(name = "iExtensionBoolean", columnList = "ownerType, item_id, booleanValue"),
		        @Index(name = "iExtensionBooleanDef", columnList = "owner_oid, ownerType")})
@Persister(impl = MidPointIdProvidingSingleTableEntityPersister.class)
public class ROExtBoolean extends ROExtBase implements ROExtValue {

    private Boolean value;

    public ROExtBoolean() {
    }

    public ROExtBoolean(Boolean value) {
        this.value = value;
    }

	@Id
//	@MapsId("owner")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(foreignKey = @ForeignKey(name = "fk_object_ext_boolean_owner"))
	@NotQueryable
	public RObject getOwner() {
		return super.getOwner();
	}

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
	@JoinColumn(foreignKey = @ForeignKey(name = "fk_object_ext_boolean_item"))
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
	@Column(name = "booleanValue")
    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ROExtBoolean))
			return false;
		if (!super.equals(o))
			return false;
		ROExtBoolean that = (ROExtBoolean) o;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), value);
	}
}

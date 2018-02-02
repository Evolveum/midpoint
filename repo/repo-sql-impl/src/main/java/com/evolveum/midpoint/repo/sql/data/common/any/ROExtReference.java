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

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.MidPointIdProvidingSingleTableEntityPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author lazyman
 */
@Entity
//@IdClass(ROExtReferenceId.class)
@Table(name = "m_object_ext_reference",
        indexes = {
                @Index(name = "iExtensionReference", columnList = "ownerType, item_id, targetoid"),
                @Index(name = "iExtensionReferenceDef", columnList = "owner_oid, ownerType")})
@Persister(impl = MidPointIdProvidingSingleTableEntityPersister.class)
public class ROExtReference extends ROExtBase implements ROExtValue {

    public static final String F_TARGET_OID = "value";
    public static final String F_RELATION = "relation";
    public static final String F_TARGET_TYPE = "targetType";

    //this is target oid
    private String value;
    //this is type attribute
    private RObjectType targetType;
    private String relation;

    public ROExtReference() {
    }

    @Id
//    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_object_ext_reference_owner"))
    @NotQueryable
    public RObject getOwner() {
        return super.getOwner();
    }

//    @Id
//    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
//    public String getOwnerOid() {
//        return super.getOwnerOid();
//    }

    @Id
    @Column(name = "ownerType")
    @Enumerated(EnumType.ORDINAL)
    public RObjectExtensionType getOwnerType() {
        return super.getOwnerType();
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_object_ext_reference_item"))
    public RExtItem getItem() {
        return super.getItem();
    }

//    @Id
//    @Column(name = "item_id", insertable = false, updatable = false)
//    public Long getItemId() {
//        return super.getItemId();
//    }
//
//	@Override
//	public void setItemId(Long itemId) {
//		super.setItemId(itemId);
//	}

	@Id
	@Column(name = "targetoid", length = RUtil.COLUMN_LENGTH_OID)
    public String getValue() {
        return value;
    }

    @Enumerated(EnumType.ORDINAL)
    public RObjectType getTargetType() {
        return targetType;
    }

    @Id
    @Column(name = "relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return relation;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setTargetType(RObjectType targetType) {
        this.targetType = targetType;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public static ROExtReference createReference(PrismReferenceValue jaxb) {
        ROExtReference repo = new ROExtReference();
        repo.setValue(jaxb.getOid());
        repo.setRelation(RUtil.qnameToString(jaxb.getRelation()));
        repo.setTargetType(ClassMapper.getHQLTypeForQName(jaxb.getTargetType()));
        return repo;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ROExtReference))
			return false;
		if (!super.equals(o))
			return false;
		ROExtReference that = (ROExtReference) o;
		return Objects.equals(value, that.value) &&
				targetType == that.targetType &&
				Objects.equals(relation, that.relation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), value, targetType, relation);
	}
}

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
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(ROExtReferenceId.class)
@Table(name = "m_object_ext_reference", indexes = {
        @Index(name = "iExtensionReference", columnList = "targetoid")
})
public class ROExtReference extends ROExtBase {

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

    @Override
    @Id
    @ForeignKey(name = "fk_o_ext_reference_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RObject getOwner() {
        return super.getOwner();
    }

    @Override
    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Override
    @Id
    @Column(name = "ownerType")
    @Enumerated(EnumType.ORDINAL)
    public RObjectExtensionType getOwnerType() {
        return super.getOwnerType();
    }

    @Override
    @MapsId("item")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @javax.persistence.ForeignKey(name = "fk_o_ext_reference_item"))
    public RExtItem getItem() {
        return super.getItem();
    }

    @Override
    @Id
    @Column(name = "item_id", insertable = false, updatable = false)
    public Integer getItemId() {
        return super.getItemId();
    }

    @Override
    @Column(name = "targetoid", length = RUtil.COLUMN_LENGTH_OID)
    public String getValue() {
        return value;
    }

    @Enumerated(EnumType.ORDINAL)
    public RObjectType getTargetType() {
        return targetType;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ROExtReference))
            return false;
        if (!super.equals(o))
            return false;
        ROExtReference that = (ROExtReference) o;
        return Objects.equals(value, that.value);
    }

    public static PrismReferenceValue createReference(ROExtReference repo) {
        PrismReferenceValue value = new PrismReferenceValue();
        value.setOid(repo.getValue());
        value.setRelation(RUtil.stringToQName(repo.getRelation()));
        value.setTargetType(ClassMapper.getQNameForHQLType(repo.getTargetType()));

        return value;
    }

    public static ROExtReference createReference(PrismReferenceValue jaxb) {
        ROExtReference repo = new ROExtReference();

        repo.setValue(jaxb.getOid());
        repo.setRelation(RUtil.qnameToString(jaxb.getRelation()));
        repo.setTargetType(ClassMapper.getHQLTypeForQName(jaxb.getTargetType()));

        return repo;
    }
}

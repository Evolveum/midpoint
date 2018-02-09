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
import com.evolveum.midpoint.repo.sql.data.common.id.RAExtReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RAExtReferenceId.class)
@Table(name = "m_assignment_ext_reference")
@org.hibernate.annotations.Table(appliesTo = "m_assignment_ext_reference"
        /*,
        indexes = {@Index(name = "iAExtensionReference", columnNames = {"extensionType", "eName", "targetoid"})} */)
public class RAExtReference extends RAExtBase<String> implements RAExtValue<String> {

    //this is target oid
    private String value;
    //this is type attribute
    private RObjectType targetType;
    private String relation;

    public RAExtReference() {
    }

    @ForeignKey(name = "fk_a_ext_reference_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_owner_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_id", referencedColumnName = "owner_type")
    })
    @NotQueryable
    public RAssignmentExtension getAnyContainer() {
        return super.getAnyContainer();
    }

    @Id
    @Column(name = "anyContainer_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Id
    @Column(name = "anyContainer_owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        return super.getOwnerId();
    }

    @Id
    @Column(name = "item_id", updatable = false, insertable = false)
    public Integer getItemId() {
        return super.getItemId();
    }

    @MapsId("item")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @javax.persistence.ForeignKey(name = "fk_a_ext_boolean_reference"))
    public RExtItem getItem() {
        return super.getItem();
    }

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

    public static PrismReferenceValue createReference(RAExtReference repo) {
        PrismReferenceValue value = new PrismReferenceValue();
        value.setOid(repo.getValue());
        value.setRelation(RUtil.stringToQName(repo.getRelation()));
        value.setTargetType(ClassMapper.getQNameForHQLType(repo.getTargetType()));

        return value;
    }

    public static RAExtReference createReference(PrismReferenceValue jaxb) {
        RAExtReference repo = new RAExtReference();

        repo.setValue(jaxb.getOid());
        repo.setRelation(RUtil.qnameToString(jaxb.getRelation()));
        repo.setTargetType(ClassMapper.getHQLTypeForQName(jaxb.getTargetType()));

        return repo;
    }
}

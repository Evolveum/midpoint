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

import com.evolveum.midpoint.repo.sql.data.common.id.RAExtStringId;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
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
//@IdClass(RAExtStringId.class)
@Table(name = "m_assignment_ext_string",
        indexes = {@Index(name = "iAExtensionString", columnList = "item_id, stringValue")})
@Persister(impl = MidPointIdProvidingSingleTableEntityPersister.class)
public class RAExtString extends RAExtBase<String> implements RAExtValue<String> {

    private String value;

    public RAExtString() {
    }

    public RAExtString(String value) {
        this.value = value;
    }

    @Id
    //@MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(value = {
            @JoinColumn(name = "anyContainer_owner_owner_oid", referencedColumnName = "owner_owner_oid"),
            @JoinColumn(name = "anyContainer_owner_id", referencedColumnName = "owner_id")},
            foreignKey = @ForeignKey(name = "fk_a_ext_string_owner"))
    @NotQueryable
    public RAssignmentExtension getAnyContainer() {
        return super.getAnyContainer();
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_a_ext_string_item"))
    public RExtItem getItem() {
        return super.getItem();
    }

//    @Id
//    @Column(name = "anyContainer_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
//    @NotQueryable
//    public String getOwnerOid() {
//        return super.getOwnerOid();
//    }
//
//    @Id
//    @Column(name = "anyContainer_owner_id")
//    @NotQueryable
//    public Integer getOwnerId() {
//        return super.getOwnerId();
//    }
//
//    @Id
//    @Column(name = "item_id", insertable = false, updatable = false)
//    public Long getItemId() {
//        return super.getItemId();
//    }
//
//    @Override
//    public void setItemId(Long itemId) {
//        super.setItemId(itemId);
//    }

    @Id
    @Column(name = "stringValue")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RAExtString))
            return false;
        if (!super.equals(o))
            return false;
        RAExtString that = (RAExtString) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}

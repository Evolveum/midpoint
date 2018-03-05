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

import com.evolveum.midpoint.repo.sql.data.common.id.RAExtLongId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RAExtLongId.class)
@Table(name = "m_assignment_ext_long", indexes = {
        @Index(name = "iAExtensionLong", columnList = "longValue")
})
public class RAExtLong extends RAExtBase<Long> implements RAExtValue<Long> {

    private Long value;

    public RAExtLong() {
    }

    public RAExtLong(Long value) {
        this.value = value;
    }

    @Override
    @ForeignKey(name = "fk_a_ext_long_owner")
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

    @Override
    @Id
    @Column(name = "anyContainer_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Override
    @Id
    @Column(name = "anyContainer_owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        return super.getOwnerId();
    }

    @Override
    @Id
    @Column(name = "item_id", updatable = false, insertable = false)
    public Integer getItemId() {
        return super.getItemId();
    }

    @Override
    @MapsId("item")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(foreignKey = @javax.persistence.ForeignKey(name = "fk_a_ext_long_item"))
    public RExtItem getItem() {
        return super.getItem();
    }

    @Override
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
        if (!(o instanceof RAExtLong))
            return false;
        if (!super.equals(o))
            return false;
        RAExtLong that = (RAExtLong) o;
        return Objects.equals(value, that.value);
    }
}

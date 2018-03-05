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
@IdClass(ROExtBooleanId.class)
@Table(name = "m_object_ext_boolean", indexes = {
        @Index(name = "iExtensionBoolean", columnList = "booleanValue")
})
public class ROExtBoolean extends ROExtBase {

    private Boolean value;

    public ROExtBoolean() {
    }

    public ROExtBoolean(Boolean value) {
        this.value = value;
    }

    @Override
    @Id
    @ForeignKey(name = "fk_o_ext_boolean_owner")
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
    @JoinColumn(foreignKey = @javax.persistence.ForeignKey(name = "fk_o_ext_boolean_item"))
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
}

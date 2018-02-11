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

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtPolyStringId;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(ROExtPolyStringId.class)
@Table(name = "m_object_ext_poly")
@org.hibernate.annotations.Table(appliesTo = "m_object_ext_poly"
        /*,
        indexes = {@Index(name = "iExtensionPolyString", columnNames = {"ownerType", "eName", "orig"}),
                @Index(name = "iExtensionPolyStringDef", columnNames = {"owner_oid", "ownerType"})} */)
public class ROExtPolyString extends ROExtBase {

    //orig value
    private String value;
    private String norm;

    public ROExtPolyString() {
        this(null);
    }

    public ROExtPolyString(PolyString polyString) {
        if (polyString != null) {
            value = polyString.getOrig();
            norm = polyString.getNorm();
        }
    }

    @Id
    @ForeignKey(name = "fk_o_ext_poly_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RObject getOwner() {
        return super.getOwner();
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Id
    @Column(name = "ownerType")
    @Enumerated(EnumType.ORDINAL)
    public RObjectExtensionType getOwnerType() {
        return super.getOwnerType();
    }

    @MapsId("item")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @javax.persistence.ForeignKey(name = "fk_o_ext_poly_item"))
    public RExtItem getItem() {
        return super.getItem();
    }

    @Id
    @Column(name = "item_id", insertable = false, updatable = false)
    public Integer getItemId() {
        return super.getItemId();
    }

    @Column(name = "orig")
    public String getValue() {
        return value;
    }

    public String getNorm() {
        return norm;
    }

    public void setNorm(String norm) {
        this.norm = norm;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ROExtPolyString))
            return false;
        if (!super.equals(o))
            return false;
        ROExtPolyString that = (ROExtPolyString) o;
        return Objects.equals(value, that.value);
    }
}

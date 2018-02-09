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
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtDateId;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(ROExtDateId.class)
@Table(name = "m_object_ext_date")
@org.hibernate.annotations.Table(appliesTo = "m_object_ext_date"
		/*,
        indexes = {@Index(name = "iExtensionDate", columnNames = {"ownerType", "eName", "dateValue"}),
                @Index(name = "iExtensionDateDef", columnNames = {"owner_oid", "ownerType"})} */)
public class ROExtDate extends ROExtBase {

    private Timestamp value;

    public ROExtDate() {
    }

    public ROExtDate(Timestamp value) {
        this.value = value;
    }

    @Id
    @ForeignKey(name = "fk_o_ext_date_owner")
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
    @JoinColumn(foreignKey = @javax.persistence.ForeignKey(name = "fk_o_ext_date_item"))
    public RExtItem getItem() {
        return super.getItem();
    }

    @Id
    @Column(name = "item_id", insertable = false, updatable = false)
    public Integer getItemId() {
        return super.getItemId();
    }

    @Column(name = "dateValue")
    public Timestamp getValue() {
        return value;
    }

    public void setValue(Timestamp value) {
        this.value = value;
    }
}

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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo.COLUMN_OWNER_OID;
import static com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo.TABLE_NAME;

/**
 * @author mederly
 */
@Entity
@Table(name = TABLE_NAME, indexes = {
        @Index(name = "iTextInfoOid", columnList = COLUMN_OWNER_OID)})
public class RObjectTextInfo implements Serializable {

    public static final String TABLE_NAME = "m_object_text_info";
    public static final String COLUMN_OWNER_OID = "owner_oid";

    private Integer id;

    private RObject owner;
    private String ownerOid;

    private String text;

    public RObjectTextInfo() {
    }

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ForeignKey(name = "fk_reference_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RObject getOwner() {
        return owner;
    }

    @Id
    @Column(name = COLUMN_OWNER_OID, length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RObjectTextInfo that = (RObjectTextInfo) o;
        return Objects.equals(owner, that.owner) &&
                Objects.equals(ownerOid, that.ownerOid) &&
                Objects.equals(id, that.id) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, ownerOid, id, text);
    }

    public static <T extends ObjectType> Set<RObjectTextInfo> createSet(@NotNull ObjectType object, @NotNull RObject repo) {
        Set<RObjectTextInfo> rv = new HashSet<>();
        if (object.getName() != null) {
            rv.add(create(repo, object.getName().getOrig()));
        }
        if (object.getDescription() != null) {
            rv.add(create(repo, object.getDescription()));
        }
        return rv;
    }

    private static RObjectTextInfo create(RObject repo, String text) {
        RObjectTextInfo rv = new RObjectTextInfo();
        rv.setOwner(repo);
        rv.setText(text);
        return rv;
    }
}

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

import com.evolveum.midpoint.repo.sql.data.common.id.ROrgClosureId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Ignore
@IdClass(ROrgClosureId.class)
@Entity
@Table(name = "m_org_closure")
@org.hibernate.annotations.Table(appliesTo = "m_org_closure",
        indexes = {@Index(name = "iDescendant", columnNames = {"descendant_oid"}),
                   @Index(name = "iDescendantAncestor", columnNames = {"descendant_oid", "ancestor_oid"})})
@NotQueryable
public class ROrgClosure implements Serializable {

    private RObject ancestor;
    private String ancestorOid;

    private RObject descendant;
    private String descendantOid;

    private int val;

    public ROrgClosure() {
    }

    public ROrgClosure(String ancestorOid, String descendantOid, int val) {
        if (ancestorOid != null) {
            this.ancestorOid = ancestorOid;
        }
        if (descendantOid != null) {
            this.descendantOid = descendantOid;
        }
        this.val = val;
    }

    public ROrgClosure(RObject ancestor, RObject descendant, int val) {
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.val = val;
    }

    @MapsId("ancestorOid")
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns({@JoinColumn(name = "ancestor_oid", referencedColumnName = "oid")})
    @ForeignKey(name = "fk_ancestor")
    @NotQueryable
    public RObject getAncestor() {
        return ancestor;
    }

    @Id
    @Index(name = "iAncestor")
    @Column(name = "ancestor_oid", length = RUtil.COLUMN_LENGTH_OID, insertable = false, updatable = false)
    @NotQueryable
    public String getAncestorOid() {
        if (ancestorOid == null && ancestor.getOid() != null) {
            ancestorOid = ancestor.getOid();
        }
        return ancestorOid;
    }

    public void setAncestor(RObject ancestor) {
        this.ancestor = ancestor;
    }

    @MapsId("descendantOid")
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns({@JoinColumn(name = "descendant_oid", referencedColumnName = "oid")})
    @ForeignKey(name = "fk_descendant")
    @NotQueryable
    public RObject getDescendant() {
        return descendant;
    }

    @Id
    @Index(name = "iDescendant")
    @Column(name = "descendant_oid", length = RUtil.COLUMN_LENGTH_OID, insertable = false, updatable = false)
    @NotQueryable
    public String getDescendantOid() {
        if (descendantOid == null && descendant.getOid() != null) {
            descendantOid = descendant.getOid();
        }
        return descendantOid;
    }

    public void setDescendant(RObject descendant) {
        this.descendant = descendant;
    }

    public void setAncestorOid(String ancestorOid) {
        this.ancestorOid = ancestorOid;
    }

    public void setDescendantOid(String descendantOid) {
        this.descendantOid = descendantOid;
    }

    @Id
    @Column(name = "val")
    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        ROrgClosure that = (ROrgClosure) obj;

        if (ancestor != null ? !ancestor.equals(that.ancestor) : that.ancestor != null)
            return false;
        if (descendant != null ? !descendant.equals(that.descendant) : that.descendant != null)
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "ROrgClosure{a='" + ancestorOid + "', d='" + descendantOid + "', val=" + val + "}";
    }
}

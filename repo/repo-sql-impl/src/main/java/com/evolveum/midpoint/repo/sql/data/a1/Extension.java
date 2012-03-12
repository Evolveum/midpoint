/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.a1;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 7:12 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Extension implements Serializable {

    private RType ownerType;

    private Container owner;
    private String ownerOid;
    private Long ownerId;

    private Set<StringValue> strings;
    private Set<LongValue> longs;
    private Set<DateValue> dates;
    private Set<ClobValue> clobs;

    @ForeignKey(name = "fk_reference_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public Container getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @ElementCollection
    @ForeignKey(name = "fk_extension_long")
    @CollectionTable(name = "extension_long", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<LongValue> getLongs() {
        return longs;
    }

    @ElementCollection
    @ForeignKey(name = "fk_extension_string")
    @CollectionTable(name = "extension_string", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<StringValue> getStrings() {
        return strings;
    }

    @ElementCollection
    @ForeignKey(name = "fk_extension_clob")
    @CollectionTable(name = "extension_clob", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ClobValue> getClobs() {
        return clobs;
    }

    @ElementCollection
    @ForeignKey(name = "fk_extension_date")
    @CollectionTable(name = "extension_date", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<DateValue> getDates() {
        return dates;
    }

//    @Id
    @Enumerated(EnumType.ORDINAL)
    public RType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(RType ownerType) {
        this.ownerType = ownerType;
    }

    public void setClobs(Set<ClobValue> clobs) {
        this.clobs = clobs;
    }

    public void setDates(Set<DateValue> dates) {
        this.dates = dates;
    }

    public void setLongs(Set<LongValue> longs) {
        this.longs = longs;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setStrings(Set<StringValue> strings) {
        this.strings = strings;
    }

    public void setOwner(Container owner) {
        this.owner = owner;
    }
}

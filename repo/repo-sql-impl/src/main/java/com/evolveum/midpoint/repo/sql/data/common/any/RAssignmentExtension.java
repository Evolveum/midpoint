/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

import org.apache.commons.lang.Validate;

import javax.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
//@IdClass(RAssignmentExtensionId.class)
@Table(name = "m_assignment_extension")
public class RAssignmentExtension implements Serializable {

    private RAssignment owner;
//    private String ownerOwnerOid;
//    private Integer ownerId;

    private Short stringsCount;
    private Short longsCount;
    private Short datesCount;
    private Short referencesCount;
    private Short polysCount;
    private Short booleansCount;

    private Set<RAExtString> strings;
    private Set<RAExtLong> longs;
    private Set<RAExtDate> dates;
    private Set<RAExtReference> references;
    private Set<RAExtPolyString> polys;
    private Set<RAExtBoolean> booleans;

//    @Id
//    @Column(name = "owner_owner_oid")
//    public String getOwnerOwnerOid() {
//        return ownerOwnerOid;
//    }
//
//    public void setOwnerOwnerOid(String ownerOwnerOid) {
//        this.ownerOwnerOid = ownerOwnerOid;
//    }
//
//    @Id
//    @Column(name = "owner_id")
//    public Integer getOwnerId() {
//        return ownerId;
//    }
//
//    public void setOwnerId(Integer ownerId) {
//        this.ownerId = ownerId;
//    }

    @Id
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(value = {
            @JoinColumn(name = "owner_owner_oid", referencedColumnName = "owner_oid"),
            @JoinColumn(name = "owner_id", referencedColumnName = "id") },
            foreignKey = @javax.persistence.ForeignKey(name = "fk_assignment_extension_owner"))
    @NotQueryable
    public RAssignment getOwner() {
        return owner;
    }

    @Transient
    public String getAssignmentOwnerOid() {
        return owner != null ? owner.getOwnerOid() : null;
    }

    @Transient
    public Integer getAssignmentId() {
        return owner != null ? owner.getId() : null;
    }

//    @Id
//    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
//    public String getOwnerOid() {
//        if (ownerOid == null && owner != null) {
//            ownerOid = owner.getOwnerOid();
//        }
//        return ownerOid;
//    }
//
//    @Id
//    @Column(name = "owner_id", length = RUtil.COLUMN_LENGTH_OID)
//    public Integer getOwnerId() {
//        if (ownerId == null && owner != null) {
//            ownerId = owner.getId();
//        }
//        return ownerId;
//    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<RAExtBoolean> getBooleans() {
        if (booleans == null) {
            booleans = new HashSet<>();
        }
        return booleans;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<RAExtLong> getLongs() {
        if (longs == null) {
            longs = new HashSet<>();
        }
        return longs;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<RAExtString> getStrings() {
        if (strings == null) {
            strings = new HashSet<>();
        }
        return strings;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<RAExtDate> getDates() {
        if (dates == null) {
            dates = new HashSet<>();
        }
        return dates;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<RAExtReference> getReferences() {
        if (references == null) {
            references = new HashSet<>();
        }
        return references;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<RAExtPolyString> getPolys() {
        if (polys == null) {
            polys = new HashSet<>();
        }
        return polys;
    }

    public Short getBooleansCount() {
        return booleansCount;
    }

    public Short getStringsCount() {
        return stringsCount;
    }

    public Short getLongsCount() {
        return longsCount;
    }

    public Short getDatesCount() {
        return datesCount;
    }

    public Short getReferencesCount() {
        return referencesCount;
    }

    public Short getPolysCount() {
        return polysCount;
    }

    public void setStringsCount(Short stringsCount) {
        this.stringsCount = stringsCount;
    }

    public void setLongsCount(Short longsCount) {
        this.longsCount = longsCount;
    }

    public void setDatesCount(Short datesCount) {
        this.datesCount = datesCount;
    }

    public void setReferencesCount(Short referencesCount) {
        this.referencesCount = referencesCount;
    }

    public void setPolysCount(Short polysCount) {
        this.polysCount = polysCount;
    }

    public void setPolys(Set<RAExtPolyString> polys) {
        this.polys = polys;
    }

    public void setReferences(Set<RAExtReference> references) {
        this.references = references;
    }

    public void setDates(Set<RAExtDate> dates) {
        this.dates = dates;
    }

    public void setLongs(Set<RAExtLong> longs) {
        this.longs = longs;
    }

//    public void setOwnerOid(String ownerOid) {
//        this.ownerOid = ownerOid;
//    }

    public void setStrings(Set<RAExtString> strings) {
        this.strings = strings;
    }

    public void setOwner(RAssignment owner) {
        this.owner = owner;
    }

//    public void setOwnerId(Integer ownerId) {
//        this.ownerId = ownerId;
//    }

    public void setBooleans(Set<RAExtBoolean> booleans) {
        this.booleans = booleans;
    }

    public void setBooleansCount(Short booleansCount) {
        this.booleansCount = booleansCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RAssignmentExtension))
            return false;
        RAssignmentExtension that = (RAssignmentExtension) o;
        return //Objects.equals(getAssignmentOwnerOid(), that.getAssignmentOwnerOid()) &&
                //Objects.equals(getAssignmentId(), that.getAssignmentId()) &&
                Objects.equals(stringsCount, that.stringsCount) &&
                Objects.equals(longsCount, that.longsCount) &&
                Objects.equals(datesCount, that.datesCount) &&
                Objects.equals(referencesCount, that.referencesCount) &&
                Objects.equals(polysCount, that.polysCount) &&
                Objects.equals(booleansCount, that.booleansCount);// &&
//                Objects.equals(strings, that.strings) &&
//                Objects.equals(longs, that.longs) &&
//                Objects.equals(dates, that.dates) &&
//                Objects.equals(references, that.references) &&
//                Objects.equals(polys, that.polys) &&
//                Objects.equals(booleans, that.booleans);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(/*getAssignmentOwnerOid(), getAssignmentId(), */stringsCount, longsCount, datesCount, referencesCount,
                        polysCount, booleansCount);
    }

    public static void copyFromJAXB(ExtensionType jaxb, RAssignmentExtension repo, RAssignmentExtensionType type,
                                    RepositoryContext repositoryContext) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        copyFromJAXB(jaxb.asPrismContainerValue(), repo, type, repositoryContext);
    }

    private static void copyFromJAXB(PrismContainerValue containerValue, RAssignmentExtension repo,
                                     RAssignmentExtensionType type, RepositoryContext repositoryContext) throws
            DtoTranslationException {
        RAnyConverter converter = new RAnyConverter(repositoryContext.prismContext);

        Set<RAnyValue> values = new HashSet<>();
        try {
            List<Item<?,?>> items = containerValue.getItems();
            for (Item item : items) {
                values.addAll(converter.convertToRValue(item, true, null));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        for (RAnyValue value : values) {
            ((RAExtValue) value).setAnyContainer(repo);
            ((RAExtValue) value).setExtensionType(type);

            if (value instanceof RAExtDate) {
                repo.getDates().add((RAExtDate) value);
            } else if (value instanceof RAExtLong) {
                repo.getLongs().add((RAExtLong) value);
            } else if (value instanceof RAExtReference) {
                repo.getReferences().add((RAExtReference) value);
            } else if (value instanceof RAExtString) {
                repo.getStrings().add((RAExtString) value);
            } else if (value instanceof RAExtPolyString) {
                repo.getPolys().add((RAExtPolyString) value);
            } else if (value instanceof RAExtBoolean) {
                repo.getBooleans().add((RAExtBoolean) value);
            }
        }

        repo.setStringsCount((short) repo.getStrings().size());
        repo.setDatesCount((short) repo.getDates().size());
        repo.setPolysCount((short) repo.getPolys().size());
        repo.setReferencesCount((short) repo.getReferences().size());
        repo.setLongsCount((short) repo.getLongs().size());
        repo.setBooleansCount((short) repo.getBooleans().size());
    }

    @Override
    public String toString() {
        return "RAssignmentExtension{" +
//                "owner=" + (owner != null ? owner.getOwner() + ":" + owner.getId() : "null" ) +
//                ", strings=" + strings +
//                ", longs=" + longs +
//                ", dates=" + dates +
//                ", references=" + references +
//                ", polys=" + polys +
//                ", booleans=" + booleans +
                "strings#=" + stringsCount +
                ", longs#=" + longsCount +
                ", dates#=" + datesCount +
                ", references#=" + referencesCount +
                ", polys#=" + polysCount +
                ", booleans#=" + booleansCount +
                '}';
    }
}

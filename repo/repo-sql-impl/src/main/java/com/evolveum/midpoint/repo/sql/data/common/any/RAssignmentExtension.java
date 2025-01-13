/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.id.RAssignmentExtensionId;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RAssignmentExtensionId.class)
@Table(name = "m_assignment_extension")
public class RAssignmentExtension implements Serializable, EntityState {

    private Boolean trans;

    private RAssignment owner;
    private String ownerOid;
    private Integer ownerId;

    private Set<RAExtString> strings;
    private Set<RAExtLong> longs;
    private Set<RAExtDate> dates;
    private Set<RAExtReference> references;
    private Set<RAExtPolyString> polys;
    private Set<RAExtBoolean> booleans;

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumns(
            value = {
                    @JoinColumn(name = "owner_owner_oid", referencedColumnName = "owner_oid"),
                    @JoinColumn(name = "owner_id", referencedColumnName = "id")
            },
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    public RAssignment getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOwnerOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "owner_id", length = RUtil.COLUMN_LENGTH_OID)
    public Integer getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

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

    public void setOwnerOid(String ownerOid) {
            this.ownerOid = ownerOid;
    }

    public void setStrings(Set<RAExtString> strings) {
        this.strings = strings;
    }

    public void setOwner(RAssignment owner) {
        this.owner = owner;
//        if (owner != null && owner.getExtension() != this) {
//            owner.setExtension(this);
//        }
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public void setBooleans(Set<RAExtBoolean> booleans) {
        this.booleans = booleans;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        RAssignmentExtension that = (RAssignmentExtension) o;

        if (dates != null ? !dates.equals(that.dates) : that.dates != null) {return false;}
        if (longs != null ? !longs.equals(that.longs) : that.longs != null) {return false;}
        if (polys != null ? !polys.equals(that.polys) : that.polys != null) {return false;}
        if (references != null ? !references.equals(that.references) : that.references != null) {return false;}
        if (strings != null ? !strings.equals(that.strings) : that.strings != null) {return false;}
        if (booleans != null ? !booleans.equals(that.booleans) : that.booleans != null) {return false;}

        return true;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    public static void fromJaxb(ExtensionType jaxb, RAssignmentExtension repo, RAssignmentExtensionType type,
            RepositoryContext repositoryContext) throws DtoTranslationException {
        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");

        fromJaxb(jaxb.asPrismContainerValue(), repo, type, repositoryContext);
    }

    private static void fromJaxb(PrismContainerValue<?> containerValue, RAssignmentExtension repo,
            RAssignmentExtensionType type, RepositoryContext repositoryContext) throws
            DtoTranslationException {
        RAnyConverter converter = new RAnyConverter(repositoryContext.prismContext, repositoryContext.extItemDictionary);

        Set<RAnyValue> values = new HashSet<>();
        try {
            for (Item item : containerValue.getItems()) {
                values.addAll(converter.convertToRValue(item, true, RObjectExtensionType.EXTENSION));
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
    }

    @Override
    public String toString() {
        return "RAssignmentExtension{" +
                '}';
    }
}

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_org")
public class ROrg extends RRole {

    @QueryAttribute(polyString = true)
    private RPolyString displayName;
    private String identifier;
    private Set<String> orgType;
    private String costCenter;
    @QueryAttribute(polyString = true)
    private RPolyString locality;

    public String getCostCenter() {
        return costCenter;
    }

    @Embedded
    public RPolyString getDisplayName() {
        return displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Embedded
    public RPolyString getLocality() {
        return locality;
    }

    @ElementCollection
    @ForeignKey(name = "fk_org_org_type")
    @CollectionTable(name = "m_org_org_type", joinColumns = {
            @JoinColumn(name = "org_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "org_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getOrgType() {
        return orgType;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public void setDisplayName(RPolyString displayName) {
        this.displayName = displayName;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setLocality(RPolyString locality) {
        this.locality = locality;
    }

    public void setOrgType(Set<String> orgType) {
        this.orgType = orgType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ROrg rOrg = (ROrg) o;

        if (costCenter != null ? !costCenter.equals(rOrg.costCenter) : rOrg.costCenter != null) return false;
        if (displayName != null ? !displayName.equals(rOrg.displayName) : rOrg.displayName != null) return false;
        if (identifier != null ? !identifier.equals(rOrg.identifier) : rOrg.identifier != null) return false;
        if (locality != null ? !locality.equals(rOrg.locality) : rOrg.locality != null) return false;
        if (orgType != null ? !orgType.equals(rOrg.orgType) : rOrg.orgType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (orgType != null ? orgType.hashCode() : 0);
        result = 31 * result + (costCenter != null ? costCenter.hashCode() : 0);
        result = 31 * result + (locality != null ? locality.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(OrgType jaxb, ROrg repo, PrismContext prismContext) throws
            DtoTranslationException {
        RRole.copyFromJAXB(jaxb, repo, prismContext);

        repo.setCostCenter(jaxb.getCostCenter());
        repo.setDisplayName(RPolyString.copyFromJAXB(jaxb.getDisplayName()));
        repo.setIdentifier(jaxb.getIdentifier());
        repo.setLocality(RPolyString.copyFromJAXB(jaxb.getLocality()));
        repo.setOrgType(RUtil.listToSet(jaxb.getOrgType()));
    }

    public static void copyToJAXB(ROrg repo, OrgType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RRole.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setCostCenter(repo.getCostCenter());
        jaxb.setDisplayName(RPolyString.copyToJAXB(repo.getDisplayName()));
        jaxb.setIdentifier(repo.getIdentifier());
        jaxb.setLocality(RPolyString.copyToJAXB(repo.getLocality()));
        jaxb.getOrgType().addAll(RUtil.safeSetToList(repo.getOrgType()));
    }

    @Override
    public OrgType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        OrgType object = new OrgType();
        RUtil.revive(object, prismContext);
        ROrg.copyToJAXB(this, object, prismContext);

        return object;
    }
}

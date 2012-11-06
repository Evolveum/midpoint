package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceBusinessConfigurationType;


@Embeddable
public class RResourceBussinesConfiguration {
	
	private RResourceAdministrativeState administrativeState;
	private Set<REmbeddedReference> approverRef;
	
	@Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
	public RResourceAdministrativeState getAdministrativeState() {
		return administrativeState;
	}
	
	public void setAdministrativeState(RResourceAdministrativeState administrativeState) {
		this.administrativeState = administrativeState;
	}
	
	@ElementCollection
	@ForeignKey(name = "fk_resource_approver_ref")
	@CollectionTable(name = "m_resource_approver_ref", joinColumns = {
			@JoinColumn(name = "user_oid", referencedColumnName = "oid"),
			@JoinColumn(name = "user_id", referencedColumnName = "id") })
	@Cascade({ org.hibernate.annotations.CascadeType.ALL })
	public Set<REmbeddedReference> getApproverRef() {
		return approverRef;
	}
	
	public void setApproverRef(Set<REmbeddedReference> approverRef) {
		this.approverRef = approverRef;
	}
	
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RResourceBussinesConfiguration that = (RResourceBussinesConfiguration) o;

        if (administrativeState != null ? !administrativeState.equals(that.administrativeState) :
                that.administrativeState != null) return false;
        if (approverRef != null ? !approverRef.equals(that.approverRef) : that.approverRef != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = approverRef != null ? approverRef.hashCode() : 0;
        result = 31 * result + (administrativeState != null ? administrativeState.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(RResourceBussinesConfiguration repo, ResourceBusinessConfigurationType jaxb, ObjectType parent, ItemPath path,
            PrismContext prismContext) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (repo.getAdministrativeState() == null && (repo.getApproverRef() == null || repo.getApproverRef().isEmpty())){
        	jaxb = null;
        	return;
        }
        
		try {
			if (repo.getAdministrativeState() != null) {
				jaxb.setAdministrativeState(repo.getAdministrativeState().getAdministrativeState());
			}
			if (repo.getApproverRef() != null) {
				jaxb.getApproverRef().addAll(RUtil.safeSetApproverRefToList(repo.getApproverRef(), prismContext));
			}
		} catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }
    
    public boolean empty(){
    	return (administrativeState == null && (approverRef == null || approverRef.isEmpty()));
    }

    public static void copyFromJAXB(ResourceBusinessConfigurationType jaxb, RResourceBussinesConfiguration repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (jaxb.getAdministrativeState() == null && (jaxb.getApproverRef() == null || jaxb.getApproverRef().isEmpty())){
        	repo = null;
        	return;
        }
        
        try {
            if (jaxb.getAdministrativeState() != null){
            	repo.setAdministrativeState(RResourceAdministrativeState.toRepoType(jaxb.getAdministrativeState()));
            }
            if (jaxb.getApproverRef() != null){
            	Set<REmbeddedReference> approverRefs = new HashSet<REmbeddedReference>();
            	for (ObjectReferenceType ort : jaxb.getApproverRef()){
            		repo.getApproverRef().add(RUtil.jaxbRefToEmbeddedRepoRef(ort, prismContext));
            	}
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public ResourceBusinessConfigurationType toJAXB(ObjectType parent, ItemPath path, PrismContext prismContext) throws
            DtoTranslationException {
        ResourceBusinessConfigurationType businessConfiguration = new ResourceBusinessConfigurationType();
        RResourceBussinesConfiguration.copyToJAXB(this, businessConfiguration, parent, path, prismContext);
        return businessConfiguration;
    }
}

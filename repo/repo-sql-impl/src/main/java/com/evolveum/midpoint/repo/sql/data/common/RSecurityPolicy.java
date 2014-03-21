package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Collection;
import java.util.List;

import javax.persistence.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RDataSource;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExportType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROrientationType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@org.hibernate.annotations.Table(appliesTo = "m_security_policy",
        indexes = {@Index(name = "iSecurityPolicyName", columnNames = "name_orig")})
@ForeignKey(name = "fk_security_policy")
public class RSecurityPolicy extends RObject<SecurityPolicyType> {

    private RPolyString name;
    private String authentication;
    private String credentials;
  
    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RSecurityPolicy that = (RSecurityPolicy) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (authentication != null ? !authentication.equals(that.authentication) : that.authentication != null)
            return false;
        if (credentials != null ? !credentials.equals(that.credentials) : that.credentials != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (authentication != null ? authentication.hashCode() : 0);
        result = 31 * result + (credentials != null ? credentials.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(SecurityPolicyType jaxb, RSecurityPolicy repo, PrismContext prismContext)
            throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
      
        try {
            repo.setAuthentication(RUtil.toRepo(jaxb.getAuthentication(), prismContext));
            repo.setCredentials(RUtil.toRepo(jaxb.getCredentials(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyToJAXB(RSecurityPolicy repo, SecurityPolicyType jaxb, PrismContext prismContext,
                                  Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        RObject.copyToJAXB(repo, jaxb, prismContext, options);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        
        try {
            if (StringUtils.isNotEmpty(repo.getAuthentication())) {
                jaxb.setAuthentication(RUtil.toJAXB(SecurityPolicyType.class, new ItemPath(SecurityPolicyType.F_AUTHENTICATION),
                        repo.getAuthentication(), AuthenticationPolicyType.class, prismContext));
            }
            if (StringUtils.isNotEmpty(repo.getCredentials())) {
                jaxb.setCredentials(RUtil.toJAXB(SecurityPolicyType.class, new ItemPath(SecurityPolicyType.F_CREDENTIALS),
                        repo.getCredentials(), CredentialsPolicyType.class, prismContext));
            }
            
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public SecurityPolicyType toJAXB(PrismContext prismContext,
                             Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        SecurityPolicyType object = new SecurityPolicyType();
        RUtil.revive(object, prismContext);
        RSecurityPolicy.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}


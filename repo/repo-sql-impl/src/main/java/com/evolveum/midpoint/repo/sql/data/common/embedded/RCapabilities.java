package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

@Embeddable
public class RCapabilities {

    private String cachingMetadata;
    private String _native;
    private String configured;

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getCachingMetadata() {
        return cachingMetadata;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getNative() {
        return _native;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getConfigured() {
        return configured;
    }

    public void setCachingMetadata(String cachingMetadata) {
        this.cachingMetadata = cachingMetadata;
    }

    public void setNative(String _native) {
        this._native = _native;
    }

    public void setConfigured(String configured) {
        this.configured = configured;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RCapabilities that = (RCapabilities) o;

        if (cachingMetadata != null ? !cachingMetadata.equals(that.cachingMetadata) : that.cachingMetadata != null)
            return false;
        if (_native != null ? !_native.equals(that._native) : that._native != null)
            return false;
        if (configured != null ? !configured.equals(that.configured) : that._native != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = configured != null ? configured.hashCode() : 0;
        result = 31 * result + (_native != null ? _native.hashCode() : 0);
        result = 31 * result + (cachingMetadata != null ? cachingMetadata.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyFromJAXB(CapabilitiesType jaxb, RCapabilities repo, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(repo);
        Validate.notNull(jaxb);

        try {
            repo.setNative(RUtil.toRepo(jaxb.getNative(), prismContext));
            repo.setConfigured(RUtil.toRepo(jaxb.getConfigured(), prismContext));
            repo.setCachingMetadata(RUtil.toRepo(jaxb.getCachingMetadata(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public boolean empty() {
        return StringUtils.isBlank(_native) && StringUtils.isBlank(configured);
    }

    public static void copyToJAXB(RCapabilities repo, CapabilitiesType jaxb, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(repo);
        Validate.notNull(jaxb);
        try {
            jaxb.setNative(RUtil.toJAXB(CapabilitiesType.class, new ItemPath(CapabilitiesType.F__NATIVE),
                    repo.getNative(), CapabilityCollectionType.class, prismContext));
            jaxb.setConfigured(RUtil.toJAXB(CapabilitiesType.class, new ItemPath(CapabilitiesType.F_CONFIGURED),
                    repo.getConfigured(), CapabilityCollectionType.class, prismContext));
            jaxb.setCachingMetadata(RUtil.toJAXB(CachingMetadataType.class,
                    new ItemPath(CapabilitiesType.F_CACHING_METADATA), repo.getCachingMetadata(),
                    CachingMetadataType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public CapabilitiesType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        CapabilitiesType cap = new CapabilitiesType();
        copyToJAXB(this, cap, prismContext);
        return cap;
    }

}

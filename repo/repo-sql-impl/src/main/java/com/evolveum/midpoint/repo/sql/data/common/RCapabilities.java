package com.evolveum.midpoint.repo.sql.data.common;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;

@Embeddable
public class RCapabilities {

	private String _native;
	private String configured;

	@Column(nullable = true)
	@Type(type = "org.hibernate.type.TextType")
	public String getNative() {
		return _native;
	}

	public void setNative(String _native) {
		this._native = _native;
	}

	@Column(nullable = true)
	@Type(type = "org.hibernate.type.TextType")
	public String getConfigured() {
		return configured;
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
			jaxb.setNative(RUtil.toJAXB(CapabilitiesType.class, new PropertyPath(CapabilitiesType.F__NATIVE),
					repo.getNative(), CapabilityCollectionType.class, prismContext));
			jaxb.setConfigured(RUtil.toJAXB(CapabilitiesType.class, new PropertyPath(CapabilitiesType.F__NATIVE),
					repo.getConfigured(), CapabilityCollectionType.class, prismContext));
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

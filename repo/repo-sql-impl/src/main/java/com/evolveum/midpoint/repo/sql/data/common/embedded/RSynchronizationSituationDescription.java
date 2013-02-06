package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

@Embeddable
public class RSynchronizationSituationDescription implements Serializable {

    /**
     * This constant is fix for oracle DB, because it handles empty string as null.
     */
    private static final String UNDEFINED_CHANNEL = " ";

    @QueryAttribute(enumerated = true)
    private RSynchronizationSituation situation;
    private XMLGregorianCalendar timestampValue;
    private String chanel;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RSynchronizationSituation getSituation() {
        return situation;
    }

    public void setSituation(RSynchronizationSituation situation) {
        this.situation = situation;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getTimestampValue() {
        return timestampValue;
    }

    public void setTimestampValue(XMLGregorianCalendar timestampValue) {
        this.timestampValue = timestampValue;
    }

    public String getChanel() {
        if (chanel == null) {
            chanel = UNDEFINED_CHANNEL;
        }
        return chanel;
    }

    public void setChanel(String chanel) {
        this.chanel = chanel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RSynchronizationSituationDescription that = (RSynchronizationSituationDescription) o;

        if (situation != null ? !situation.equals(that.situation) : that.situation != null)
            return false;
        if (timestampValue != null ? !timestampValue.equals(that.timestampValue) : that.timestampValue != null)
            return false;
        if (getChanel() != null ? !getChanel().equals(that.getChanel()) : that.getChanel() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = situation != null ? situation.hashCode() : 0;
        result = 31 * result + (timestampValue != null ? timestampValue.hashCode() : 0);
        result = 31 * result + (getChanel() != null ? getChanel().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static RSynchronizationSituationDescription copyFromJAXB(SynchronizationSituationDescriptionType jaxb) {
        Validate.notNull(jaxb, "Synchronization situation description must not be null.");

        RSynchronizationSituationDescription repo = new RSynchronizationSituationDescription();
        repo.setChanel(jaxb.getChannel());
        repo.setTimestampValue(jaxb.getTimestamp());
        repo.setSituation(RSynchronizationSituation.toRepoType(jaxb.getSituation()));

        return repo;
    }

    public static SynchronizationSituationDescriptionType copyToJAXB(RSynchronizationSituationDescription repo) {
        Validate.notNull(repo, "Synchronization situation description must not be null.");

        SynchronizationSituationDescriptionType jaxb = new SynchronizationSituationDescriptionType();
        if (!UNDEFINED_CHANNEL.equals(repo.getChanel())) {
            jaxb.setChannel(repo.getChanel());
        }
        jaxb.setTimestamp(repo.getTimestampValue());
        if (repo.getSituation() != null) {
            jaxb.setSituation(repo.getSituation().getSyncType());
        }
        return jaxb;
    }
}

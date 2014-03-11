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

import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.data.common.id.RSynchronizationSituationDescriptionId;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

@Entity
@IdClass(RSynchronizationSituationDescriptionId.class)
@Table(name = "m_sync_situation_description")
public class RSynchronizationSituationDescription implements Serializable {

    private RShadow shadow;
    private String shadowOid;
    private String checksum;
    //fields
    private RSynchronizationSituation situation;
    private XMLGregorianCalendar timestampValue;
    private String chanel;
    private Boolean full;

    @ForeignKey(name = "none")
    @MapsId("shadow")
    @ManyToOne(fetch = FetchType.LAZY)
    public RShadow getShadow() {
        return shadow;
    }

    @Id
    @Column(name = "shadow_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getShadowOid() {
        if (shadowOid == null && shadow != null) {
            shadowOid = shadow.getOid();
        }
        return shadowOid;
    }

    /**
     * This method is used for content comparing when querying database (e. g. we don't want to compare clob values).
     *
     * @return md5 hash of some fields of this class
     */
    @Id
    @Column(length = 32, name = "checksum")
    public String getChecksum() {
        if (checksum == null) {
            recomputeChecksum();
        }
        return checksum;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RSynchronizationSituation getSituation() {
        return situation;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getTimestampValue() {
        return timestampValue;
    }

    public String getChanel() {
        return chanel;
    }

    @Column(name = "fullFlag")
    public Boolean isFull() {
        return full;
    }

    public void setFull(Boolean full) {
        this.full = full;

        recomputeChecksum();
    }

    public void setTimestampValue(XMLGregorianCalendar timestampValue) {
        this.timestampValue = timestampValue;

        recomputeChecksum();
    }

    public void setSituation(RSynchronizationSituation situation) {
        this.situation = situation;

        recomputeChecksum();
    }

    public void setChanel(String chanel) {
        this.chanel = chanel;

        recomputeChecksum();
    }

    public void setShadow(RShadow shadow) {
        this.shadow = shadow;
    }

    public void setShadowOid(String shadowOid) {
        this.shadowOid = shadowOid;
    }

    public void setChecksum(String checksum) {
        //this method is here only to satisfy hibernate, checksum value is always recomputed
    }

    @Transient
    private void recomputeChecksum() {
        checksum = RUtil.computeChecksum(situation, timestampValue, chanel, full);
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
        if (chanel != null ? !chanel.equals(that.chanel) : that.chanel != null)
            return false;
        if (checksum != null ? !checksum.equals(that.checksum) : that.checksum != null)
            return false;
        if (full != null ? !full.equals(that.full) : that.full != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = situation != null ? situation.hashCode() : 0;
        result = 31 * result + (timestampValue != null ? timestampValue.hashCode() : 0);
        result = 31 * result + (chanel != null ? chanel.hashCode() : 0);
        result = 31 * result + (checksum != null ? checksum.hashCode() : 0);
        result = 31 * result + (full != null ? full.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static RSynchronizationSituationDescription copyFromJAXB(RShadow owner,
                                                                    SynchronizationSituationDescriptionType jaxb) {
        Validate.notNull(owner, "Resource object shadow must not be null.");
        Validate.notNull(jaxb, "Synchronization situation description must not be null.");

        RSynchronizationSituationDescription repo = new RSynchronizationSituationDescription();
        repo.setShadow(owner);
        repo.setChanel(jaxb.getChannel());
        repo.setTimestampValue(jaxb.getTimestamp());
        repo.setSituation(RUtil.getRepoEnumValue(jaxb.getSituation(), RSynchronizationSituation.class));
        repo.setFull(jaxb.isFull());

        return repo;
    }

    public static SynchronizationSituationDescriptionType copyToJAXB(RSynchronizationSituationDescription repo) {
        Validate.notNull(repo, "Synchronization situation description must not be null.");

        SynchronizationSituationDescriptionType jaxb = new SynchronizationSituationDescriptionType();
        jaxb.setChannel(repo.getChanel());
        jaxb.setTimestamp(repo.getTimestampValue());
        if (repo.getSituation() != null) {
            jaxb.setSituation(repo.getSituation().getSchemaValue());
        }
        jaxb.setFull(repo.isFull());
        return jaxb;
    }
}

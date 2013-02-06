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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RSynchronizationSituationDescriptionId implements Serializable {

    private String shadowOid;
    private Long shadowId;
    private String checksum;

    public String getShadowOid() {
        return shadowOid;
    }

    public void setShadowOid(String shadowOid) {
        this.shadowOid = shadowOid;
    }

    public Long getShadowId() {
        return shadowId;
    }

    public void setShadowId(Long shadowId) {
        this.shadowId = shadowId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RSynchronizationSituationDescriptionId that = (RSynchronizationSituationDescriptionId) o;

        if (checksum != null ? !checksum.equals(that.checksum) : that.checksum != null) return false;
        if (shadowId != null ? !shadowId.equals(that.shadowId) : that.shadowId != null) return false;
        if (shadowOid != null ? !shadowOid.equals(that.shadowOid) : that.shadowOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = shadowOid != null ? shadowOid.hashCode() : 0;
        result = 31 * result + (shadowId != null ? shadowId.hashCode() : 0);
        result = 31 * result + (checksum != null ? checksum.hashCode() : 0);
        return result;
    }
}

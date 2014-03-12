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

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RSynchronizationSituationDescriptionId implements Serializable {

    private String shadowOid;
    private String checksum;

    public String getShadowOid() {
        return shadowOid;
    }

    public void setShadowOid(String shadowOid) {
        this.shadowOid = shadowOid;
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
        if (shadowOid != null ? !shadowOid.equals(that.shadowOid) : that.shadowOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = shadowOid != null ? shadowOid.hashCode() : 0;
        result = 31 * result + (checksum != null ? checksum.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RSynchronizationSituationDescriptionId{shadowOid='").append(shadowOid).append('\'');
        sb.append(", checksum='").append(checksum).append("'}");

        return sb.toString();
    }
}

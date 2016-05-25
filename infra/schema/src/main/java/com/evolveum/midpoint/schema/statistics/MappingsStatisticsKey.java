/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.statistics;

/**
 * @author Pavol Mederly
 */
public class MappingsStatisticsKey {

    private final String objectOid;
    private final String objectName;
    private final String objectType;

    public MappingsStatisticsKey(String objectOid, String objectName, String objectType) {
        this.objectOid = objectOid;
        this.objectName = objectName;
		this.objectType = objectType;
    }

    public String getObjectOid() {
        return objectOid;
    }

    public String getObjectName() {
        return objectName;
    }

	public String getObjectType() {
		return objectType;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MappingsStatisticsKey that = (MappingsStatisticsKey) o;

        if (objectOid != null ? !objectOid.equals(that.objectOid) : that.objectOid != null) return false;
        if (objectType != null ? !objectType.equals(that.objectType) : that.objectType != null) return false;
        return !(objectName != null ? !objectName.equals(that.objectName) : that.objectName != null);

    }

    @Override
    public int hashCode() {
        int result = objectOid != null ? objectOid.hashCode() : 0;
        result = 31 * result + (objectName != null ? objectName.hashCode() : 0);
        return result;
    }
}

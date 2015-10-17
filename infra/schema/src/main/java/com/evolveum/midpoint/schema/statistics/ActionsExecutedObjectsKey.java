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

import com.evolveum.midpoint.prism.delta.ChangeType;

import javax.xml.namespace.QName;

/**
 * @author Pavol Mederly
 */
public class ActionsExecutedObjectsKey {

    private QName objectType;
    private ChangeType operation;
    private String channel;

    public ActionsExecutedObjectsKey(QName objectType, ChangeType operation, String channel) {
        this.objectType = objectType;
        this.operation = operation;
        this.channel = channel;
    }

    public QName getObjectType() {
        return objectType;
    }

    public ChangeType getOperation() {
        return operation;
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionsExecutedObjectsKey that = (ActionsExecutedObjectsKey) o;

        if (objectType != null ? !objectType.equals(that.objectType) : that.objectType != null) return false;
        if (operation != that.operation) return false;
        return !(channel != null ? !channel.equals(that.channel) : that.channel != null);

    }

    @Override
    public int hashCode() {
        int result = objectType != null ? objectType.hashCode() : 0;
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        return result;
    }
}

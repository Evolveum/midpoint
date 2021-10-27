/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state.actions;

import com.evolveum.midpoint.prism.delta.ChangeType;

import javax.xml.namespace.QName;
import java.util.Objects;

class ActionsExecutedObjectsKey {

    private final QName objectType;
    private final ChangeType operation;
    private final String channel;

    ActionsExecutedObjectsKey(QName objectType, ChangeType operation, String channel) {
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActionsExecutedObjectsKey that = (ActionsExecutedObjectsKey) o;
        return Objects.equals(objectType, that.objectType) && operation == that.operation && Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectType, operation, channel);
    }

    @Override
    public String toString() {
        return "ActionsExecutedObjectsKey{" +
                "objectType=" + objectType +
                ", operation=" + operation +
                ", channel='" + channel + '\'' +
                '}';
    }
}

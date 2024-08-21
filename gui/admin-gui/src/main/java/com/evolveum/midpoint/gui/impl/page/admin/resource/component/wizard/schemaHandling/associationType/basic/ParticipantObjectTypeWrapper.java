/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Objects;

public class ParticipantObjectTypeWrapper implements Serializable {

    private final ShadowKindType kind;
    private final String intent;
    private final String displayName;
    private final QName objectClass;

    ParticipantObjectTypeWrapper(ShadowKindType kind, String intent, String displayName, QName objectClass){
        this.kind = kind;
        this.intent = intent;
        this.displayName = displayName;
        this.objectClass = objectClass;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public String getIntent() {
        return intent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public QName getObjectClass() {
        return objectClass;
    }

    public boolean equals(ShadowKindType kind, String intent) {
        return kind == this.kind && Objects.equals(intent, this.intent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ParticipantObjectTypeWrapper that = (ParticipantObjectTypeWrapper) o;
        return kind == that.kind && Objects.equals(intent, that.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, intent);
    }
}

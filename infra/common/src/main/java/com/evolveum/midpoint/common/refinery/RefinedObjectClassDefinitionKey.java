/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import javax.xml.namespace.QName;

/**
 * @author semancik
 */
class RefinedObjectClassDefinitionKey {
    private QName typeName;
    private String intent;

    RefinedObjectClassDefinitionKey(RefinedObjectClassDefinition rObjectClassDefinition) {
        typeName = rObjectClassDefinition.getTypeName();
        intent = rObjectClassDefinition.getIntent();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((intent == null) ? 0 : intent.hashCode());
        result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RefinedObjectClassDefinitionKey other = (RefinedObjectClassDefinitionKey) obj;
        if (intent == null) {
            if (other.intent != null) {
                return false;
            }
        } else if (!intent.equals(other.intent)) {
            return false;
        }
        if (typeName == null) {
            if (other.typeName != null) {
                return false;
            }
        } else if (!typeName.equals(other.typeName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "(type=" + typeName + ", intent=" + intent + ")";
    }
}

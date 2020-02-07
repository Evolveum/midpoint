/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import java.io.Serializable;

import javax.xml.namespace.QName;

/**
 * @author semancik
 */
public class SchemaMigration implements Serializable {
    private static final long serialVersionUID = 1L;

    private final QName elementQName;
    private final String version;
    private final SchemaMigrationOperation operation;

    public SchemaMigration(QName elementQName, String version, SchemaMigrationOperation operation) {
        super();
        this.elementQName = elementQName;
        this.version = version;
        this.operation = operation;
    }

    public QName getElementQName() {
        return elementQName;
    }

    public String getVersion() {
        return version;
    }

    public SchemaMigrationOperation getOperation() {
        return operation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((elementQName == null) ? 0 : elementQName.hashCode());
        result = prime * result + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        SchemaMigration other = (SchemaMigration) obj;
        if (elementQName == null) {
            if (other.elementQName != null) {
                return false;
            }
        } else if (!elementQName.equals(other.elementQName)) {
            return false;
        }
        if (operation != other.operation) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SchemaMigration(element=" + elementQName + ", version=" + version + ", operation="
                + operation + ")";
    }

}

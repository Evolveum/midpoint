/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

import java.io.Serializable;
import java.util.Objects;

/**
 * Lightweight identifier is a "reasonable unique" identifier that is very cheap
 * to create. While objects have OID, creating a unique OID means communication
 * with the repository. This quite expensive and therefore it is unsuitable
 * for more purposes, such as creating identifiers for tasks or audit records.
 * Lightweight identifiers are used instead.
 *
 * @see https://wiki.evolveum.com/display/midPoint/Lightweight+Identifier
 *
 * @author semancik
 */
public class LightweightIdentifier implements Serializable {

    private static final String SEPARATOR = "-";

    private long timestamp;
    private int hostIdentifier;
    private int sequenceNumber;
    private String string;

    public LightweightIdentifier(long timestamp, int hostIdentifier, int sequenceNumber) {
        this.timestamp = timestamp;
        this.hostIdentifier = hostIdentifier;
        this.sequenceNumber = sequenceNumber;
        formatString();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getHostIdentifier() {
        return hostIdentifier;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    private void formatString() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp);
        sb.append(SEPARATOR);
        sb.append(hostIdentifier);
        sb.append(SEPARATOR);
        sb.append(sequenceNumber);
        string = sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LightweightIdentifier that = (LightweightIdentifier) o;
        return Objects.equals(string, that.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string);
    }

    @Override
    public String toString() {
        return string;
    }
}

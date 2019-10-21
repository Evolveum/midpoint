/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

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
public class LightweightIdentifier {

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((string == null) ? 0 : string.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LightweightIdentifier other = (LightweightIdentifier) obj;
        if (string == null) {
            if (other.string != null)
                return false;
        } else if (!string.equals(other.string))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return string;
    }

}

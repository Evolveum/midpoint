/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
 * See https://docs.evolveum.com/midpoint/architecture/concepts/lightweight-identifier/
 *
 * @author semancik
 */
public class LightweightIdentifier implements Serializable {

    private static final String SEPARATOR = "-";

    private final long timestamp;
    private final int hostIdentifier;
    private final int sequenceNumber;
    private final String string;

    public LightweightIdentifier(long timestamp, int hostIdentifier, int sequenceNumber) {
        this.timestamp = timestamp;
        this.hostIdentifier = hostIdentifier;
        this.sequenceNumber = sequenceNumber;
        this.string = formatString();
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

    private String formatString() {
        return timestamp
                + SEPARATOR
                + hostIdentifier
                + SEPARATOR
                + sequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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

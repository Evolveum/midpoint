/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.util;

/**
 * @author semancik
 *
 */
public class Counter {
    private int count = 0;

    public int getCount() {
        return count;
    }

    public synchronized void click() {
        count++;
    }

    public void assertCount(int expectedCount) {
        assert count == expectedCount : "Wrong counter, expected "+expectedCount+", was "+count;
    }

    public void assertCount(String message, int expectedCount) {
        assert count == expectedCount : message + ", expected "+expectedCount+", was "+count;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + count;
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
        Counter other = (Counter) obj;
        if (count != other.count) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Counter(" + count + ")";
    }


}

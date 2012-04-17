package com.evolveum.midpoint.task.quartzimpl;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 17.4.2012
 * Time: 21:57
 * To change this template use File | Settings | File Templates.
 */
public enum UseThreadInterrupt {

    NEVER("never"), WHEN_NECESSARY("whenNecessary"), ALWAYS("always");

    private final String value;

    UseThreadInterrupt(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static UseThreadInterrupt fromValue(String v) {
        for (UseThreadInterrupt c: UseThreadInterrupt.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

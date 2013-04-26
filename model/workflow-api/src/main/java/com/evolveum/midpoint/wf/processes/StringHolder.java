package com.evolveum.midpoint.wf.processes;

import java.io.Serializable;

/**
 * Just to persuade Activiti to store strings into dedicated table enabling more than 4000 characters.
 *
 * @author mederly
 */
public class StringHolder implements Serializable {

    private String value;

    public StringHolder(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.assignment;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class AssignmentSearchDto implements Serializable {

    public static final String F_SEARCH_TEXT = "text";
    public static final String F_SEARCH_ROLE_TYPE = "type";

    private String text;
    private String type;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentSearchDto)) return false;

        AssignmentSearchDto that = (AssignmentSearchDto) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}

/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;
import java.util.Objects;

public class RObjectTextInfoId implements Serializable {

    private String ownerOid;
    private String text;

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RObjectTextInfoId))
            return false;
        RObjectTextInfoId that = (RObjectTextInfoId) o;
        return Objects.equals(ownerOid, that.ownerOid) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOid, text);
    }

    @Override
    public String toString() {
        return "RObjectTextInfoId[" + ownerOid + "," + text + "]";
    }
}

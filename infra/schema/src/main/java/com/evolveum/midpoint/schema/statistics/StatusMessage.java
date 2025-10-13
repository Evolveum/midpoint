/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Pavol
 */
public class StatusMessage implements Serializable {
    private Date date;
    private String message;

    public StatusMessage(Date date, String message) {
        this.date = date;
        this.message = message;
    }

    public StatusMessage(String message) {
        this.date = new Date();
        this.message = message;
    }

    public Date getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }
}

/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

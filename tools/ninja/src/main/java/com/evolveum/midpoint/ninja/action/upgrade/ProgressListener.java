/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

public interface ProgressListener {

    void update(long bytesRead, long contentLength, boolean done);
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Utility methods useful for displaying task information.
 * Intentionally package-private for now.
 */
@Experimental
class TaskDisplayUtil {

    static Long getExecutionTime(TaskType task) {
        Long started = WebComponentUtil.xgc2long(task.getLastRunStartTimestamp());
        if (started == null) {
            return null;
        }
        Long finished = WebComponentUtil.xgc2long(task.getLastRunFinishTimestamp());
        if (finished == null || finished < started) {
            finished = System.currentTimeMillis();
        }
        return finished - started;
    }

}

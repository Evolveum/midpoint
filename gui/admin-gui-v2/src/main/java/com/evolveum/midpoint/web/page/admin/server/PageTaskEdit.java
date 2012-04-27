/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class PageTaskEdit extends PageAdminTasks {

    public static final String PARAM_TASK_ID = "taskOid";
    private static final String OPERATION_LOAD_TASK = "pageTask.loadTask";
    private static final String OPERATION_SAVE_TASK = "pageTask.saveTask";
    private IModel<ObjectViewDto> model;

    public PageTaskEdit() {
        initLayout();
    }

    private void initLayout() {

    }
}

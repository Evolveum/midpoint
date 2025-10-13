/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.wf.processes;

import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class EmptyProcessDetailsPanel extends Panel {

    public EmptyProcessDetailsPanel(String id, IModel<ProcessInstanceDto> model) {
        super(id);
    }

}

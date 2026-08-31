/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;

public interface EvidenceUploadStateAware extends Serializable {

    void evidenceUploadStateChanged(
            AjaxRequestTarget target,
            boolean invalid);
}

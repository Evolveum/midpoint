/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.certification.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrWorkItemDto;
import org.apache.wicket.model.IModel;

@FunctionalInterface
public interface CertGuiHandler {
    String getCaseInfoButtonTitle(IModel<? extends CertCaseOrWorkItemDto> rowModel, PageBase page);
}

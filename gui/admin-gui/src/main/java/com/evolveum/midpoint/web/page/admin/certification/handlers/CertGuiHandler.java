/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrWorkItemDto;
import org.apache.wicket.model.IModel;

@FunctionalInterface
public interface CertGuiHandler {
    String getCaseInfoButtonTitle(IModel<? extends CertCaseOrWorkItemDto> rowModel, PageBase page);
}

/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.workflow.dto;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.datetime.PatternDateConverter;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author mederly
 */
public class ProcessInstanceDto extends Selectable<ProcessInstanceDto> {

    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_NAME = "name";
    public static final String F_START_FORMATTED = "startFormatted";
    public static final String F_END_FORMATTED = "endFormatted";
    public static final String F_STAGE = "stage";

    @NotNull private final CaseType aCase;

    private final PatternDateConverter converter;

    public ProcessInstanceDto(@NotNull CaseType aCase, String dateTimeStyle) {
        this.aCase = aCase;
        converter = new PatternDateConverter
                (WebComponentUtil.getLocalizedDatePattern(dateTimeStyle), true);
        Validate.notNull(aCase.getApprovalContext(), "Case has no workflow context");
    }

    public XMLGregorianCalendar getStartTimestamp() {
        return aCase.getMetadata() != null ? aCase.getMetadata().getCreateTimestamp() : null;
    }

    public XMLGregorianCalendar getEndTimestamp() {
        return aCase.getCloseTimestamp();
    }

    public String getStartFormatted() {
        return getStartTimestamp() != null ? converter.convertToString(XmlTypeConverter.toDate(getStartTimestamp()),
                WebComponentUtil.getCurrentLocale()) : "";
    }

    public String getEndFormatted() {
        return getEndTimestamp() != null ? converter.convertToString(XmlTypeConverter.toDate(getEndTimestamp()),
                WebComponentUtil.getCurrentLocale()) : "";
    }

    @NotNull
    public ApprovalContextType getApprovalContext() {
        return aCase.getApprovalContext();
    }

    public String getName() {
        return PolyString.getOrig(aCase.getName());
    }

    public String getOutcome() {
        return aCase.getOutcome();
    }

    public String getObjectName() {
        return WebComponentUtil.getName(aCase.getObjectRef());
    }

    public ObjectReferenceType getObjectRef() {
        return aCase.getObjectRef();
    }

    public ObjectReferenceType getTargetRef() {
        return aCase.getTargetRef();
    }

    public QName getObjectType() {
        return getObjectRef() != null ? getObjectRef().getType() : null;
    }

    public QName getTargetType() {
        return getTargetRef() != null ? getTargetRef().getType() : null;
    }

    public String getTargetName() {
        return WebComponentUtil.getName(aCase.getTargetRef());
    }

    public String getStage() {
        return ApprovalContextUtil.getStageInfo(aCase);
    }

    public String getProcessInstanceId() {
        return aCase.getOid();
    }

    public String getTaskOid() {
        return aCase.getOid();
    }
}

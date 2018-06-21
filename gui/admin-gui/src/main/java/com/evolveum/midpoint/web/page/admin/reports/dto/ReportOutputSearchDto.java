/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;

import java.util.*;

/**
 * @author lazyman
 * @author shood
 */
public class ReportOutputSearchDto extends Selectable implements DebugDumpable {
	private static final long serialVersionUID = 1L;

	public static final String F_REPORT_TYPE = "reportType";
    public static final String F_FILE_TYPE = "fileType";
    public static final String F_TYPE = "type";
    public static final String F_TEXT = "text";
    public static final String F_REPORT_TYPES = "reportTypes";

    private Map<String, String> reportTypeMap = new HashMap<>();
    private ExportType fileType;
    private String reportType = "Report Type";
    private String text;

    public ReportOutputSearchDto() {
    }

    public Map<String, String> getReportTypeMap() {
        return reportTypeMap;
    }

    public void setReportTypeMap(Map<String, String> reportTypeMap) {
        this.reportTypeMap = reportTypeMap;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ExportType getFileType() {
        return fileType;
    }

    public void setFileType(ExportType fileType) {
        this.fileType = fileType;
    }

    public List<String> getReportTypes() {
        List<String> list = new ArrayList<>();
        list.addAll(reportTypeMap.keySet());

        Collections.sort(list);

        return list;
    }

    @Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ReportOutputSearchDto\n");
		DebugUtil.debugDumpWithLabelLn(sb, "reportTypeMap", reportTypeMap, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "fileType", fileType==null?null:fileType.toString(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "reportType", reportType, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "text", text, indent+1);
		return sb.toString();
	}
}

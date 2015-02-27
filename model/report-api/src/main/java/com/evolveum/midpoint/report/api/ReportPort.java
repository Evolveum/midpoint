package com.evolveum.midpoint.report.api;

import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;

public interface ReportPort {
	String CLASS_NAME_WITH_DOT = ReportPortType.class.getName() + ".";
	String PROCESS_REPORT = CLASS_NAME_WITH_DOT + "processReport";

}

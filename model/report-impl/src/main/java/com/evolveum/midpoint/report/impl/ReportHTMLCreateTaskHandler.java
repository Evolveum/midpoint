/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.report.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.ObjectResolver;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author skublik
 */

@Component
public class ReportHTMLCreateTaskHandler extends ReportJasperCreateTaskHandler {

    public static final String REPORT_HTML_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/html/create/handler-3";
    private static final Trace LOGGER = TraceManager.getTrace(ReportHTMLCreateTaskHandler.class);

    private static String START_THEAD = "<!--THEAD-->";
    private static String START_TBODY = "<!--TBODY-->";
    private static final String REPORT_ADMIN_DASHBOARD_SUBTYPE = "admin-dashboard-report";
    private static final String REPORT_HTML_TEMPLATE_FILE_NAME = "admin-dashboard-report-template.html";
    

    @Autowired private TaskManager taskManager;
    @Autowired private AuditService auditService;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;

    @PostConstruct
    private void initialize() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Registering with taskManager as a handler for " + REPORT_HTML_CREATE_TASK_URI);
        }
        taskManager.registerHandler(REPORT_HTML_CREATE_TASK_URI, this);
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        // TODO Auto-generated method stub
        OperationResult parentResult = task.getResult();
        OperationResult result = parentResult.createSubresult(ReportHTMLCreateTaskHandler.class.getSimpleName() + ".run");

        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(result);

        super.recordProgress(task, 0, result);
        try {
            ReportType parentReport = objectResolver.resolve(task.getObjectRef(), ReportType.class, null, "resolving report", task, result);
            
            if(!parentReport.getSubtype().contains(REPORT_ADMIN_DASHBOARD_SUBTYPE)) {
            	parentReport.setExport(ExportType.HTML);
            	return super.run(task);
            }
            
            String template = "";
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream in = classLoader.getResourceAsStream(REPORT_HTML_TEMPLATE_FILE_NAME);
    		byte[] data = IOUtils.toByteArray(in);
    		template = new String(data, Charset.defaultCharset());
            
    		
    		String[] parts = template.split(START_THEAD);
    		StringBuilder body = new StringBuilder(parts[0]);
    		Task opTask = taskManager.createTaskInstance();
    		body.append(createTHead());
    		parts = parts[1].split(START_TBODY);
    		body.append(parts[0]);
    		body.append(createTBody(opTask));
    		body.append(parts[1]);
    		
            String reportFilePath = getDestinationFileName(parentReport);
            FileUtils.writeByteArrayToFile(new File(reportFilePath), body.toString().getBytes());
            super.saveReportOutputType(reportFilePath, parentReport, task, result);
            LOGGER.trace("create report output type : {}", reportFilePath);

            if (parentReport.getPostReportScript() != null) {
                super.processPostReportScript(parentReport, reportFilePath, task, result);
            }
            result.computeStatus();

        } catch (Exception ex) {
            LOGGER.error("CreateReport: {}", ex.getMessage(), ex);
            result.recordFatalError(ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.trace("CreateReportTaskHandler.run stopping");
        return runResult;
    }
    
    private String createTHead() {
    	List<HTMLElement> childElements = new ArrayList<HTMLElement>();
		for(String value : getHead()) {
			HTMLElement div = new HTMLElement("div").addChildElement(new HTMLElement("span").setClasses("sortableLabel").setText(value));
			childElements.add(new HTMLElement("th").addChildElement(div));
		}
		return new HTMLElement("tr").setChildElements(childElements).toHTMLFormat();
	}
    
    private String createTBody(Task task) {
    	StringBuilder sb = new StringBuilder();
    	List<String> heads = getHead();
		for(Map<String, HTMLElement> value : getBody(task, task.getResult())) {
			List<HTMLElement> childElementsOfTr = new ArrayList<HTMLElement>();
			for(String head: heads) {
				HTMLElement div = value.get(head);
				childElementsOfTr.add(new HTMLElement("td").addChildElement(div).setStyle("width: 10%;"));
			}
			sb.append(new HTMLElement("tr").setChildElements(childElementsOfTr).toHTMLFormat());
		}
		return sb.toString();
	}

	private List<String> getHead(){
    	List<String> heads = new ArrayList<>();
    	heads.add("Label");
    	heads.add("Number");
    	heads.add("Status");
    	
    	return heads;
    }
    
    private List<Map<String, HTMLElement>> getBody(Task task, OperationResult parentResult){
    	List<Map<String, HTMLElement>> body = new ArrayList<>();
    	Map<String, HTMLElement> resources = new HashMap<String, HTMLElement>();
    	
    	resources.put("Label", new HTMLElement("div").setText("Resources"));
    	String number = getNumber(ResourceType.class,
    			Arrays.asList(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
    			AvailabilityStatusType.UP, false, task, parentResult);
    	resources.put("Number", new HTMLElement("div").setText(number + " up"));
    	HTMLElement element = new HTMLElement("div").setStyle("width: 100%; height: 20px;");
    	setStatusColor(number, element);
    	resources.put("Status", element);
    	body.add(resources);
    	
    	Map<String, HTMLElement> tasks = new HashMap<String, HTMLElement>();
    	tasks.put("Label", new HTMLElement("div").setText("Tasks"));
    	number = getNumber(TaskType.class,
    			Arrays.asList(TaskType.F_EXECUTION_STATUS),
    			TaskExecutionStatusType.RUNNABLE, false, task, parentResult);
    	tasks.put("Number", new HTMLElement("div").setText(number + " active"));
    	element = new HTMLElement("div").setStyle("width: 100%; height: 20px;");
    	setStatusColor(number, element);
    	tasks.put("Status", element);
    	body.add(tasks);
    	
    	Map<String, HTMLElement> modifications = new HashMap<String, HTMLElement>();
    	modifications.put("Label", new HTMLElement("div").setText("Modifications"));
    	int total = listModificationsRecords(false).size();
    	int active = listModificationsRecords(true).size();
    	number = formatPercentage(total, active) + " %";
    	modifications.put("Number", new HTMLElement("div").setText(number + " success"));
    	element = new HTMLElement("div").setStyle("width: 100%; height: 20px;");
    	setStatusColor(total, active, false, element);
    	modifications.put("Status", element);
    	body.add(modifications);
    	
    	Map<String, HTMLElement> errors = new HashMap<String, HTMLElement>();
    	errors.put("Label", new HTMLElement("div").setText("Errors"));
    	total = listAllOperationsRecords().size();
    	active = listErrorsRecords().size();
    	number = formatPercentage(total, active) + " %";
    	errors.put("Number", new HTMLElement("div").setText(number + " failed"));
    	element = new HTMLElement("div").setStyle("width: 100%; height: 20px;");
    	setStatusColor(total, active, true, element);
    	errors.put("Status", element);
    	body.add(errors);
    	
    	return body;
    }
    
    private void setStatusColor(int totalCount, int activeCount, boolean zeroIsGood, HTMLElement element) {
		if((zeroIsGood && activeCount == 0) || (totalCount == activeCount)) {
			element.setClasses("object-access-bg");
			return;
		} 
		element.setClasses("object-failed-bg");
	}
    
    private void setStatusColor(String number, HTMLElement element) {
    	String[] parts = number.split("/");
    	setStatusColor(Integer.valueOf(parts[1]), Integer.valueOf(parts[0]), false, element);
	}
    
    private String getNumber(Class type, List<QName> items, Object eqObject, boolean isPercentage, Task task, OperationResult parentResult){
    	
    	Integer total = 0;
    	Integer active = 0;
    	try {
    		total = modelService.countObjects(type, null, null, task, parentResult);
    		ObjectQuery query = prismContext.queryFor(type)
				.item((QName[])items.toArray()).eq(eqObject)
				.build();
    		if(total == null) {
    			total = 0;
    		}
		
			active = modelService.countObjects(type, query, null, task, parentResult);
			if(active == null) {
				active = 0;
    		}
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException | ConfigurationException
				| CommunicationException | ExpressionEvaluationException e) {
			return "ERROR: "+e.getMessage();
		}
    	
    	if(isPercentage) {
    		return formatPercentage(total, active) + " %";
    	}
    	
    	return active + "/" + total;
    }
    
    private String formatPercentage(int totalItems, int actualItems) {
    	float percentage = (totalItems==0 ? 0 : actualItems*100.0f/totalItems);
    	String format = "%.0f";
    	
    	if(percentage < 100.0f && percentage % 10 != 0 && ((percentage % 10) % 1) != 0) {
    		format = "%.1f";
    	}
    	return String.format(format, percentage);
    }
    
    private List<AuditEventRecordType> listModificationsRecords(boolean isSuccess){
    	Map<String, Object> parameters = new HashedMap<String, Object>();
		List<String> conditions = new ArrayList<>();
		conditions.add("aer.eventType = :auditEventType");
		parameters.put("auditEventType", AuditEventTypeType.MODIFY_OBJECT);
		
		if(isSuccess){
			conditions.add("aer.outcome = :outcome");
			parameters.put("outcome", OperationResultStatusType.SUCCESS);
		}
		
		return listAuditRecords(parameters, conditions);
    }
    
    private List<AuditEventRecordType> listErrorsRecords(){
    	Map<String, Object> parameters = new HashedMap<String, Object>();
		List<String> conditions = new ArrayList<>();
		conditions.add("aer.outcome = :outcome");
		parameters.put("outcome", OperationResultStatusType.FATAL_ERROR);
		
		return listAuditRecords(parameters, conditions);
    }
    
    private List<AuditEventRecordType> listAllOperationsRecords(){
    	Map<String, Object> parameters = new HashedMap<String, Object>();
		List<String> conditions = new ArrayList<>();
		return listAuditRecords(parameters, conditions);
    }
    
    private List<AuditEventRecordType> listAuditRecords(Map<String, Object> parameters, List<String> conditions) {
		
		Date date = new Date(System.currentTimeMillis() - (24*3600000));
		conditions.add("aer.timestamp >= :from");
		parameters.put("from", XmlTypeConverter.createXMLGregorianCalendar(date));
		conditions.add("aer.eventStage = :auditStageType");
		parameters.put("auditStageType", AuditEventStageType.EXECUTION);
		
		String query = "from RAuditEventRecord as aer";
		if (!conditions.isEmpty()) {
			query += " where ";
		}
		
		query += conditions.stream().collect(Collectors.joining(" and "));
		query += " order by aer.timestamp desc";


        List<AuditEventRecord> auditRecords;
		auditRecords = auditService.listRecords(query, parameters);
		
		if (auditRecords == null) {
			auditRecords = new ArrayList<>();
		}
		List<AuditEventRecordType> auditRecordList = new ArrayList<>();
		for (AuditEventRecord record : auditRecords){
			auditRecordList.add(record.createAuditEventRecordType());
		}
		return auditRecordList;
	}
    
    private class HTMLElement {
    	
    	private String tag;
    	private List<HTMLElement> childElements = new ArrayList<HTMLElement>();
    	private String text = "";
    	private String style = "";
    	private String classes = "";
    	
    	public HTMLElement(String tag) {
    		this.tag = tag;
		}
    	
    	public HTMLElement setText(String text) {
			this.text = StringEscapeUtils.unescapeHtml4(text);
			return this;
		}
    	
    	public HTMLElement setStyle(String style) {
			this.style = StringEscapeUtils.unescapeHtml4(style);
			return this;
		}
    	
    	public HTMLElement setClasses(String classes) {
			this.classes = StringEscapeUtils.unescapeHtml4(classes);
			return this;
		}
    	
    	public HTMLElement setChildElements(List<HTMLElement> childElements) {
			this.childElements = childElements;
			return this;
		}
    	
    	public HTMLElement addChildElement(HTMLElement childElement) {
			this.childElements.add(childElement);
			return this;
		}
    	
    	public String toHTMLFormat() {
    		StringBuilder sb = new StringBuilder("<" + tag + " style=\"" + style + "\" class=\"" + classes + "\">" + text);
    		
    		for(HTMLElement element : childElements) {
    			sb.append(element.toHTMLFormat());
    		}
    		
    		sb.append("</" + tag + ">");
    		return sb.toString();
    	}
    }
    
    @Override
    protected ExportType getExport(ReportType report) {
    	return ExportType.HTML;
    }
    
}

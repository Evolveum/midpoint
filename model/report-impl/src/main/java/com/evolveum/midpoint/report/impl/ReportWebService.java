package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JasperReport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportPort;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;

@Service
public class ReportWebService implements ReportPortType, ReportPort {

	private static transient Trace LOGGER = TraceManager.getTrace(ReportWebService.class);

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private ModelService model;

	@Autowired(required = true)
	private ObjectResolver objectResolver;

	@Autowired(required = true)
	private AuditService auditService;

	@Override
	public ObjectListType processReport(ReportType report) {

		Task task = taskManager.createTaskInstance("process report");
		OperationResult parentResult = task.getResult();

		JasperReport jasperReport;
		try {
			jasperReport = ReportUtils.loadJasperReport(report);

			JRDataset[] datasets = jasperReport.getDatasets();

			if (datasets.length > 1) {
				throw new UnsupportedOperationException("Only one dataset supported");
			}

			JRDataset dataset = datasets[0];

			MidPointQueryExecutor queryExecutor = new MidPointQueryExecutor(prismContext, taskManager,
					dataset);
			List results = new ArrayList<>();
			if (queryExecutor.getQuery() != null) {
				results = ReportUtils.getReportData(model, queryExecutor.getType(), queryExecutor.getQuery(),
						task, parentResult);
			} else {
				ReportFunctions reportFunctions = new ReportFunctions(prismContext, model, taskManager,
						auditService);
				results = ReportUtils.getReportData(prismContext, task, reportFunctions,
						queryExecutor.getScript(), queryExecutor.getVariables(), objectResolver);
			}

			ObjectListType listType = new ObjectListType();
			for (Object o : results) {
				if (o instanceof PrismObject) {
					listType.getObject().add((ObjectType) ((PrismObject) o).asObjectable());
				} else {
					listType.getObject().add((ObjectType) o);
				}
			}
			return listType;
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

}

package com.evolveum.midpoint.report;

import java.util.LinkedHashMap;
import java.util.List;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;


public class DataSourceReport implements JRDataSource
{
	private ModelReport modelReport;
	private ReportType reportType;
	
	private LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
	
	private List<PrismObject<ObjectType>> data;
	private OperationResult result = null;
	private OperationResult subResult = null;
	private PagingType paging = null;
	private ItemPath fieldPath = null;
	private int rowCounter = -1;
	private int pageOffset = 0;
	private int rowCount = 0;

	
	public DataSourceReport(ReportType reportType, ModelReport modelReport, OperationResult result)
	{
		this.reportType = reportType;
		this.modelReport = modelReport;
		this.result = result;
		initialize();
	}
	
	@Override
	public boolean next() throws JRException {
		try 
		{	
			if (rowCounter == rowCount - 1)			 
			{ 
				subResult = result.createSubresult("Paging");				
				data = modelReport.searchReportObjects(reportType, subResult);
				subResult.computeStatus();
				
				pageOffset += paging.getMaxSize();
				paging.setOffset(pageOffset);
				reportType.getQuery().setPaging(paging);
				rowCounter = 0;
				rowCount  = Math.min(paging.getMaxSize(), data.size());
			}
			else rowCounter++; 
				
			return !data.isEmpty();
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	@Override
	public Object getFieldValue(JRField jrField) throws JRException {
		fieldPath = null;
		if (fieldsPair.containsKey(jrField.getName()))
			fieldPath = fieldsPair.get(jrField.getName());	
		
		if (fieldPath != null)
		{
			PrismObject<ObjectType> record = data.get(rowCounter);
			PrismProperty<?> fieldValue = record.findProperty(fieldPath);
			return  fieldValue != null ? fieldValue.getRealValue().toString() : "";
		}
		else return "";
	}
	
	private void initialize()
	{	
		paging = reportType.getQuery().getPaging();
		rowCount = paging.getMaxSize();
		rowCounter = rowCount - 1;
		fieldsPair = modelReport.getFieldsPair(reportType);
	}
}

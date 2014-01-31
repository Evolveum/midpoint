package com.evolveum.midpoint.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;


public class DataSourceReport implements JRDataSource
{
	private static final Trace LOGGER = TraceManager.getTrace(DataSourceReport.class);
	
	private ModelService modelService;
	
    private PrismContext prismContext;
	
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

	
	public DataSourceReport(ReportType reportType, OperationResult result, PrismContext prismContext, ModelService modelService)
	{
		this.reportType = reportType;
		this.result = result;
		this.prismContext = prismContext;
		this.modelService = modelService;
		initialize();
	}
	
	private void initialize()
	{	
		subResult = result.createSubresult("Initialize");	
		paging = new PagingType();
		paging.setOffset(0);
		paging.setMaxSize(50);
		reportType.getQuery().setPaging(paging);
		rowCount = paging.getMaxSize();
		rowCounter = rowCount - 1;
		fieldsPair = getFieldsPair();
		subResult.computeStatus();
	}
	
	private LinkedHashMap<String, ItemPath> getFieldsPair()
	{
    	LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
	    // pair fields in the report with fields in repo
	    for (ReportFieldConfigurationType fieldRepo : reportType.getReportField())
	   	{
	   		fieldsPair.put(fieldRepo.getNameReportField(), new XPathHolder(fieldRepo.getItemPathField()).toItemPath());
	   	}	
	   	return fieldsPair;
	}

	
	private <T extends ObjectType> List<PrismObject<T>> searchReportObjects() throws Exception
	{
		List<PrismObject<T>> listReportObjects =  new ArrayList<PrismObject<T>>();;
		try
		{
			Class<T> clazz = (Class<T>) ObjectTypes.getObjectTypeFromTypeQName(reportType.getObjectClass()).getClassDefinition();
			ObjectQuery objectQuery = QueryJaxbConvertor.createObjectQuery(clazz, reportType.getQuery(), prismContext);
			LOGGER.trace("Search report objects {}:", reportType);
			
			listReportObjects = modelService.searchObjects(clazz, objectQuery, SelectorOptions.createCollection(GetOperationOptions.createRaw()), null, result);
			return listReportObjects;
		}
		catch (Exception ex) 
		{
			LOGGER.error("Search report objects {}:", ex);
			throw ex;
        }
		
	}
	@Override
	public boolean next() throws JRException {
		try 
		{	
			if (rowCounter == rowCount - 1)			 
			{ 
				subResult = result.createSubresult("Paging");				
				data = searchReportObjects();
				subResult.computeStatus();
				LOGGER.trace("Select next report objects {}:", data);
				pageOffset += paging.getMaxSize();
				paging.setOffset(pageOffset);
				reportType.getQuery().setPaging(paging);
				rowCounter = 0;
				rowCount  = Math.min(paging.getMaxSize(), data.size());
				LOGGER.trace("Set next select paging {}:", paging);
				if (pageOffset == 5950)
				{
					String blb = "blb";
				}
			}
			else rowCounter++; 
				
			return !data.isEmpty();
		}
		catch (Exception ex)
		{
			LOGGER.error("An error has occurred while loading the records into a report - {}:", ex);
			throw new JRException(ex.getMessage());
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
			LOGGER.trace("Select next field value {}:", fieldValue);
			return  fieldValue != null ? fieldValue.getRealValue().toString() : "";
		}
		else return "";
	}
	
	
	
	
}

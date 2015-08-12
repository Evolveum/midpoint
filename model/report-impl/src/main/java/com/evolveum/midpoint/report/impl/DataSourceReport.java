package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperReport;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;


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
	private ObjectPaging paging = null;
	private ItemPath fieldPath = null;
	private int rowCounter = -1;
	private int pageOffset = 0;
	private int rowCount = 0;
	private ObjectQuery objectQuery = null;
	private Class clazz = null;
	private Map params = null;
	
	public DataSourceReport(Map params, PrismContext prismContext, ModelService modelService)
	{
		this.params = params;
		this.prismContext = prismContext;
		this.modelService = modelService;
		initialize();
	}
	
	private void initialize(){}
//	{	clazz = ReportUtils.getObjectTypeClass(params);
//		objectQuery = ReportUtils.getObjectQuery(params, clazz, prismContext);
//		result = ReportUtils.getOperationResult(params);
//		try
//		{
//			reportType = ReportUtils.getReport(params, modelService, prismContext);
//		} catch (Exception ex)
//		{
//			LOGGER.error("Report doesn't correct in datasource. {}", ex.getMessage());
//		}
//		subResult = result.createSubresult("Initialize datasource");	
//		paging = ObjectPaging.createPaging(0, 50);
//		objectQuery.setPaging(paging);		
//		rowCount = paging.getMaxSize();
//		rowCounter = rowCount - 1;
//		fieldsPair = getFieldsPair();
//		subResult.computeStatus();
//	}
	
	private LinkedHashMap<String, ItemPath> getFieldsPair()
	{
    	LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
	    // pair fields in the report with fields in repo
	    for (ReportFieldConfigurationType fieldRepo : reportType.getField()) {
            if (fieldRepo.getItemPath() != null) {
	   		    fieldsPair.put(fieldRepo.getNameReport(), fieldRepo.getItemPath().getItemPath());
            }
	   	}	
	   	return fieldsPair;
	}

	
	private <T extends ObjectType> List<PrismObject<T>> searchReportObjects() throws Exception
	{
		List<PrismObject<T>> listReportObjects =  new ArrayList<PrismObject<T>>();;
		try
		{			
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
				objectQuery.setPaging(paging);
				rowCounter = 0;
				rowCount  = Math.min(paging.getMaxSize(), data.size());
				LOGGER.trace("Set next select paging {}:", paging);
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
			if (fieldValue != null) {
				LOGGER.trace("Select next field value : {}, real value : {}", fieldValue, fieldValue.getRealValue().toString());
			} else {
				LOGGER.trace("Select next field value : {}", fieldValue);
			}
			return  fieldValue != null ? fieldValue.getRealValue().toString() : "";
		}
		else return "";
	}
	
	 /**
     * Disposes of this data source.  Nothing to do here, yet
     */
    public void dispose()
    {
            //nothing to do, yet
    }

	
	
}

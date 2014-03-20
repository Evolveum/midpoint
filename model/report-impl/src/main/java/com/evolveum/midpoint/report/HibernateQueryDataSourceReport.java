package com.evolveum.midpoint.report;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

public class HibernateQueryDataSourceReport implements JRDataSource {
   
    private Iterator iterator;   
    private Object currentValue;   
    
    private ReportType reportType;
    private LinkedHashMap<String, Class> fields = new LinkedHashMap<String, Class>();

    
    public HibernateQueryDataSourceReport(List list, ReportType reportType) {   
   
      this.iterator = list.iterator();
      this.reportType = reportType;
      this.fields = getFields();
    }   
    
    private  LinkedHashMap<String, Class> getFields()
	{
    	int countFields = reportType.getField().size();
	    for (int i=0; i<countFields; i++)
	   	{
	    	ReportFieldConfigurationType fieldRepo = reportType.getField().get(i);
	   		//fields.put(fieldRepo.getNameReportField(), ReportUtils.getClassType(fieldRepo.getClassTypeField()));
	   	}	
	   	return fields;
	}

    
    @Override
    public Object getFieldValue(JRField field) throws JRException {   
      Object value = null;   
      int index = getFieldIndex(field.getName());   
      if (index > -1) {   
    	Class classType = fields.values().toArray(new Class[0])[index];
        Object[] values = (Object[])currentValue;  
        if (classType.getName() == ("javax.xml.datatype.XMLGregorianCalendar")) {
        	value = XmlTypeConverter.createXMLGregorianCalendar((Timestamp)values[index]);
  	    }
  		else {
  			value = classType.cast(values[index]);
  		}
           
      }   
      return value;   
    }   
    @Override
    public boolean next() throws JRException {   
      currentValue = iterator.hasNext() ? iterator.next() : null;   
      return (currentValue != null);   
    }   
    
    private int getFieldIndex(String field) {   
      int index = -1;   
      for (int i = 0; i < fields.size(); i++) {   
        if (fields.keySet().toArray()[i].equals(field)) {   
          index = i;   
          break;   
        }   
      }   
      return index;   
    }   
    
  }  
	
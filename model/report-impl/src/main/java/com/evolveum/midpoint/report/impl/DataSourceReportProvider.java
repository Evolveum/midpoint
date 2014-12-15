package com.evolveum.midpoint.report.impl;

import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataSourceProvider;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.base.JRBaseField;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;

@Service
public class DataSourceReportProvider implements JRDataSourceProvider {
		
	@Autowired
	private PrismContext prismContext;
	@Autowired
	private ModelService modelService;
		
	/**
	 * A field is composed basically of three informations: a name, a description and a type. And 
	 * every instance of this class represent a field.
	 * To be sure that our field provide this information we normally should implements the interface 
	 * JRField. To avoid to implement all the methods we extended the class JRBaseField (that already 
	 * implements JRField), redefining only the constructors to adapt to our needs.
	*/
	private class DataSourceField extends JRBaseField{
			
	/**
	 * An optional numerical id of the class, it can be generated automatically or omitted.
	*/
	
	private static final long serialVersionUID = -5570289821891736393L;

	/**
	 * First constructor for the field
	 * 
	 * @param name : name of the field
	 * @param description : description of the field
	 * @param type : type of the field
	*/
	public DataSourceField(String name,String description, Class<?> type){
		this.name = name;
		this.description = description;
		this.valueClass = type;
		this.valueClassName = type.getName();
	}
			
	/**
	 * Second Constructor, the type of the field is supposed to be 
	 * String.
	 * @param name : name of the field
	 * @param description : description of the field
	*/
	public DataSourceField(String name, String description){
		this(name, description, String.class);
		}
	}
	
	/**
	 * Build and return the data adapter that will provide an access to the real 
	 * data that will be used to fill the report. 
	 * In this case will be returned an instance of MyImplementation.
	*/
	@Override
	public JRDataSource create(JasperReport report) throws JRException {
		throw new UnsupportedOperationException("Not implemented.");
		}

		/**
		 * Method used to destroy the data adapter. Some time a data adapter can 
		 * require additional operations when it isn't more needed. For example 
		 * a remote connection should be closed. In this case we don't need to do 
		 * anything since all data is embedded inside the data adapter.
		 */
		@Override
		public void dispose(JRDataSource dataSource) throws JRException {
			((DataSourceReport)dataSource).dispose();
		}

		
		/**
		 * This method return true if the datasource can provide a list of fields used by the 
		 * data adapter, otherwise false. If this return true the method getFileds is used to 
		 * obtain a list of the fields provided.
		 */
		@Override
		public boolean supportsGetFieldsOperation() {
			return true;
		}
		
		/**
		 * Return a list of all the fields this datasource provide. 
		 */
		@Override
		public JRField[] getFields(JasperReport report) throws JRException,
				UnsupportedOperationException {
			//LinkedHashMap<String, Object> datasourceFields = ;
			//JRField field1 = new DataSourceField("Name","The name of an employee");
			//JRField field2 = new DataSourceField("Age","The age of an employee", Integer.class);
			//return new JRField[]{field1, field2};
			return report.getFields();
		}

		/**
		 * Return a list of all the fields in the report. 
		 */
		public JRField[] getReportFields(JasperReport report) throws JRException,
				UnsupportedOperationException {
			return report.getFields();
		}

//		@Override
//		public JRDataSource create(JasperReport report, Map params) {
//			
//			return new DataSourceReport(params, prismContext, modelService);
//			
//		}

	}


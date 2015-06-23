package com.evolveum.midpoint.report.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;

public class MidPointRemoteDataSource implements JRDataSource {

	Collection<? extends ObjectType> resultList = null;
	Iterator<? extends ObjectType> iterator = null;
	ObjectType currentObject = null;

	private ReportPortType reportPort;

	public MidPointRemoteDataSource(Collection<? extends ObjectType> results) {
		this.resultList = results;
		if (results != null) {
			iterator = results.iterator();
		}
	}

	public MidPointRemoteDataSource(Collection<? extends ObjectType> results, ReportPortType reportPort) {
		this.reportPort = reportPort;
		resultList = new ArrayList<>();
		for (ObjectType objType : results) {
			((Collection) resultList).add(objType);
		}
		iterator = resultList.iterator();
	}

	// public MidPointDataSource(ObjectListType results, PrismContext context){
	// resultList = new ArrayList<>();
	// for (ObjectType objType : results.getObject()){
	// PrismObject prism = ((Objectable)objType).asPrismObject();
	// prism.revive(context);
	//
	// resultList.add(prism);
	// }
	// iterator = resultList.iterator();
	// }

	@Override
	public boolean next() throws JRException {
		// TODO Auto-generated method stub
		boolean hasNext = false;
		if (this.iterator != null) {
			hasNext = this.iterator.hasNext();

			if (hasNext) {
				this.currentObject = iterator.next();
			}
		}

		return hasNext;
		// }
		// throw new
		// UnsupportedOperationException("dataSource.next() not supported");
		// return false;
	}

	@Override
	public Object getFieldValue(JRField jrField) throws JRException {
		// TODO Auto-generated method stub
		String fieldName = jrField.getName();
		if (fieldName.equals("oid")) {
			return currentObject.getOid();
		}

		ReportParameterType param = reportPort.getFieldValue(
				QNameUtil.qNameToUri(new QName(fieldName)), currentObject);

		if (param == null) {
			return null;
		}

		if (param.getAny() == null || param.getAny().isEmpty()) {
			return null;
		}

		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance("com.evolveum.midpoint.xml.ns._public.common.common_3");

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			if (param.getAny().size() == 1 && !List.class.isAssignableFrom(jrField.getValueClass())) {
				Object val = param.getAny().iterator().next();
				return unmarshallValue(jrField, val, jaxbUnmarshaller);
			}

			List<Object> values = new ArrayList<>();
			for (Object val : param.getAny()) {
				values.add(unmarshallValue(jrField, val, jaxbUnmarshaller));
			}

			return values;
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			throw new JRException(e);
		}

	}
	
	private Object unmarshallValue(JRField jrField, Object val, Unmarshaller jaxbUnmarshaller) throws JAXBException{
		if (val instanceof Element) {
			if (jrField.getValueClass()!= null && List.class.isAssignableFrom(jrField.getValueClass())){
				Object unmarshalled = jaxbUnmarshaller.unmarshal((Node) val);
				if (unmarshalled instanceof JAXBElement){
					return ((JAXBElement) unmarshalled).getValue();
				}
			}
			JAXBElement jaxb = jaxbUnmarshaller.unmarshal((Node) val, jrField.getValueClass());
			return jaxb.getValue();
		} else if (val instanceof JAXBElement){
			return ((JAXBElement) val).getValue();
		} else {
			return val;
		}
	}

}

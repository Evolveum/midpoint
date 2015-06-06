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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.parser.JaxbDomHack;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.reportPortImpl;

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

		RemoteReportParameterType param = reportPort.getFieldValue(
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

			if (param.getAny().size() == 1) {

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

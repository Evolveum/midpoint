package com.evolveum.midpoint.report.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Holder;
import javax.xml.ws.Provider;
import javax.xml.ws.soap.SOAPFaultException;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.report.api.ReportPort;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.report.report_3.EvaluateAuditScriptResponseType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.EvaluateAuditScriptType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.EvaluateScriptResponseType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.EvaluateScriptType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ProcessReportResponseType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ProcessReportType;

@Service
public class ReportWebServiceRaw implements Provider<DOMSource> {

	private static transient Trace LOGGER = TraceManager.getTrace(ReportWebService.class);

    public static final String NS_SOAP11_ENV = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String NS_SOAP11_ENV_PREFIX = "SOAP-ENV";
    public static final QName SOAP11_FAULT = new QName(NS_SOAP11_ENV, "Fault");
    public static final QName SOAP11_FAULTCODE = new QName("", "faultcode");
    public static final String SOAP11_FAULTCODE_SERVER = NS_SOAP11_ENV_PREFIX + ":Server";
    public static final QName SOAP11_FAULTSTRING = new QName("", "faultstring");
    public static final QName SOAP11_FAULTACTOR = new QName("", "faultactor");
    public static final QName SOAP11_FAULT_DETAIL = new QName("", "detail");
    public static final String ACTOR = "TODO";

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private ReportWebService reportService;

	 @Override
	    public DOMSource invoke(DOMSource request) {
	        try {
	            return invokeAllowingFaults(request);
	        } catch (FaultMessage faultMessage) {
	            try {
	                SOAPFactory factory = SOAPFactory.newInstance();
	                SOAPFault soapFault = factory.createFault();
	                soapFault.setFaultCode(SOAP11_FAULTCODE_SERVER);           // todo here is a constant until we have a mechanism to determine the correct value (client / server)
	                soapFault.setFaultString(faultMessage.getMessage());
	                Detail detail = soapFault.addDetail();
	                serializeFaultMessage(detail, faultMessage);
	                // fault actor?
	                // stack trace of the outer exception (FaultMessage) is unimportant, because it is always created at one place
	                // todo consider providing stack trace of the inner exception
	                //Detail detail = soapFault.addDetail();
	                //detail.setTextContent(getStackTraceAsString(faultMessage));
	                throw new SOAPFaultException(soapFault);
	            } catch (SOAPException e) {
	                throw new RuntimeException("SOAP Exception: " + e.getMessage(), e);
	            }
	        }
	    }

		public DOMSource invokeAllowingFaults(DOMSource request) throws FaultMessage {
	        Node rootNode = request.getNode();
	        Element rootElement;
	        if (rootNode instanceof Document) {
	            rootElement = ((Document) rootNode).getDocumentElement();
	        } else if (rootNode instanceof Element) {
	            rootElement = (Element) rootNode;
	        } else {
//	            throw ws.createIllegalArgumentFault("Unexpected DOM node type: " + rootNode);
	        	throw new FaultMessage("Unexpected DOM node type: " + rootNode);
	        }

	        Object requestObject;
	        try {
	            requestObject = prismContext.parserFor(rootElement).parseRealValue();
	        } catch (SchemaException e) {
	        	throw new FaultMessage("Couldn't parse SOAP request body because of schema exception: " + e.getMessage());
//	            throw ws.createIllegalArgumentFault("Couldn't parse SOAP request body because of schema exception: " + e.getMessage());
	        }

	        Node response;
	        Holder<OperationResultType> operationResultTypeHolder = new Holder<>();
	        SerializationContext ctx=  new SerializationContext(SerializationOptions.createSerializeReferenceNames());
	        try {
	        	if (requestObject instanceof EvaluateScriptType){
	        		EvaluateScriptType s = (EvaluateScriptType) requestObject;
	        		ObjectListType olt = reportService.evaluateScript(s.getScript(), s.getParameters());
	        		EvaluateScriptResponseType sr = new EvaluateScriptResponseType();
	        		sr.setObjectList(olt);
	        		response = prismContext.domSerializer().context(ctx).serializeAnyData(sr, ReportPort.EVALUATE_SCRIPT_RESPONSE);
	        	} else if (requestObject instanceof EvaluateAuditScriptType){
	        		EvaluateAuditScriptType s = (EvaluateAuditScriptType) requestObject;
	        		AuditEventRecordListType olt = reportService.evaluateAuditScript(s.getScript(), s.getParameters());
	        		EvaluateAuditScriptResponseType sr = new EvaluateAuditScriptResponseType();
	        		sr.setObjectList(olt);
	        		response = prismContext.domSerializer().context(ctx).serializeAnyData(sr, ReportPort.EVALUATE_AUDIT_SCRIPT_RESPONSE);
	            } else if (requestObject instanceof ProcessReportType){
	            	ProcessReportType p = (ProcessReportType) requestObject;
	            	ObjectListType olt = reportService.processReport(p.getQuery(), p.getParameters(), p.getOptions());
	            	ProcessReportResponseType pr = new ProcessReportResponseType();
	            	pr.setObjectList(olt);
	            	response = prismContext.domSerializer().context(ctx).serializeAnyData(pr, ReportPort.PROCESS_REPORT_RESPONSE);
	            } else {
	            	throw new FaultMessage("Unsupported request type: " + requestObject);
	            }
	        } catch (SchemaException e) {
	        	throwFault(e, operationResultTypeHolder.value);
	        	// not reached
	        	return null;
	        }

	        // brutal hack for MID-2001 (serializing and parsing eliminates the problem!)
	        //String serialized = DOMUtil.printDom(response).toString();
	        //LOGGER.trace("WEB SERVICE RESPONSE:\n{}", serialized);
	        //response = DOMUtil.parseDocument(serialized);

	        return new DOMSource(response);
	    }

		private void serializeFaultMessage(Detail detail, FaultMessage faultMessage) {
			MiscSchemaUtil.serializeFaultMessage(detail, faultMessage, prismContext, LOGGER);
		}

//	    private DOMSource serializeFaultMessage(FaultMessage faultMessage) {
//	        Element faultElement = DOMUtil.createElement(SOAP11_FAULT);
//	        Element faultCodeElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULTCODE);
//	        faultCodeElement.setTextContent(SOAP11_FAULTCODE_SERVER);           // todo here is a constant until we have a mechanism to determine the correct value (client / server)
//	        Element faultStringElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULTSTRING);
//	        faultStringElement.setTextContent(faultMessage.getMessage());
//	        Element faultActorElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULTACTOR);
//	        faultActorElement.setTextContent("TODO");               // todo
//	        Element faultDetailElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULT_DETAIL);
//	        faultDetailElement.setTextContent(getStackTraceAsString(faultMessage));
//	        return new DOMSource(faultElement.getOwnerDocument());
//	    }

	    private String getStackTraceAsString(FaultMessage faultMessage) {
	        StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
	        faultMessage.printStackTrace(pw);
	        pw.close();
	        return sw.toString();
	    }

	    private void throwFault(Exception ex, OperationResultType resultType) throws FaultMessage {
			if (resultType != null) {
				throw new FaultMessage(ex.getMessage());
//	            ws.throwFault(ex, OperationResult.createOperationResult(resultType));
			} else {
				throw new FaultMessage(ex.getMessage());
//	            ws.throwFault(ex, null);
	        }
		}

}

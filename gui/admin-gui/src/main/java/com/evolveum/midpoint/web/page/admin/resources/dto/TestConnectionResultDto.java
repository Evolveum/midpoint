package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.result.OperationResult;

public class TestConnectionResultDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private String operationName;
	private OperationResultStatus status;
	private String errorMessage;
	
	
	
	
	public TestConnectionResultDto(String operationName, OperationResultStatus status, String errorMessage) {
		super();
		this.operationName = operationName;
		this.status = status;
		this.errorMessage = errorMessage;
	}

	public String getOperationName() {
		return operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public OperationResultStatus getStatus() {
		return status;
	}

	public void setStatus(OperationResultStatus status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

//	public OperationResult getOverall() {
//		return overall;
//	}
//
//	public void setOverall(OperationResult overall) {
//		this.overall = overall;
//	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

//	private OperationResultStatus lastAvailability;
//	private OperationResult overall;
//	private OperationResult confValidation;
//	private OperationResult conInitialization;
//	private OperationResult conConnection;
//	private OperationResult conSanity;
//	private OperationResult conSchema;
//	private OperationResult extra;
//	private String extraName;
//
//	public TestConnectionResultDto(OperationResult result) {
//
//		List<OperationResult> results = result.getSubresults();
//		confValidation = getResultForOperation(ConnectorTestOperation.CONFIGURATION_VALIDATION, results);
//		conInitialization = getResultForOperation(ConnectorTestOperation.CONNECTOR_INITIALIZATION, results);
//		conConnection = getResultForOperation(ConnectorTestOperation.CONNECTOR_CONNECTION, results);
//		conSanity = getResultForOperation(ConnectorTestOperation.CONNECTOR_SANITY, results);
//		conSchema = getResultForOperation(ConnectorTestOperation.CONNECTOR_SCHEMA, results);
//	}
//
//	// public OperationResult getOverall() {
//	// overall = updateOverallStatus();
//	// if (overall == null) {
//	// return OperationResult.UNKNOWN;
//	// }
//	// return overall;
//	// }
//
//	public OperationResult getConfValidation() {
//		return confValidation;
//	}
//
//	public void setConfValidation(OperationResult confValidation) {
//		this.confValidation = confValidation;
//	}
//
//	public OperationResult getConInitialization() {
//		return conInitialization;
//	}
//
//	public void setConInitialization(OperationResult conInitialization) {
//		this.conInitialization = conInitialization;
//	}
//
//	public OperationResult getConConnection() {
//		return conConnection;
//	}
//
//	public void setConConnection(OperationResult conConnection) {
//		this.conConnection = conConnection;
//	}
//
//	public OperationResult getConSanity() {
//		return conSanity;
//	}
//
//	public void setConSanity(OperationResult conSanity) {
//		this.conSanity = conSanity;
//	}
//
//	public OperationResult getConSchema() {
//		return conSchema;
//	}
//
//	public void setConSchema(OperationResult conSchema) {
//		this.conSchema = conSchema;
//	}
//
//	public OperationResult getExtra() {
//		return extra;
//	}
//
//	public void setExtra(OperationResult extra) {
//		this.extra = extra;
//	}
//
//	public String getExtraName() {
//		return extraName;
//	}
//
//	public void setExtraName(String extraName) {
//		this.extraName = extraName;
//	}
//
//	public OperationResultStatus getLastAvailability() {
//		if (lastAvailability == null) {
//			return OperationResultStatus.UNKNOWN;
//		}
//		return lastAvailability;
//	}
//
//	public void setLastAvailability(OperationResultStatus lastAvailability) {
//		this.lastAvailability = lastAvailability;
//	}
//
//	private static OperationResult getResultForOperation(ConnectorTestOperation operation,
//			List<OperationResult> results) {
//		for (OperationResult result : results) {
//			if (operation.getOperation().equals(result.getOperation())) {
//				return result;
//			}
//
//		}
//		return null;
//	}
//
//	// private OperationResult updateOverallStatus() {
//	// OperationResult overall = OperationResult.UNKNOWN;
//	// overall = getOverallBasedOnPartialStatus(overall, getConConnection());
//	// overall = getOverallBasedOnPartialStatus(overall, getConfValidation());
//	// overall = getOverallBasedOnPartialStatus(overall,
//	// getConInitialization());
//	// overall = getOverallBasedOnPartialStatus(overall, getConSanity());
//	// overall = getOverallBasedOnPartialStatus(overall, getConSchema());
//	// overall = getOverallBasedOnPartialStatus(overall, getExtra());
//	//
//	// return overall;
//	// }
//
//	// private OperationResult getOverallBasedOnPartialStatus(OperationResult
//	// overall,
//	// OperationResult partial) {
//	// switch (overall) {
//	// case UNKNOWN:
//	// case SUCCESS:
//	// if (!OperationResult.UNKNOWN.equals(partial)) {
//	// overall = partial;
//	// }
//	// break;
//	// case WARNING:
//	// if (!OperationResult.UNKNOWN.equals(partial) &&
//	// !OperationResult.SUCCESS.equals(partial)) {
//	// overall = partial;
//	// }
//	// break;
//	// case FATAL_ERROR:
//	// break;
//	// case PARTIAL_ERROR:
//	// break;
//	// case HANDLED_ERROR:
//	// break;
//	// case IN_PROGRESS:
//	// break;
//	// }
//	//
//	// return overall;
//	// }
//
}

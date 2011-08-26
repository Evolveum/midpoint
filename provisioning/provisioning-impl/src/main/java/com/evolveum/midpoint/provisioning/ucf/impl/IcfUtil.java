/**
 * 
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import java.net.ConnectException;

import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.SchemaViolationException;

import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;

/**
 * Set of utility methods that work around some of the ICF problems.
 * 
 * @author Radovan Semancik
 *
 */
class IcfUtil {
	
	/**
	 * Transform ICF exception to something more usable.
	 * 
	 * WARNING: This is black magic. Really. Blame ICF interface design.
	 * 
	 * @param ex
	 *            exception from the ICF
	 * @param parentResult
	 *            OperationResult to record failure
	 * @return reasonable midPoint exception
	 */
	static Exception processIcfException(Exception ex,
			OperationResult parentResult) {
		// Whole exception handling in this case is a black magic.
		// ICF does not define any exceptions and there is no "best practice"
		// how to handle ICF errors
		// Therefore let's just guess what might have happened. That's the best
		// we can do.

		// Introspect the inner exceptions and look for known causes
		Exception knownCause = lookForKnownCause(ex, ex, parentResult);
		if (knownCause != null) {
			return knownCause;
		}

		// Otherwise try few obvious things
		if (ex instanceof IllegalArgumentException) {
			// This is most likely missing attribute or similar schema thing
			parentResult.recordFatalError("Schema violation", ex);
			return new SchemaException("Schema violation (most likely): "
					+ ex.getMessage(), ex);

		} else if (ex instanceof ConnectorSecurityException) {
			// Note: connection refused is also packed inside
			// ConnectorSecurityException. But that will get addressed by the
			// lookForKnownCause(..) before
			parentResult.recordFatalError(
					"Security violation: " + ex.getMessage(), ex);
			// Maybe we need special exception for security?
			return new SystemException(
					"Security violation: " + ex.getMessage(), ex);
		}
		// Fallback
		parentResult.recordFatalError(ex);
		return new GenericFrameworkException(ex);
	}

	private static Exception lookForKnownCause(Throwable ex,
			Throwable originalException, OperationResult parentResult) {
		if (ex instanceof NameAlreadyBoundException) {
			// This is thrown by LDAP connector and may be also throw by similar
			// connectors
			parentResult.recordFatalError("Object already exists", ex);
			return new ObjectAlreadyExistsException(ex.getMessage(),
					originalException);
		} else if (ex instanceof SchemaViolationException) {
			// This is thrown by LDAP connector and may be also throw by similar
			// connectors
			parentResult.recordFatalError("Schema violation", ex);
			return new SchemaException(ex.getMessage(), originalException);
		} else if (ex instanceof ConnectException) {
			// Buried deep in many exceptions, usually connection refused or
			// similar errors
			parentResult.recordFatalError("Connect error: " + ex.getMessage(),
					ex);
			return new CommunicationException("Connect error: "
					+ ex.getMessage(), ex);
		}
		if (ex.getCause() == null) {
			// found nothing
			return null;
		} else {
			// Otherwise go one level deeper ...
			return lookForKnownCause(ex.getCause(), originalException,
					parentResult);
		}
	}



}

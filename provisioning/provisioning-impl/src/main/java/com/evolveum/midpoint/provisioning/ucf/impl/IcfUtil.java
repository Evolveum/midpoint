/**
 * 
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.Set;

import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.SchemaViolationException;

import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Set of utility methods that work around some of the ICF problems.
 * 
 * @author Radovan Semancik
 *
 */
class IcfUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(IcfUtil.class);
	
	/**
	 * Transform ICF exception to something more usable.
	 *
	 * ICF throws exceptions that contains inner exceptions that cannot be
	 * reached by current classloader. Such inner exceptions may cause a lot
	 * of problems in upper layers, such as attempt to serialize/deserialize
	 * them. Therefore we cannot pass such exceptions to the upper layers.
	 * As Throwable is immutable and there is no good way how to copy it, we
	 * just cannot remove the "bad" exceptions from the inner exception stack.
	 * We need to do the brutal thing: remove all the ICF exceptions and do
	 * not pass then to upper layers. Try to save at least some information
	 * and "compress" the class names and messages of the inner ICF exceptions.
	 * The full exception with a stack trace is logger here, so the details are
	 * still in the log.
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
		
		if (ex == null) {
			throw new IllegalArgumentException("Null exception while processing ICF exception ");
		}
		
		LOGGER.error("ICF Exception {}: {}",new Object[]{ex.getClass().getName(),ex.getMessage(),ex});
		
		if (parentResult == null) {
			throw new IllegalArgumentException(createMessage("Null parent result while processing ICF exception",ex));
		}

		// Introspect the inner exceptions and look for known causes
		Exception knownCause = lookForKnownCause(ex, ex, parentResult);
		if (knownCause != null) {
			return knownCause;
		}

		// Otherwise try few obvious things
		if (ex instanceof IllegalArgumentException) {
			// This is most likely missing attribute or similar schema thing
			Exception newEx = new SchemaException(createMessage("Schema violation (most likely)", ex));
			parentResult.recordFatalError("Schema violation: "+ex.getMessage(), newEx);
			return newEx;
			
		} else if (ex instanceof ConfigurationException) {
			Exception newEx = new SchemaException(createMessage("Configuration error", ex));
			parentResult.recordFatalError("Configuration error: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof AlreadyExistsException) {
			Exception newEx = new ObjectAlreadyExistsException(createMessage(null, ex));
			parentResult.recordFatalError("Object already exists: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof ConnectionBrokenException) {
			Exception newEx = new CommunicationException(createMessage("Connection broken", ex));
			parentResult.recordFatalError("Connection broken: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof ConnectionFailedException) {
			Exception newEx = new CommunicationException(createMessage("Connection failed", ex));
			parentResult.recordFatalError("Connection failed: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof ConnectorIOException) {
			Exception newEx = new CommunicationException(createMessage("IO error", ex));
			parentResult.recordFatalError("IO error: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof InvalidCredentialException) {
			Exception newEx = new GenericFrameworkException(createMessage("Invalid credentials", ex));
			parentResult.recordFatalError("Invalid credentials: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof OperationTimeoutException) {
			Exception newEx = new CommunicationException(createMessage("Operation timed out", ex));
			parentResult.recordFatalError("Operation timed out: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof UnknownUidException) {
			Exception newEx = new ObjectNotFoundException(createMessage(null, ex));
			parentResult.recordFatalError("Unknown UID: "+ex.getMessage(), newEx);
			return newEx;

		} else if (ex instanceof ConnectorSecurityException) {
			// Note: connection refused is also packed inside
			// ConnectorSecurityException. But that will get addressed by the
			// lookForKnownCause(..) before
			
			// Maybe we need special exception for security?
			Exception newEx =  new SystemException(createMessage("Security violation",ex));
			parentResult.recordFatalError(
					"Security violation: " + ex.getMessage(), newEx);
			return newEx;
			
		} else if (ex instanceof NullPointerException && ex.getMessage() != null) {
			// NPE with a message text is in fact not a NPE but an application exception
			// this usually means that some parameter is missing
			Exception newEx = new SchemaException(createMessage("Required attribute is missing",ex));  
			parentResult.recordFatalError("Required attribute is missing: "+ex.getMessage(),newEx);
			return newEx;
		}
		
		// Fallback
		Exception newEx = new GenericFrameworkException(createMessage(null,ex)); 
		parentResult.recordFatalError(newEx);
		return newEx;
	}

	private static Exception lookForKnownCause(Throwable ex,
			Throwable originalException, OperationResult parentResult) {
		if (ex instanceof FileNotFoundException) {
			Exception newEx = new GenericFrameworkException(createMessage(null, originalException));
			parentResult.recordFatalError("File not found: "+ex.getMessage(), newEx);
			return newEx;
		} else if (ex instanceof NameAlreadyBoundException) {
			// This is thrown by LDAP connector and may be also throw by similar
			// connectors
			Exception newEx = new ObjectAlreadyExistsException(createMessage(null, originalException));
			parentResult.recordFatalError("Object already exists: "+ex.getMessage(), newEx);
			return newEx;
		} else if (ex instanceof SchemaViolationException) {
			// This is thrown by LDAP connector and may be also throw by similar
			// connectors
			Exception newEx = new SchemaException(createMessage(null,originalException)); 
			parentResult.recordFatalError("Schema violation: "+ex.getMessage(), newEx);
			return newEx;
		} else if (ex instanceof ConnectException) {
			// Buried deep in many exceptions, usually connection refused or
			// similar errors
			Exception newEx = new CommunicationException(createMessage("Connect error", ex));
			parentResult.recordFatalError("Connect error: " + ex.getMessage(), newEx);
			return newEx;
		} else if (ex instanceof SQLSyntaxErrorException) {
			// Buried deep in many exceptions, usually DB schema problems of
			// DB-based connectors
			Exception newEx = new SchemaException(createMessage("DB syntax error", ex));
			parentResult.recordFatalError("DB syntax error: " + ex.getMessage(), newEx);
			return newEx;
		} else if (ex instanceof SQLException) {
			// Buried deep in many exceptions, usually DB connection problems
			Exception newEx = new GenericFrameworkException(createMessage("DB error", ex));
			parentResult.recordFatalError("DB error: " + ex.getMessage(), newEx);
			return newEx;		
		} else if (ex instanceof UnknownUidException) {
			// Object not found
			Exception newEx = new ObjectNotFoundException(createMessage(null,ex));
			parentResult.recordFatalError("Object not found: "+ex.getMessage(), newEx);
			return newEx;
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

	public static String dump(Set<Attribute> attributes) {
		StringBuilder sb = new StringBuilder();
		for (Attribute attr : attributes) {
			sb.append(attr.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	
	private static String createMessage(String prefix, Throwable ex) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) {
			sb.append(prefix);
			sb.append(": ");
		}
		addExceptionToMessage(sb,ex);
		return sb.toString();
	}
	
	private static void addExceptionToMessage(StringBuilder sb, Throwable ex) {
		sb.append(ex.getClass().getName());
		sb.append("(");
		sb.append(ex.getMessage());
		sb.append(")");
		if (ex.getCause() != null) {
			sb.append("->");
			addExceptionToMessage(sb, ex.getCause());
		}
	}

}

package com.evolveum.midpoint.provisioning.integration.identityconnector.logging;

import org.identityconnectors.common.logging.Log.Level;
import org.identityconnectors.common.logging.LogSpi;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

public class Slf4jConnectorLogger implements LogSpi {


	@Override
	public void log(Class<?> clazz, String method, Level level, String message,
			Throwable ex) {
		Trace trace = TraceManager.getTrace(clazz);

		if (Level.OK.equals(level)) {
			trace.trace("method: {} \t{}", new Object[]{method, message}, ex);
		} else if (Level.INFO.equals(level)) {
			trace.info("method: {} \t{}", new Object[]{method, message}, ex);
		} else if (Level.WARN.equals(level)) {
			trace.warn("method: {} \t{}", new Object[]{method, message}, ex);
		} else if (Level.ERROR.equals(level)) {
			trace.error("method: {} \t{}", new Object[]{method, message}, ex);
		}

	}

	@Override
	public boolean isLoggable(Class<?> clazz, Level level) {
		return true;
	}

	

}

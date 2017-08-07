/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl.api.transports;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NamedConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
public class TransportUtil {

    static void appendToFile(String filename, String text) throws IOException {
        FileWriter fw = new FileWriter(filename, true);
        fw.append(text);
        fw.close();
    }

	public static <T extends NamedConfigurationType> T getTransportConfiguration(String transportName, String baseTransportName,
			Function<NotificationConfigurationType, List<T>> getter, RepositoryService cacheRepositoryService,
			OperationResult result) {

		SystemConfigurationType systemConfiguration = NotificationFunctionsImpl.getSystemConfiguration(cacheRepositoryService, result);
		if (systemConfiguration == null || systemConfiguration.getNotificationConfiguration() == null) {
			return null;
		}

		String transportConfigName = transportName.length() > baseTransportName.length() ? transportName.substring(baseTransportName.length() + 1) : null;      // after e.g. "sms:" or "file:"
		for (T namedConfiguration: getter.apply(systemConfiguration.getNotificationConfiguration())) {
			if ((transportConfigName == null && namedConfiguration.getName() == null) || (transportConfigName != null && transportConfigName.equals(namedConfiguration.getName()))) {
				return namedConfiguration;
			}
		}
		return null;
	}

	public static void appendToFile(String fileName, String messageText, Trace logger, OperationResult result) {
		try {
			TransportUtil.appendToFile(fileName, messageText);
			result.recordSuccess();
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(logger, "Couldn't write the notification to a file {}", t, fileName);
			result.recordPartialError("Couldn't write the notification to a file " + fileName, t);
		}
	}

	public static void logToFile(String fileName, String messageText, Trace logger) {
		try {
			TransportUtil.appendToFile(fileName, messageText);
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(logger, "Couldn't write the notification to a file {}", t, fileName);
		}
	}

	public static String formatToFileOld(Message message) {
		return "============================================ " + "\n" +new Date() + "\n" + message.toString() + "\n\n";
	}

	public static String formatToFileNew(Message message, String transport) {
		return "================ " + new Date() + " ======= [" + transport + "]\n" + message.debugDump() + "\n\n";
	}

}

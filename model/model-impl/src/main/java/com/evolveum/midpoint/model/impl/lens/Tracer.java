/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
@Component
public class Tracer {

	private static final Trace LOGGER = TraceManager.getTrace(Tracer.class);

	@Autowired private PrismContext prismContext;

	private static final String MIDPOINT_HOME = System.getProperty("midpoint.home");
	private static final String TRACE_DIR = MIDPOINT_HOME + "trace/";

	void storeTrace(OperationResult result) {
		File file = createFileName();
		try {
			OperationResultType resultBean = result.createOperationResultType();
			String xml = prismContext.xmlSerializer().serializeRealValue(resultBean);
			try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
				pw.write(xml);
				LOGGER.info("Trace was written to {} ({} chars)", file, xml.length());
			}
		} catch (IOException | SchemaException | RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't write trace to file {}", e, file);
		}
	}

	@NotNull
	private File createFileName() {
		File traceDir = new File(TRACE_DIR);
		if (!traceDir.exists() || !traceDir.isDirectory()) {
			if (!traceDir.mkdir()) {
				LOGGER.warn("Attempted to create trace directory but failed: {}", traceDir);
			}
		}
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		String fileName = String.format("trace-%s.xml", df.format(new Date()));
		return new File(traceDir, fileName);
	}
}

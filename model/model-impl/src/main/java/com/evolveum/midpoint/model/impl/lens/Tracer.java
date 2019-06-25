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

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Component
public class Tracer {

	private static final Trace LOGGER = TraceManager.getTrace(Tracer.class);
	private static final String MACRO_TIMESTAMP = "timestamp";
	private static final String MACRO_TEST_NAME = "testName";
	private static final String MACRO_TEST_NAME_SHORT = "testNameShort";
	private static final String MACRO_FOCUS_NAME = "focusName";
	private static final String MACRO_MILLISECONDS = "milliseconds";
	private static final String MACRO_RANDOM = "random";

	@Autowired private PrismContext prismContext;
	@Autowired private SystemObjectCache systemObjectCache;
	@Autowired private TaskManager taskManager;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService repositoryService;

	private static final String MIDPOINT_HOME = System.getProperty("midpoint.home");
	private static final String TRACE_DIR = MIDPOINT_HOME + "trace/";
	private static final String ZIP_ENTRY_NAME = "trace.xml";

	private static final String DEFAULT_FILE_NAME_PATTERN = "trace-%timestamp";

	void storeTrace(Task task, OperationResult result) {
		TracingProfileType tracingProfile = result.getTracingProfile();
		boolean zip = !Boolean.FALSE.equals(tracingProfile.isCompressOutput());
		Map<String, String> templateParameters = createTemplateParameters(task, result);      // todo evaluate lazily if needed
		File file = createFileName(zip, tracingProfile, templateParameters);
		try {
			OperationResultType resultBean = result.createOperationResultType();
			String xml = prismContext.xmlSerializer().serializeRealValue(resultBean);
			if (zip) {
				MiscUtil.writeZipFile(file, ZIP_ENTRY_NAME, xml, StandardCharsets.UTF_8);
				LOGGER.info("Trace was written to {} ({} chars uncompressed)", file, xml.length());
			} else {
				try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
					pw.write(xml);
					LOGGER.info("Trace was written to {} ({} chars)", file, xml.length());
				}
			}
			if (!Boolean.FALSE.equals(tracingProfile.isCreateRepoObject())) {
				ReportOutputType reportOutputObject = new ReportOutputType(prismContext)
						.name(createObjectName(tracingProfile, templateParameters))
						.archetypeRef(SystemObjectsType.ARCHETYPE_TRACE.value(), ArchetypeType.COMPLEX_TYPE)
						.filePath(file.getAbsolutePath())
						.nodeRef(ObjectTypeUtil.createObjectRef(taskManager.getLocalNode(), prismContext));
				repositoryService.addObject(reportOutputObject.asPrismObject(), null, result);
			}
		} catch (IOException | SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't write trace ({})", e, file);
			throw new SystemException(e);
		}
	}

	private Map<String, String> createTemplateParameters(Task task, OperationResult result) {
		Map<String, String> rv = new HashMap<>();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		rv.put(MACRO_TIMESTAMP, df.format(new Date()));
		String testName = task.getResult().getOperation();
		rv.put(MACRO_TEST_NAME, testName);  // e.g. com.evolveum.midpoint.model.intest.TestIteration.test532GuybrushModifyDescription
		int testNameIndex = StringUtils.lastOrdinalIndexOf(testName, ".", 2);
		rv.put(MACRO_TEST_NAME_SHORT, testNameIndex >= 0 ? testName.substring(testNameIndex+1) : testName);
		rv.put(MACRO_FOCUS_NAME, getFocusName(result));
		rv.put(MACRO_MILLISECONDS, getMilliseconds(result));
		rv.put(MACRO_RANDOM, String.valueOf((long) (Math.random() * 1000000000000000L)));
		return rv;
	}

	private String getFocusName(OperationResult result) {
		ClockworkRunTraceType trace = result.getFirstTrace(ClockworkRunTraceType.class);
		if (trace != null && trace.getFocusName() != null) {
			return trace.getFocusName();
		} else {
			return "unknown";
		}
	}

	private String getMilliseconds(OperationResult result) {
		if (result.getMicroseconds() != null) {
			return String.valueOf(result.getMicroseconds() / 1000);
		} else if (result.getStart() != null && result.getEnd() != null) {
			return String.valueOf(result.getEnd() - result.getStart());
		} else {
			return "unknown";
		}
	}

	private String createObjectName(TracingProfileType profile, Map<String, String> parameters) {
		String pattern;
		if (profile.getObjectNamePattern() != null) {
			pattern = profile.getObjectNamePattern();
		} else if (profile.getFileNamePattern() != null) {
			pattern = profile.getFileNamePattern();
		} else {
			pattern = DEFAULT_FILE_NAME_PATTERN;
		}
		return expandMacros(pattern, parameters);
	}

	@NotNull
	private File createFileName(boolean zip, TracingProfileType profile, Map<String, String> parameters) {
		File traceDir = new File(TRACE_DIR);
		if (!traceDir.exists() || !traceDir.isDirectory()) {
			if (!traceDir.mkdir()) {
				LOGGER.warn("Attempted to create trace directory but failed: {}", traceDir);
			}
		}
		String pattern = profile.getFileNamePattern() != null ? profile.getFileNamePattern() : DEFAULT_FILE_NAME_PATTERN;
		return new File(traceDir, normalizeFileName(expandMacros(pattern, parameters)) + (zip ? ".zip" : ".xml"));
	}

	private String normalizeFileName(String name) {
		return name.replaceAll("[^a-zA-Z0-9 .-]", "_");
	}

	private String expandMacros(String pattern, Map<String, String> parameters) {
		StringBuilder sb = new StringBuilder();
		for (int current = 0;;) {
			int i = pattern.indexOf("%{", current);
			if (i < 0) {
				sb.append(pattern.substring(current));
				return sb.toString();
			}
			sb.append(pattern.substring(current, i));
			int j = pattern.indexOf("}", i);
			if (j < 0) {
				LOGGER.warn("Missing '}' in pattern '{}'", pattern);
				return sb.toString() + " - error - " + parameters.get(MACRO_RANDOM);
			} else {
				String macroName = pattern.substring(i+2, j);
				String value = parameters.get(macroName);
				if (value == null) {
					LOGGER.warn("Unknown parameter '{}' in pattern '{}'", macroName, pattern);
				}
				sb.append(value);
			}
			current = j+1;
		}
	}

	public TracingProfileType resolve(TracingProfileType tracingProfile, OperationResult result) throws SchemaException {
		if (tracingProfile == null) {
			return null;
		} else if (tracingProfile.getRef().isEmpty()) {
			return tracingProfile;
		} else {
			PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
			TracingConfigurationType tracingConfiguration = systemConfiguration != null && systemConfiguration.asObjectable().getInternals() != null ?
					systemConfiguration.asObjectable().getInternals().getTracing() : null;
			List<String> resolutionPath = new ArrayList<>();
			return resolveProfile(tracingProfile, tracingConfiguration, resolutionPath);
		}
	}

	@NotNull
	private TracingProfileType resolveProfile(TracingProfileType tracingProfile, TracingConfigurationType tracingConfiguration,
			List<String> resolutionPath) throws SchemaException {
		if (tracingProfile.getRef().isEmpty()) {
			return tracingProfile;
		} else {
			TracingProfileType rv = new TracingProfileType();
			for (String ref : tracingProfile.getRef()) {
				merge(rv, getResolvedProfile(ref, tracingConfiguration, resolutionPath));
			}
			merge(rv, tracingProfile);
			return rv;
		}
	}

	private TracingProfileType getResolvedProfile(String ref, TracingConfigurationType tracingConfiguration,
			List<String> resolutionPath) throws SchemaException {
		if (resolutionPath.contains(ref)) {
			throw new IllegalStateException("A cycle in tracing profile resolution path: " + resolutionPath + " -> " + ref);
		}
		resolutionPath.add(ref);
		TracingProfileType profile = findProfile(ref, tracingConfiguration);
		resolutionPath.remove(resolutionPath.size()-1);
		TracingProfileType rv = resolveProfile(profile, tracingConfiguration, resolutionPath);
		LOGGER.info("Resolved '{}' into:\n{}", ref, rv.asPrismContainerValue().debugDump());
		return rv;
	}

	private TracingProfileType findProfile(String name, TracingConfigurationType tracingConfiguration) throws SchemaException {
		List<TracingProfileType> matching = tracingConfiguration.getProfile().stream()
				.filter(p -> name.equals(p.getName()))
				.collect(Collectors.toList());
		if (matching.isEmpty()) {
			throw new SchemaException("Tracing profile '" + name + "' is referenced but not defined. Known names: "
					+ tracingConfiguration.getProfile().stream().map(TracingProfileType::getName).collect(Collectors.joining(", ")));
		} else if (matching.size() == 1) {
			return matching.get(0);
		} else {
			throw new SchemaException("More than one profile with the name '" + name + "' exist.");
		}
	}

	// fixme experimental ... not thought out very much - this method will probably work only for non-overlapping profiles
	private void merge(TracingProfileType summary, TracingProfileType delta) throws SchemaException {
		//noinspection unchecked
		summary.asPrismContainerValue().mergeContent(delta.asPrismContainerValue(), Collections.emptyList());
	}
}

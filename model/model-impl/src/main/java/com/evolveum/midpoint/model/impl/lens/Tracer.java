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
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Component
public class Tracer {

	private static final Trace LOGGER = TraceManager.getTrace(Tracer.class);

	@Autowired private PrismContext prismContext;
	@Autowired private SystemObjectCache systemObjectCache;
	@Autowired private TaskManager taskManager;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService repositoryService;

	private static final String MIDPOINT_HOME = System.getProperty("midpoint.home");
	private static final String TRACE_DIR = MIDPOINT_HOME + "trace/";
	private static final String ZIP_ENTRY_NAME = "trace.xml";

	void storeTrace(OperationResult result) {
		boolean zip = !Boolean.FALSE.equals(result.getTracingProfile().isCompressOutput());
		File file = createFileName(zip);
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
			if (!Boolean.FALSE.equals(result.getTracingProfile().isCreateRepoObject())) {
				ReportOutputType reportOutputObject = new ReportOutputType(prismContext)
						.name(file.getName())
						.archetypeRef(SystemObjectsType.ARCHETYPE_TRACE.value(), ArchetypeType.COMPLEX_TYPE)
						.filePath(file.getAbsolutePath())
						.nodeRef(ObjectTypeUtil.createObjectRef(taskManager.getLocalNode(), prismContext));
				repositoryService.addObject(reportOutputObject.asPrismObject(), null, result);
			}
			OperationResultType reparsed = prismContext.parserFor(xml).xml().parseRealValue(OperationResultType.class);
			System.out.println("Reparsed OK: " + reparsed);
		} catch (IOException | SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't write trace ({})", e, file);
			throw new SystemException(e);
		}
	}

	@NotNull
	private File createFileName(boolean zip) {
		File traceDir = new File(TRACE_DIR);
		if (!traceDir.exists() || !traceDir.isDirectory()) {
			if (!traceDir.mkdir()) {
				LOGGER.warn("Attempted to create trace directory but failed: {}", traceDir);
			}
		}
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
		String fileName = String.format("trace-%s.%s", df.format(new Date()), zip ? "zip" : "xml");
		return new File(traceDir, fileName);
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

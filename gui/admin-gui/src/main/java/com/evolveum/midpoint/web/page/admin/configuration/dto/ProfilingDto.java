/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;

/**
 *
 * @author katkav
 *
 */
public class ProfilingDto implements Serializable {

	private boolean profilingEnabled;
	private boolean requestFilter;
	private boolean performanceStatistics;
	private boolean subsystemModel;
	private boolean subsystemRepository;
	private boolean subsystemProvisioning;
	private boolean subsystemUcf;
	private boolean subsystemSynchronizationService;
	private boolean subsystemTaskManager;
	private boolean subsystemWorkflow;
	private Integer dumpInterval;

	private ProfilingLevel profilingLevel;
	private String profilingAppender;

	private List<AppenderConfiguration> appenders;

	public static final String LOGGER_PROFILING = "PROFILING";

	public ProfilingDto() {
		this(null, null);
	}

	public ProfilingDto(ProfilingConfigurationType profiling,
			List<ClassLoggerConfigurationType> classLoggerConfig) {
		init(profiling, classLoggerConfig);
	}

	private void init(ProfilingConfigurationType profilingConfiguration,
			List<ClassLoggerConfigurationType> classLoggerConfig) {

		if (profilingConfiguration != null) {

			profilingEnabled = checkXsdBooleanValue(profilingConfiguration.isEnabled());
			requestFilter = checkXsdBooleanValue(profilingConfiguration.isRequestFilter());
			performanceStatistics = checkXsdBooleanValue(profilingConfiguration.isPerformanceStatistics());
			subsystemModel = checkXsdBooleanValue(profilingConfiguration.isModel());
			subsystemProvisioning = checkXsdBooleanValue(profilingConfiguration.isProvisioning());
			subsystemRepository = checkXsdBooleanValue(profilingConfiguration.isRepository());
			subsystemSynchronizationService = checkXsdBooleanValue(profilingConfiguration.isSynchronizationService());
			subsystemTaskManager = checkXsdBooleanValue(profilingConfiguration.isTaskManager());
			subsystemUcf = checkXsdBooleanValue(profilingConfiguration.isUcf());
			subsystemWorkflow = checkXsdBooleanValue(profilingConfiguration.isWorkflow());

			if (profilingConfiguration.getDumpInterval() != null) {
				dumpInterval = profilingConfiguration.getDumpInterval();
			}

		}
		if (classLoggerConfig == null) {
			return;
		}
		for (ClassLoggerConfigurationType logger : classLoggerConfig) {
			if (LOGGER_PROFILING.equals(logger.getPackage())) {
				setProfilingAppender(logger.getAppender() != null && logger.getAppender().size() > 0
						? logger.getAppender().get(0) : null);
				setProfilingLevel(ProfilingLevel.fromLoggerLevelType(logger.getLevel()));
				continue;
			}
		}

	}

	public ClassLoggerConfigurationType getProfilingClassLogerConfig() {

		if (getProfilingLevel() != null) {
			ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
			type.setPackage(ProfilingDto.LOGGER_PROFILING);
			type.setLevel(ProfilingLevel.toLoggerLevelType(getProfilingLevel()));

			if (StringUtils.isEmpty(getProfilingAppender())) {
				return type;
			}

			if (StringUtils.isNotEmpty(getProfilingAppender())
					|| !(PageSystemConfiguration.ROOT_APPENDER_INHERITANCE_CHOICE
							.equals(getProfilingAppender()))) {
				type.getAppender().add(getProfilingAppender());
			}

			return type;

		}

		return null;
	}

	public ProfilingConfigurationType getNewObject() {

		ProfilingConfigurationType config = new ProfilingConfigurationType();

		if (isProfilingEnabled() || isPerformanceStatistics() || isRequestFilter()
				|| isSubsystemModel() || isSubsystemRepository()
				|| isSubsystemProvisioning() || isSubsystemSynchronizationService() || isSubsystemUcf()
				|| isSubsystemTaskManager() || isSubsystemWorkflow())
			config.setEnabled(true);
		else
			config.setEnabled(false);

		config.setDumpInterval(getDumpInterval());
		config.setPerformanceStatistics(isPerformanceStatistics());
		config.setRequestFilter(isRequestFilter());
		config.setModel(isSubsystemModel());
		config.setProvisioning(isSubsystemProvisioning());
		config.setRepository(isSubsystemRepository());
		config.setUcf(isSubsystemUcf());
		config.setSynchronizationService(isSubsystemSynchronizationService());
		config.setTaskManager(isSubsystemTaskManager());
		config.setWorkflow(isSubsystemWorkflow());

		return config;
	}

	public List<AppenderConfiguration> getAppenders() {
		return appenders;
	}

	public void setAppenders(List<AppenderConfiguration> appenders) {
		this.appenders = appenders;
	}

	private static boolean checkXsdBooleanValue(Boolean value) {
		if (value == null || !value)
			return false;
		else
			return true;
	}

	public boolean isRequestFilter() {
		return requestFilter;
	}

	public void setRequestFilter(boolean requestFilter) {
		this.requestFilter = requestFilter;
	}

	public boolean isPerformanceStatistics() {
		return performanceStatistics;
	}

	public void setPerformanceStatistics(boolean performanceStatistics) {
		this.performanceStatistics = performanceStatistics;
	}

	public boolean isSubsystemModel() {
		return subsystemModel;
	}

	public void setSubsystemModel(boolean subsystemModel) {
		this.subsystemModel = subsystemModel;
	}

	public boolean isSubsystemRepository() {
		return subsystemRepository;
	}

	public void setSubsystemRepository(boolean subsystemRepository) {
		this.subsystemRepository = subsystemRepository;
	}

	public boolean isSubsystemProvisioning() {
		return subsystemProvisioning;
	}

	public void setSubsystemProvisioning(boolean subsystemProvisioning) {
		this.subsystemProvisioning = subsystemProvisioning;
	}

	public boolean isSubsystemUcf() {
		return subsystemUcf;
	}

	public void setSubsystemUcf(boolean subsystemUcf) {
		this.subsystemUcf = subsystemUcf;
	}

	public boolean isSubsystemSynchronizationService() {
		return subsystemSynchronizationService;
	}

	public void setSubsystemSynchronizationService(boolean subsystemSynchronizationService) {
		this.subsystemSynchronizationService = subsystemSynchronizationService;
	}

	public boolean isSubsystemTaskManager() {
		return subsystemTaskManager;
	}

	public void setSubsystemTaskManager(boolean subsystemTaskManager) {
		this.subsystemTaskManager = subsystemTaskManager;
	}

	public boolean isSubsystemWorkflow() {
		return subsystemWorkflow;
	}

	public void setSubsystemWorkflow(boolean subsystemWorkflow) {
		this.subsystemWorkflow = subsystemWorkflow;
	}

	public Integer getDumpInterval() {
		return dumpInterval;
	}

	public void setDumpInterval(Integer dumpInterval) {
		this.dumpInterval = dumpInterval;
	}

	public boolean isProfilingEnabled() {
		return profilingEnabled;
	}

	public void setProfilingEnabled(boolean profilingEnabled) {
		this.profilingEnabled = profilingEnabled;
	}

	public String getProfilingAppender() {
		return profilingAppender;
	}

	public void setProfilingAppender(String profilingAppender) {
		this.profilingAppender = profilingAppender;
	}

	public ProfilingLevel getProfilingLevel() {
		return profilingLevel;
	}

	public void setProfilingLevel(ProfilingLevel profilingLevel) {
		this.profilingLevel = profilingLevel;
	}

}

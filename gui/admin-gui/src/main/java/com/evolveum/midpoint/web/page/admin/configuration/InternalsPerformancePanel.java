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
package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.statistics.CachePerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.MethodsPerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.util.aspect.MethodsPerformanceInformation;
import com.evolveum.midpoint.util.aspect.MethodsPerformanceMonitor;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.model.IModel;

import java.util.Map;

/**
 *
 */
public class InternalsPerformancePanel extends BasePanel<Void> {
	private static final long serialVersionUID = 1L;

	private static final String ID_INFORMATION = "information";

	InternalsPerformancePanel(String id) {
		super(id);
		initLayout();
	}

	private void initLayout() {
		AceEditor informationText = new AceEditor(ID_INFORMATION, new IModel<String>() {
			@Override
			public String getObject() {
				return getStatistics();
			}

			@Override
			public void setObject(String object) {
				// nothing to do here
			}
		});
		informationText.setReadonly(true);
		informationText.setHeight(300);
		informationText.setResizeToMaxHeight(true);
		informationText.setMode(null);
		add(informationText);

	}

	@SuppressWarnings("Duplicates")
	private String getStatistics() {
		StringBuilder sb = new StringBuilder();
		MidPointApplication midPointApplication = MidPointApplication.get();
		if (midPointApplication != null) {
			PerformanceInformation repo = midPointApplication.getRepositoryService()
					.getPerformanceMonitor().getGlobalPerformanceInformation();
			if (repo != null) {
				sb.append("Repository performance information:\n")
						.append(RepositoryPerformanceInformationUtil.format(repo.toRepositoryPerformanceInformationType()))
						.append("\n");
			} else {
				sb.append("Repository performance information is currently not available."
						+ "Please set up repository statistics monitoring in the system configuration.\n\n");
			}
		}
		Map<String, CachePerformanceCollector.CacheData> cache = CachePerformanceCollector.INSTANCE
				.getGlobalPerformanceMap();
		if (cache != null) {
			sb.append("Cache performance information:\n")
					.append(CachePerformanceInformationUtil.format(CachePerformanceInformationUtil.toCachesPerformanceInformationType(cache)))
					.append("\n");
		} else {
			sb.append("Cache performance information is currently not available."
					+ "Please set up cache monitoring in the system configuration.\n\n");
		}
		MethodsPerformanceInformation methods = MethodsPerformanceMonitor.INSTANCE
				.getGlobalPerformanceInformation();
		if (methods != null) {
			sb.append("Methods performance information:\n")
					.append(MethodsPerformanceInformationUtil.format(MethodsPerformanceInformationUtil.toMethodsPerformanceInformationType(methods)))
					.append("\n");
		} else {
			sb.append("Methods performance information is currently not available."
					+ "Please set up performance monitoring in the system configuration.\n\n");
		}
		return sb.toString();
	}
}

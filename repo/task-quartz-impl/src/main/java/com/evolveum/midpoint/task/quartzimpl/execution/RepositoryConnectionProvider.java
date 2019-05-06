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

package com.evolveum.midpoint.task.quartzimpl.execution;

import org.quartz.utils.ConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Quartz connection provider that uses built-in midPoint data source factory.
 */
public class RepositoryConnectionProvider implements ConnectionProvider {

	/**
	 * Maybe too cautious; we could probably go forward with a single-valued static dataSource property here.
	 */
	static final Map<Integer, DataSource> dataSources = new ConcurrentHashMap<>();

	private int dataSourceIndex;

	@SuppressWarnings("unused")     // probably called by Quartz
	public int getDataSourceIndex() {
		return dataSourceIndex;
	}

	@SuppressWarnings("unused")     // called by Quartz
	public void setDataSourceIndex(int dataSourceIndex) {
		this.dataSourceIndex = dataSourceIndex;
	}

	@Override
	public Connection getConnection() throws SQLException {
		DataSource dataSource = dataSources.get(dataSourceIndex);
		if (dataSource == null) {
			throw new IllegalStateException("no data source with index " + dataSourceIndex);
		}
		return dataSource.getConnection();
	}

	@Override
	public void shutdown() {
		dataSources.remove(dataSourceIndex);
		// connection pool will be closed on repository shutdown
	}

	@Override
	public void initialize() {
	}
}

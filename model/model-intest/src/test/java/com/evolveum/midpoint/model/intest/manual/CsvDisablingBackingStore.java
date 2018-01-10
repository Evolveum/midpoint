/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.intest.manual;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;

/**
 * @author semancik
 *
 */
public class CsvDisablingBackingStore extends CsvBackingStore {
	
	private static final Trace LOGGER = TraceManager.getTrace(CsvDisablingBackingStore.class);
	
	public CsvDisablingBackingStore() {
		super();
	}
	
	public CsvDisablingBackingStore(File sourceFile, File targetFile) {
		super(sourceFile, targetFile);
	}

	protected void deprovisionInCsv(String username) throws IOException {
		disableInCsv(username);
	}


}

/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.schema.TestConstants.COMMON_DIR_PATH;

/**
 * @author mederly
 */
public abstract class AbstractParserTest {

	protected String language;
	protected boolean namespaces;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@BeforeClass
	@Parameters({ "language", "namespaces" })
	public void temp(@Optional String language, @Optional Boolean namespaces) {
		this.language = language != null ? language : "xml";
		this.namespaces = namespaces != null ? namespaces : Boolean.TRUE;
		System.out.println("Testing with language = " + this.language + ", namespaces = " + this.namespaces);
	}

	protected File getFile(String baseName) {
		return new File(COMMON_DIR_PATH + "/" + language + "/" + (namespaces ? "ns":"no-ns"),
				baseName + "." + language);
	}

	protected void displayTestTitle(String testName) {
		PrismTestUtil.displayTestTitle(testName + " (" + language + ", " + (namespaces ? "with" : "no") + " namespaces)");
	}

	protected abstract File getFile();

	protected PrismContext getPrismContext() {
		return PrismTestUtil.getPrismContext();
	}

}

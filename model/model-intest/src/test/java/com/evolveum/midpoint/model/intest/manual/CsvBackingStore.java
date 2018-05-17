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
public class CsvBackingStore implements BackingStore {
	
	public static final File CSV_SOURCE_FILE = new File(AbstractManualResourceTest.TEST_DIR, "semi-manual.csv");
	public static final File CSV_TARGET_FILE = new File("target/semi-manual.csv");
	
	private static final Trace LOGGER = TraceManager.getTrace(CsvBackingStore.class);
	
	private final File sourceFile;
	private final File targetFile;
	
	public CsvBackingStore() {
		super();
		this.sourceFile = CSV_SOURCE_FILE;
		this.targetFile = CSV_TARGET_FILE;
	}
	
	public CsvBackingStore(File sourceFile, File targetFile) {
		super();
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
	}
	
	@Override
	public void initialize() throws IOException {
		FileUtils.copyFile(sourceFile, targetFile);
	}

	@Override
	public void provisionWill(String interest) throws IOException {
		appendToCsv(new String[]{
				AbstractManualResourceTest.USER_WILL_NAME,
				AbstractManualResourceTest.USER_WILL_FULL_NAME,
				AbstractManualResourceTest.ACCOUNT_WILL_DESCRIPTION_MANUAL,
				interest,
				"false",
				AbstractManualResourceTest.USER_WILL_PASSWORD_OLD
			});
	}

	@Override
	public void updateWill(String newFullName, String interest, ActivationStatusType newAdministrativeStatus,
			String password) throws IOException {
		String disabled;
		if (newAdministrativeStatus == ActivationStatusType.ENABLED) {
			disabled = "false";
		} else {
			disabled = "true";
		}
		replaceInCsv(new String[]{
				AbstractManualResourceTest.USER_WILL_NAME,
				newFullName,
				AbstractManualResourceTest.ACCOUNT_WILL_DESCRIPTION_MANUAL,
				interest,
				disabled,
				password
			});
	}

	@Override
	public void deprovisionWill() throws IOException {
		deprovisionInCsv(AbstractManualResourceTest.USER_WILL_NAME);
	}

	
	@Override
	public void addJack() throws IOException {
		appendToCsv(new String[]{
				AbstractManualResourceTest.USER_JACK_USERNAME,
				AbstractManualResourceTest.USER_JACK_FULL_NAME,
				AbstractManualResourceTest.ACCOUNT_JACK_DESCRIPTION_MANUAL, 
				"", 
				"false",
				AbstractManualResourceTest.USER_JACK_PASSWORD_OLD
			});
	}

	@Override
	public void deleteJack() throws IOException {
		deprovisionInCsv(AbstractManualResourceTest.USER_JACK_USERNAME);
	}
	
	@Override
	public void addPhantom() throws IOException {
		appendToCsv(new String[]{
				AbstractManualResourceTest.USER_PHANTOM_USERNAME,
				// Wrong fullname here ... by purpose. We wonder whether reconciliation fixes this.
				AbstractManualResourceTest.USER_PHANTOM_FULL_NAME_WRONG,
				AbstractManualResourceTest.ACCOUNT_PHANTOM_DESCRIPTION_MANUAL, 
				"", 
				"false",
				AbstractManualResourceTest.ACCOUNT_PHANTOM_PASSWORD_MANUAL
			});
	}

	
	@Override
	public void deleteAccount(String username) throws IOException {
		deleteInCsv(username);
	}

	protected void deprovisionInCsv(String username) throws IOException {
		deleteInCsv(username);
	}

	protected void disableInCsv(String username) throws IOException {
		String[] data = readFromCsv(username);
		data[4] = "true";
		replaceInCsv(data);
	}

	protected String[] readFromCsv(String username) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] cols = line.split(",");
			if (cols[0].matches("\""+username+"\"")) {
				return unescape(cols);
			}
		}
		return null;
	}

	private String[] unescape(String[] cols) {
		String[] out = new String[cols.length];
		for (int i = 0; i < cols.length; i++) {
			if (cols[i] != null && !cols[i].isEmpty()) {
				out[i] = cols[i].substring(1, cols[i].length() - 1);
			}
		}
		return out;
	}

	protected void appendToCsv(String[] data) throws IOException {
		String line = formatCsvLine(data);
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), line.getBytes(), StandardOpenOption.APPEND);
	}

	protected void replaceInCsv(String[] data) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		boolean found = false;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] cols = line.split(",");
			if (cols[0].matches("\""+data[0]+"\"")) {
				lines.set(i, formatCsvLine(data));
				found = true;
			}
		}
		if (!found) {
			throw new IllegalStateException("Not found in CSV: "+data[0]);
		}
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), lines,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	protected void deleteInCsv(String username) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		Iterator<String> iterator = lines.iterator();
		while (iterator.hasNext()) {
			String line = iterator.next();
			String[] cols = line.split(",");
			if (cols[0].matches("\""+username+"\"")) {
				iterator.remove();
			}
		}
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), lines,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private String formatCsvLine(String[] data) {
		return Arrays.stream(data).map(s -> "\""+s+"\"").collect(Collectors.joining(",")) + "\n";
	}

	@Override
	public void displayContent() throws IOException {
		IntegrationTestTools.display("CSV", dumpCsv());
	}

	protected String dumpCsv() throws IOException {
		return StringUtils.join(Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath())), "\n");
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"(" + targetFile + ")";
	}

}

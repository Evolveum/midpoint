/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.bean;

import java.util.Random;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author lazyman
 * 
 */
public class AppenderListItem extends SelectableBean {

	private static final long serialVersionUID = 1965555036713631609L;
	private String name;
	private String pattern = "%date [%X{subsystem}] [%thread] %level \\(%logger\\): %m%n";
	private String filePattern = "${catalina.base}/logs/idm-%{yyyy-MM-dd}.log";
	private AppenderType type;
	private String filePath = "${catalina.base}/logs/idm.log";
	private boolean appending = true;
	private String datePattern = "'.'yyyy-MM-dd";
	private String maxFileSize = "500MB";

	public AppenderListItem cloneItem() {
		AppenderListItem item = new AppenderListItem();
		item.setName(getName());
		item.setPattern(getPattern());
		item.setType(getType());
		item.setFilePath(getFilePath());
		item.setMaxFileSize(getMaxFileSize());
		item.setAppending(isAppending());
		item.setDatePattern(getDatePattern());
		item.setFilePattern(getFilePattern());

		return item;
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	public void setAppending(boolean appending) {
		this.appending = appending;
	}

	public boolean isAppending() {
		return appending;
	}

	public String getName() {
		if (StringUtils.isEmpty(name)) {
			name = "Unknown-" + new Random().nextInt(100);
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public boolean isRollingFileType() {
		return isType(AppenderType.ROLLING_FILE);
	}

	private boolean isType(AppenderType type) {
		return type.equals(getType());
	}

	public AppenderType getType() {
		return type;
	}

	public void setType(AppenderType type) {
		this.type = type;
	}

	public String getTypeString() {
		if (type == null) {
			return null;
		}

		return type.getTitle();
	}

	public void setTypeString(String type) {
		if (type == null) {
			this.type = null;
			return;
		}

		for (AppenderType apType : AppenderType.values()) {
			if (apType.getTitle().equals(type)) {
				this.type = apType;
				return;
			}
		}
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(String maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	/**
	 * @return the filePattern
	 */
	public String getFilePattern() {
		return filePattern;
	}

	/**
	 * @param filePattern the filePattern to set
	 */
	public void setFilePattern(String filePattern) {
		this.filePattern = filePattern;
	}

}

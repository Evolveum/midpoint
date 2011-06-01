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

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author lazyman
 * 
 */
public class AppenderListItem extends SelectableBean {

	private static final long serialVersionUID = 1965555036713631609L;
	private String name;
	private String pattern = "%d{HH:mm:ss,SSS} %-5p [%c] - %m%n";
	private AppenderType type;
	private String filePath;
	private int maxFileSize = 500;

	public AppenderListItem cloneItem() {
		AppenderListItem item = new AppenderListItem();
		item.setName(getName());
		item.setPattern(getPattern());
		item.setType(getType());
		item.setFilePath(getFilePath());
		item.setMaxFileSize(getMaxFileSize());

		return item;
	}

	public String getName() {
		if (StringUtils.isEmpty(name)) {
			name = "Unknown-" + ((int) (Math.random() * 100));
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

	public boolean isFileType() {
		if (AppenderType.FILE.equals(type)) {
			return true;
		}

		return false;
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

	public String getMaxFileSizeString() {
		if (!AppenderType.FILE.equals(type)) {
			return null;
		}

		return Integer.toString(maxFileSize);
	}

	public int getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(int maxFileSize) {
		this.maxFileSize = maxFileSize;
	}
}

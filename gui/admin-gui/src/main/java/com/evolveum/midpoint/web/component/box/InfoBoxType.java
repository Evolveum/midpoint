/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.box;

import java.io.Serializable;

/**
 * @author katkav
 * @author semancik
 */
public class InfoBoxType implements Serializable{
	private static final long serialVersionUID = 1L;

	public static final String ICON_BACKGROUND_COLOR = "iconBackgroundColor";
	public static final String BOX_BACKGROUND_COLOR = "boxBackgroundColor";
	public static final String IMAGE_ID = "imageId";
	public static final String MESSAGE = "message";
	public static final String NUMBER = "number";
	public static final String PROGRESS = "progress";
	public static final String DESCRIPTION = "description";

	private String iconBackgroundColor;
	private String boxBackgroundColor;
	private String imageId;
	private String message;
	private String number;
	private Integer progress;
	private String description;

	public InfoBoxType(String boxBackgroundColor, String imageId, String message) {
		this.boxBackgroundColor = boxBackgroundColor;
		this.imageId = imageId;
		this.message = message;
	}

	public String getIconBackgroundColor() {
		return iconBackgroundColor;
	}

	public void setIconBackgroundColor(String iconBackgroundColor) {
		this.iconBackgroundColor = iconBackgroundColor;
	}

	public String getBoxBackgroundColor() {
		return boxBackgroundColor;
	}

	public void setBoxBackgroundColor(String boxBackgroundColor) {
		this.boxBackgroundColor = boxBackgroundColor;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}

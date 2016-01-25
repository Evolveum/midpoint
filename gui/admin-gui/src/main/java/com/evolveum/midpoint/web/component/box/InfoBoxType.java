package com.evolveum.midpoint.web.component.box;

import java.io.Serializable;
import java.util.List;

public class InfoBoxType implements Serializable{
	
	private String backgroundColor;
	private String imageId;
	private List<String> description;
	
	public InfoBoxType(String backgroundColor, String imageId, List<String> description) {
		this.backgroundColor = backgroundColor;
		this.imageId = imageId;
		this.description = description;
	}
	
	public String getBackgroundColor() {
		return backgroundColor;
	}
	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	public String getImageId() {
		return imageId;
	}
	public void setImageId(String imageId) {
		this.imageId = imageId;
	}
	public List<String> getDescription() {
		return description;
	}
	public void setDescription(List<String> description) {
		this.description = description;
	}
	
	

}

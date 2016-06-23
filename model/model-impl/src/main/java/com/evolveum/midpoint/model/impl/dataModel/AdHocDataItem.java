package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class AdHocDataItem extends DataItem {

	@NotNull private final ItemPath itemPath;

	public AdHocDataItem(@NotNull ItemPath itemPath) {
		this.itemPath = itemPath;
	}

	@Override
	public String getNodeName() {
		return "\"Unresolved: " + itemPath + "\"";
	}

	@Override
	public String getNodeLabel() {
		return String.valueOf(itemPath);
	}

	@Override
	public String getNodeStyleAttributes() {
		return "";
	}
}

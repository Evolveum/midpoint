package com.evolveum.midpoint.model.impl.dataModel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author mederly
 */
public class Relation {

	@NotNull final private List<DataItem> sources;
	@Nullable final private DataItem target;

	public Relation(@NotNull List<DataItem> sources, @Nullable DataItem target) {
		this.sources = sources;
		this.target = target;
	}

	@NotNull
	public List<DataItem> getSources() {
		return sources;
	}

	@Nullable
	public DataItem getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "Relation{" +
				"sources=" + sources +
				", target=" + target +
				'}';
	}

	public String getEdgeLabel() {
		return "";
	}

	public String getNodeLabel(String defaultLabel) {
		return null;
	}

	public String getEdgeStyle() {
		return "";
	}

	public String getNodeStyleAttributes() {
		return "";
	}

	public String getEdgeTooltip() {
		return "";
	}

	public String getNodeTooltip() {
		return "";
	}
}

package com.evolveum.midpoint.repo.sql.data.common.any;

import java.util.Objects;

public abstract class RAnyBase<T> implements RAnyValue<T> {

	private RExtItem item;
	private Integer itemId;

	public Integer getItemId() {
		if (itemId == null && item != null) {
			itemId = item.getId();
		}
		return itemId;
	}

	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RAnyBase))
			return false;
		RAnyBase rAnyBase = (RAnyBase) o;
		return Objects.equals(getItemId(), rAnyBase.getItemId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getItemId());
	}
}

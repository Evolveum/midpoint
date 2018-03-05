package com.evolveum.midpoint.repo.sql.data.common.any;

import java.util.Objects;

public abstract class RAnyBase<T> implements RAnyValue<T> {

	private RExtItem item;
	private Integer itemId;

	@Override
	public String getName() {
		return item.getName();
	}

	@Override
	public String getType() {
		return item.getType();
	}

	@Override
    public RExtItem getItem() {
		return item;
	}

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
    public void setItem(RExtItem item) {
		this.item = item;
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

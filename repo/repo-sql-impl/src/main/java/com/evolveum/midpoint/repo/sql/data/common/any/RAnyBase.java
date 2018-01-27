package com.evolveum.midpoint.repo.sql.data.common.any;

import java.util.Objects;

public abstract class RAnyBase<T> implements RAnyValue<T> {

	private RExtItem item;
	private Long itemId;

	@Override
	public String getName() {
		return item.getName();
	}

	@Override
	public String getType() {
		return item.getType();
	}

	@Override
	public RItemKind getValueType() {
		return item.getKind();
	}

//	@Override
//	public boolean isDynamic() {
//		return item.isDynamic();
//	}

	@Override
	public void setName(String name) {
	}

	@Override
	public void setType(String type) {
	}

	@Override
	public void setValueType(RItemKind valueType) {
	}

//	@Override
//	public void setDynamic(boolean dynamic) {
//	}

	public RExtItem getItem() {
		return item;
	}

	public Long getItemId() {
		if (itemId == null && item != null) {
			itemId = item.getId();
		}
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

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

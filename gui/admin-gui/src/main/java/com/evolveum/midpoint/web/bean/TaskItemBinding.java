package com.evolveum.midpoint.web.bean;

import com.evolveum.midpoint.task.api.TaskBinding;

// this class is currently not used

public enum TaskItemBinding {

	TIGHT("runn.png", "Tight"),

	LOOSE("hourglass.png", "Loose");

	private String icon;
	private String title;

	private TaskItemBinding(String icon, String title) {
		this.icon = icon;
		this.title = title;
	}

	public String getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	public static TaskItemBinding fromTask(TaskBinding binding) {
		if (binding.equals(TaskBinding.LOOSE)) {
			return LOOSE;
		}
		if (binding.equals(TaskBinding.TIGHT)) {
			return TIGHT;
		}
		return null;

	}
	
	public static TaskBinding toTask(TaskItemBinding bindingType) {
		if (bindingType.equals(TaskItemBinding.LOOSE)) {
			return TaskBinding.LOOSE;
		}
		if (bindingType.equals(TaskItemBinding.TIGHT)) {
			return TaskBinding.TIGHT;
		}
		return null;

	}
}

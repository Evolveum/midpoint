package com.evolveum.midpoint.web.component.input;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

public abstract class StringChoiceRenderer implements IChoiceRenderer<String> {
	private static final class Simple extends StringChoiceRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public String getDisplayValue(final String object) {
			return object;
		}

		Object readResolve() {
			return SIMPLE;
		}
	}

	public static class Prefixed extends StringChoiceRenderer {
		private static final long serialVersionUID = 1L;

		private final String keyPrefix;

		public Prefixed(final String keyPrefix) {
			this.keyPrefix = requireNonNull(keyPrefix);
		}

		@Override
		public String getDisplayValue(final String object) {
			return Application.get().getResourceSettings().getLocalizer().getString(keyPrefix + object, null);
		}
	}

	private static final class PrefixedSplit extends Prefixed {
		private static final long serialVersionUID = 1L;

		private final Pattern splitPattern;

		PrefixedSplit(final String keyPrefix, final Pattern splitPattern) {
			super(keyPrefix);
			this.splitPattern = requireNonNull(splitPattern);
		}

		@Override
		public String getDisplayValue(final String object) {
			return super.getDisplayValue(splitPattern.split(object)[1]);
		}
	}

	private static final StringChoiceRenderer SIMPLE = new Simple();

	private static final long serialVersionUID = 2L;

	public static StringChoiceRenderer simple() {
		return SIMPLE;
	}

	public static StringChoiceRenderer prefixed(final String keyPrefix) {
		return StringUtils.isBlank(keyPrefix) ? SIMPLE : new Prefixed(keyPrefix);
	}

	public static StringChoiceRenderer prefixedSplit(final String keyPrefix, final String splitPattern) {
		if (StringUtils.isBlank(splitPattern)) {
			return prefixed(keyPrefix);
		}

		return new PrefixedSplit(StringUtils.isBlank(keyPrefix) ? "" : keyPrefix, Pattern.compile(splitPattern));
	}

	@Override
	public String getObject(final String id, final IModel<? extends List<? extends String>> choices) {
		return StringUtils.isBlank(id) ? null : choices.getObject().get(Integer.parseInt(id));
	}

	@Override
	public String getIdValue(final String object, final int index) {
		return Integer.toString(index);
	}
}

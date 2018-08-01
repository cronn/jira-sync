package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraVersion extends JiraIdResource implements JiraNamedBean {

	private static final long serialVersionUID = 1L;

	private String name;

	public JiraVersion() {
	}

	public JiraVersion(String id, String name) {
		super(id);
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("id", getId())
			.append("name", name)
			.toString();
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof JiraVersion)) {
			return false;
		}

		JiraVersion that = (JiraVersion) o;

		return new EqualsBuilder()
			.append(getId(), that.getId())
			.append(name, that.name)
			.isEquals();
	}

	@Override
	public final int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(getId())
			.append(name)
			.toHashCode();
	}
}

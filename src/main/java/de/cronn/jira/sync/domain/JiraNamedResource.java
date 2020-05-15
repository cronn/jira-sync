package de.cronn.jira.sync.domain;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class JiraNamedResource extends JiraIdResource implements JiraNamedBean {

	private String name;

	public JiraNamedResource() {
	}

	public JiraNamedResource(String id, String name) {
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

		if (!(o instanceof JiraNamedResource)) {
			return false;
		}

		JiraNamedResource that = (JiraNamedResource) o;

		return new EqualsBuilder()
			.append(getId(), that.getId())
			.append(getName(), that.getName())
			.isEquals();
	}

	@Override
	public final int hashCode() {
		return Objects.hash(getId(), getName());
	}

}

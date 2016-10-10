package de.cronn.jira.sync.config;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.util.Assert;

public class SourceTargetStatus {

	private final String sourceStatus;

	private final String targetStatus;

	public SourceTargetStatus(String sourceTargetStatus) {
		String[] strings = sourceTargetStatus.split(",");
		Assert.isTrue(strings.length == 2, "Illegal status pair: " + sourceTargetStatus);
		this.sourceStatus = strings[0];
		this.targetStatus = strings[1];
	}

	public SourceTargetStatus(String sourceStatus, String targetStatus) {
		this.sourceStatus = sourceStatus;
		this.targetStatus = targetStatus;
	}

	public String getSourceStatus() {
		return sourceStatus;
	}

	public String getTargetStatus() {
		return targetStatus;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SourceTargetStatus that = (SourceTargetStatus) o;

		return new EqualsBuilder()
			.append(sourceStatus, that.sourceStatus)
			.append(targetStatus, that.targetStatus)
			.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(sourceStatus)
			.append(targetStatus)
			.toHashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("sourceStatus", sourceStatus)
			.append("targetStatus", targetStatus)
			.toString();
	}
}

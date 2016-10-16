package de.cronn.jira.sync;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class TestClock extends Clock {

	private static final Instant DEFAULT_INSTANT = Instant.parse("2016-05-23T18:00:00.000Z");
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");

	private Instant instant = DEFAULT_INSTANT;
	private final ZoneId zoneId;

	public TestClock() {
		this(DEFAULT_ZONE);
	}

	public TestClock(ZoneId zoneId) {
		this.zoneId = zoneId;
	}

	public TestClock(Instant instant, ZoneId zoneId) {
		this(zoneId);
		this.instant = instant;
	}

	public void reset() {
		instant = DEFAULT_INSTANT;
	}

	public void windForwardSeconds(long seconds) {
		instant = instant.plusSeconds(seconds);
	}

	@Override
	public ZoneId getZone() {
		return zoneId;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new TestClock(instant, zone);
	}

	@Override
	public Instant instant() {
		return instant;
	}
}

package org.springframework.data.release.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to represent a Version consisting of major, minor and bugfix part.
 *
 * @author Oliver Gierke
 */
public class Version implements Comparable<Version> {

	private final BigDecimal major;
	private final BigDecimal minor;
	private final BigDecimal bugfix;
	private final BigDecimal build;

	/**
	 * Creates a new {@link Version} from the given integer values. At least one value has to be given but a maximum of 4.
	 *
	 * @param parts must not be {@literal null} or empty.
	 */
	private Version(BigDecimal... parts) {

		Assert.notNull(parts, "Parts must not be null!");
		Assert.isTrue(parts.length > 0 && parts.length < 5, "We need at least 1 at most 4 parts!");

		this.major = parts[0];
		this.minor = parts.length > 1 ? parts[1] : BigDecimal.ZERO;
		this.bugfix = parts.length > 2 ? parts[2] : BigDecimal.ZERO;
		this.build = parts.length > 3 ? parts[3] : BigDecimal.ZERO;

		Assert.isTrue(major.longValue() >= 0, "Major version must be greater or equal zero!");
		Assert.isTrue(minor.longValue() >= 0, "Minor version must be greater or equal zero!");
		Assert.isTrue(bugfix.longValue() >= 0, "Bugfix version must be greater or equal zero!");
		Assert.isTrue(build.longValue() >= 0, "Build version must be greater or equal zero!");
	}

	public static Version of(int... parts) {
		return new Version(Arrays.stream(parts).mapToObj(BigDecimal::valueOf).toArray(BigDecimal[]::new));
	}

	/**
	 * Parses the given string representation of a version into a {@link Version} object.
	 *
	 * @param version must not be {@literal null} or empty.
	 * @return
	 */
	public static Version parse(String version) {

		Assert.hasText(version, "Version must not be null or empty!");

		String[] parts = version.trim().split("\\.");
		BigDecimal[] intParts = new BigDecimal[parts.length];

		for (int i = 0; i < parts.length; i++) {
			intParts[i] = new BigDecimal(parts[i]);
		}

		return new Version(intParts);
	}

	public int getMajor() {
		return major.intValueExact();
	}

	public int getMinor() {
		return minor.intValueExact();
	}

	public int getBugfix() {
		return bugfix.intValueExact();
	}

	public int getBuild() {
		return build.intValueExact();
	}

	/**
	 * Returns whether the current {@link Version} is greater (newer) than the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isGreaterThan(Version version) {
		return compareTo(version) > 0;
	}

	/**
	 * Returns whether the current {@link Version} is greater (newer) or the same as the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isGreaterThanOrEqualTo(Version version) {
		return compareTo(version) >= 0;
	}

	/**
	 * Returns whether the current {@link Version} is the same as the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean is(Version version) {
		return equals(version);
	}

	/**
	 * Returns whether the current {@link Version} is less (older) than the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isLessThan(Version version) {
		return compareTo(version) < 0;
	}

	/**
	 * Returns whether the current {@link Version} is less (older) or equal to the current one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isLessThanOrEqualTo(Version version) {
		return compareTo(version) <= 0;
	}

	public Version nextMajor() {
		return new Version(this.major.add(BigDecimal.ONE));
	}

	public Version nextMinor() {
		return new Version(this.major, this.minor.add(BigDecimal.ONE));
	}

	public Version nextBugfix() {
		return new Version(this.major, this.minor, this.bugfix.add(BigDecimal.ONE));
	}

	public Version withBugfix(BigDecimal bugfix) {
		return new Version(this.major, this.minor, bugfix);
	}

	public Version withBugfix(int bugfix) {
		return new Version(this.major, this.minor, BigDecimal.valueOf(bugfix));
	}

	public String toMajorMinorBugfix() {
		return String.format("%s.%s.%s", major, minor, bugfix);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Version that) {

		if (that == null) {
			return 1;
		}

		if (!Objects.equals(major, that.major)) {
			return major.compareTo(that.major);
		}

		if (!Objects.equals(minor, that.minor)) {
			return minor.compareTo(that.minor);
		}

		if (!Objects.equals(bugfix, that.bugfix)) {
			return bugfix.compareTo(that.bugfix);
		}

		if (!Objects.equals(build, that.build)) {
			return build.compareTo(that.build);
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Version)) {
			return false;
		}

		Version that = (Version) obj;

		return Objects.equals(this.major, that.major) && Objects.equals(this.minor, that.minor)
				&& Objects.equals(this.bugfix, that.bugfix) && Objects.equals(this.build, that.build);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;
		result += 31 * major.hashCode();
		result += 31 * minor.hashCode();
		result += 31 * bugfix.hashCode();
		result += 31 * build.hashCode();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		List<BigDecimal> digits = new ArrayList<>();
		digits.add(major);
		digits.add(minor);

		if (build.longValue() != 0 || bugfix.longValue() != 0) {
			digits.add(bugfix);
		}

		if (build.longValue() != 0) {
			digits.add(build);
		}

		return StringUtils.collectionToDelimitedString(digits, ".");
	}
}

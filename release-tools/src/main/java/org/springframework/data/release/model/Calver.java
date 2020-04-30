package org.springframework.data.release.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to represent a Calver version consisting of year, minor, micro and modifier parts.
 * 
 * @author Mark Paluch
 */
@Getter
public class Calver implements Comparable<Calver> {

	private static final Pattern PATTERN = Pattern
			.compile("(\\d{4})\\.(\\d+)\\.(\\d+)(-((SR\\d+)|(RC\\d+)|(M\\d+)|(SNAPSHOT)))?");

	private final int year;
	private final int minor;
	private final int micro;
	private final Iteration modifier;

	private Calver(int year, int minor, int micro, Iteration modifier) {
		this.year = year;
		this.minor = minor;
		this.micro = micro;
		this.modifier = modifier;
	}

	/**
	 * Parses the given string representation of a version into a {@link Calver} object.
	 * 
	 * @param version must not be {@literal null} or empty.
	 * @return
	 */
	public static Calver parse(String version) {

		Assert.hasText(version, "Version must not be null or empty!");

		Matcher matcher = PATTERN.matcher(version);
		Assert.isTrue(matcher.find(), "Version does not match CalVer");

		int year = Integer.parseInt(matcher.group(1));
		int minor = Integer.parseInt(matcher.group(2));
		int micro = Integer.parseInt(matcher.group(3));
		String modifier = matcher.group(5);
		Iteration iteration;
		if (modifier == null) {
			iteration = Iteration.GA;
		} else if (modifier.equals("SNAPSHOT")) {
			iteration = new Iteration("SNAPSHOT", null);
		} else {
			iteration = Iteration.valueOf(modifier);
		}

		return new Calver(year, minor, micro, iteration);
	}

	/**
	 * Returns whether the current {@link Calver} is greater (newer) than the given one.
	 * 
	 * @param version
	 * @return
	 */
	public boolean isGreaterThan(Calver version) {
		return compareTo(version) > 0;
	}

	/**
	 * Returns whether the current {@link Calver} is greater (newer) or the same as the given one.
	 * 
	 * @param version
	 * @return
	 */
	public boolean isGreaterThanOrEqualTo(Calver version) {
		return compareTo(version) >= 0;
	}

	/**
	 * Returns whether the current {@link Calver} is the same as the given one.
	 * 
	 * @param version
	 * @return
	 */
	public boolean is(Calver version) {
		return equals(version);
	}

	/**
	 * Returns whether the current {@link Calver} is less (older) than the given one.
	 * 
	 * @param version
	 * @return
	 */
	public boolean isLessThan(Calver version) {
		return compareTo(version) < 0;
	}

	/**
	 * Returns whether the current {@link Calver} is less (older) or equal to the current one.
	 * 
	 * @param version
	 * @return
	 */
	public boolean isLessThanOrEqualTo(Calver version) {
		return compareTo(version) <= 0;
	}

	public Calver nextMinor() {
		return new Calver(this.year, this.minor + 1, 0, modifier);
	}

	public Calver nextBugfix() {
		return new Calver(this.year, this.minor, this.micro + 1, modifier);
	}

	public Calver withBugfix(int bugfix) {
		return new Calver(this.year, this.minor, bugfix, modifier);
	}

	public Calver withModifier(Iteration modifier) {
		return new Calver(this.year, this.minor, this.micro, modifier);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Calver that) {

		if (that == null) {
			return 1;
		}

		if (year != that.year) {
			return year - that.year;
		}

		if (minor != that.minor) {
			return minor - that.minor;
		}

		if (micro != that.micro) {
			return micro - that.micro;
		}

		if (modifier != that.modifier) {

			if (modifier == null && that.modifier == null) {
				return 0;
			}

			if (modifier != null && that.modifier == null) {
				return modifier.compareTo(Iteration.GA);
			}

			if (modifier == null && that.modifier != null) {
				return Iteration.GA.compareTo(modifier);
			}
			return modifier.compareTo(that.modifier);
		}

		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Calver))
			return false;

		Calver calver = (Calver) o;

		if (year != calver.year)
			return false;
		if (minor != calver.minor)
			return false;
		if (micro != calver.micro)
			return false;
		return modifier.equals(calver.modifier);
	}

	@Override
	public int hashCode() {
		int result = year;
		result = 31 * result + minor;
		result = 31 * result + micro;
		result = 31 * result + modifier.hashCode();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		List<Integer> digits = new ArrayList<>();
		digits.add(year);
		digits.add(minor);
		digits.add(micro);

		String raw = StringUtils.collectionToDelimitedString(digits, ".");

		if (!Iteration.GA.equals(this.modifier)) {
			return String.format("%s-%s", raw, this.modifier.getName());
		}

		return raw;
	}
}

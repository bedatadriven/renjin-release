package org.renjin.release.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;

import java.io.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageDescription {

	private ArrayListMultimap<String, String> properties = ArrayListMultimap.create();


	public static class Person {
		private String name;
		private String email;

		private static final Pattern ROLE_PATTERN = Pattern.compile("\\s*'(.+)'\\s+\\[(.+)\\]\\s*");
		
		Person(String spec) {
			int bracketStart = spec.indexOf('<');
			if(bracketStart == -1) {
				this.name = spec.trim();
			} else {
				try {
					this.name = spec.substring(0, bracketStart).trim();
					int bracketEnd = spec.indexOf('>', bracketStart);
					if(bracketEnd == -1) {
						System.err.println("WARNING: Person '" + spec + "' is missing final '>'");
						this.email = spec.substring(bracketStart+1);
					} else {
						this.email = spec.substring(bracketStart+1, bracketEnd);
					}
				} catch(Exception e) {
					throw new RuntimeException("Error parsing '" + spec + "'");
				}
			}

			Matcher roleMatcher = ROLE_PATTERN.matcher(this.name);
			if(roleMatcher.matches()) {
				this.name = roleMatcher.group(1);
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		@Override
		public String toString() {
			return name + " <" + email + ">";
		}
	}

	private static class PersonParser implements Function<String, Person> {

		@Override
		public Person apply(String arg0) {
			return new Person(arg0);
		}
	}

	public static PackageDescription fromString(String contents) throws IOException {

		PackageDescription d = new PackageDescription();
		d.properties = ArrayListMultimap.create();

		List<String> lines = CharStreams.readLines(new StringReader(contents));
		String key = null;
		StringBuilder value = new StringBuilder();
		for(String line : lines) {
			if(line.length() > 0) {
				if(Character.isWhitespace(line.codePointAt(0))) {
					if(key == null) {
						throw new IllegalArgumentException("Expected key at line '" + line + "'");
					}
					value.append(" ").append(line.trim());
				} else {
					if(key != null) {
						d.properties.put(key, value.toString());
						value.setLength(0);
					}
					int colon = line.indexOf(':');
					if(colon == -1) {
						throw new IllegalArgumentException("Expected line in format key: value, found '" + line + "'");
					}
					key = line.substring(0, colon);
					value.append(line.substring(colon+1).trim());
				}
			}
		}
		if(key != null) {
			d.properties.put(key, value.toString());
		}
		return d;
	}

	public static PackageDescription fromInputStream(InputStream in) throws IOException {
		return fromString(CharStreams.toString(new InputStreamReader(in)));
	}

	public static PackageDescription fromCharSource(CharSource charSource) throws IOException {
		return fromString(charSource.read());
	}
	
	public static PackageDescription fromReader(Reader reader) throws IOException {
		return fromString(CharStreams.toString(reader));
	}

	public String getFirstProperty(String key) {
		if(properties.containsKey(key)) {
			return properties.get(key).iterator().next();
		} else {
			return null;
		}
	}

	public List<String> getProperty(String key) {
		return properties.get(key);
	}

	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	public String getPackage() {
		return getFirstProperty("Package");
	}

	public String getTitle() {
		return getFirstProperty("Title");
	}

	public String getVersion() {
		return getFirstProperty("Version");
	}
	
	public boolean isNeedsCompilation() {
		return "yes".equals(getFirstProperty("NeedsCompilation"));
	}

	public Iterable<Person> getAuthors() {
		return Iterables.transform(Arrays.asList(getFirstProperty("Author").split("\\s*,\\s*")), new PersonParser());
	}

	public Optional<Person> getMaintainer() {
		String maintainer = getFirstProperty("Maintainer");
		if(Strings.isNullOrEmpty(maintainer)) {
			return Optional.absent();
		} else {
			return Optional.of(new Person(getFirstProperty("Maintainer")));
		}
	}
	
	public Iterable<Person> getPeople() {
		return Iterables.concat(getMaintainer().asSet(), getAuthors());
	}
	
	public String getDescription() {
		return getFirstProperty("Description");
	}

	public Optional<List<String>> getCollate() {
		if(hasProperty("Collate")) {	
			return Optional.of(parseCollateList(getFirstProperty("Collate")));
		} else {
			return Optional.absent();
		}
	}

	private List<String> parseCollateList(String collate) {
		List<String> files = new ArrayList<>();
		StringBuilder file = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < collate.length(); i++) {
			char c = collate.charAt(i);
			if(c == '\'') {
				quoted = !quoted;
			} else if(!quoted && Character.isWhitespace(c)) {
				if(file.length() > 0) {
					files.add(file.toString());
					file.setLength(0);
				}
			} else {
				file.append(c);
			}
		}
		if(file.length() > 0) {
			files.add(file.toString());
		}
		return files;
	}

	public Iterable<PackageDependency> getImports() {
		return getPackageDependencyList("Imports");
	}
	
	public Iterable<PackageDependency> getLinkingTo() {
		return getPackageDependencyList("LinkingTo");
	}

	public Iterable<PackageDependency> getDepends() {
		return getPackageDependencyList("Depends");
	}

	public Iterable<PackageDependency> getSuggests() {
		return getPackageDependencyList("Suggests");
	}

	public String getSystemRequirements() {
		return getFirstProperty("SystemRequirements");
	}

	public Iterable<PackageDependency> getPackageDependencyList(String property) {
		String list = getFirstProperty(property);
		if(Strings.isNullOrEmpty(list)) {
			return Collections.emptySet();
		} else {
			return PackageDependency.parseList(property, list);
		}
	}

	public String getLicense() {
		return getFirstProperty("License");
	}

	public List<String> getUrls() {
		List<String> list = new ArrayList<>();
		for (String url : getProperty("URL")) {
			list.addAll(Arrays.asList(url.split(",\\S*")));
		}
		return list;
	}

	public Iterable<String> getProperties() {
		return properties.keySet();
	}

	public LocalDateTime getPublicationDate() throws ParseException {
		List<String> dateStrings = properties.get("Date/Publication");
		if (!dateStrings.isEmpty()) {
			return parsePublicationDate(dateStrings.get(0));
		}
		return null;
	}

	@VisibleForTesting
	static LocalDateTime parsePublicationDate(String dateString) {
		try {
			return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"));
		} catch (IllegalArgumentException ignored) {
			// Try new format with time zone
			return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss z"));
		}
	}

	public LocalDate getDate() {
		List<String> dateStrings = properties.get("Date");
		if(!dateStrings.isEmpty()) {
			String dateString = dateStrings.get(0);
			if(dateString.contains("/")) {
				return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("YYYY/MM/dd"));
			} else {
				return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("YYYY-MM-dd"));
			}
		}
		return null;
	}
	
	public LocalDateTime getPackagedDate() {
		List<String> strings = properties.get("Packaged");
		if(strings.isEmpty()) {
			return null;
		}
		
		return parseTimeFromPackaged(strings.get(0));
	}

	/**
	 * 
	 * @return the date of the package release, using, in descending order, the {@code Date/Publication} field,
	 *  the {@code Date} field, or the {@code Pacakged} field.
	 */
	public LocalDateTime findReleaseDate() throws ParseException {
		LocalDateTime publicationDate = getPublicationDate();
		if(publicationDate != null) {
			return publicationDate;
		}
		
		LocalDate date = getDate();
		if(date != null) {
			return date.atTime(LocalTime.MIDNIGHT);
		}
		
		return getPackagedDate();
	}
	
	@VisibleForTesting
	static LocalDateTime parseTimeFromPackaged(String packagedValue) {



		// In the format of:
		// Mon Nov 27 19:54:13 2006; warnes
		
		int dateEnd = packagedValue.indexOf(';');
		if(dateEnd >= 0) {
			packagedValue = packagedValue.substring(0, dateEnd);
		}
		
		// Remove double spaces
		packagedValue = packagedValue.replaceAll("\\s+", " ");

		// New format:
		// 2017-06-07 08:34:30 UTC; dalex
		if(packagedValue.startsWith("20")) {
			return LocalDateTime.parse(packagedValue,
					DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss z"));

		} else {

			return LocalDateTime.parse(packagedValue,
					DateTimeFormatter.ofPattern("E MMM d HH:mm:ss YYYY"));
		}
	}
}
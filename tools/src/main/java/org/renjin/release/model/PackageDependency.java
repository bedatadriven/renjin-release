package org.renjin.release.model;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import java.util.Arrays;

/**
 * Describes as a dependency on a package
 */
public class PackageDependency {
  private String name;
	private String version;
  private String versionRange;
	private String versionSpec;
  private final boolean optional;

  public PackageDependency(String spec) {
	  this(spec, false);
  }

  public PackageDependency(String spec, boolean optional) {
    this.optional = optional;
    int versionSpecStart = spec.indexOf('(');
    if (versionSpecStart == -1) {
      name = spec;
      versionRange = "[0,)";
    } else {
      this.name = spec.substring(0, versionSpecStart).trim();

      int versionSpecEnd = spec.indexOf(')', versionSpecStart);
      if (versionSpecEnd == -1) {
        throw new IllegalArgumentException("Unterminated version specification: " + spec);
      }
      versionSpec = spec.substring(versionSpecStart + 1, versionSpecEnd).trim();
      if (versionSpec.startsWith(">=")) {
        versionRange = "[" + versionSpec.substring(">=".length()).trim() + ",)";
      } else if (versionSpec.startsWith(">")) {
        versionRange = "(" + versionSpec.substring(">".length()).trim() + ",)";
      } else {
        versionRange = versionSpec;
				version = versionSpec;
      }
    }
    if (Strings.isNullOrEmpty(name)) {
      throw new RuntimeException(spec);
    }
  }

	public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

	/**
	 * @return the original version specification, or null if none
	 * was provided
	 */
	public String getVersionSpec() {
		return versionSpec;
	}


	/**
	 * @return Maven-style version range. If none was provided in the description file,
	 * it will be [0,)
	 */
	public String getVersionRange() {
    return versionRange;
  }

	/**
	 * @return the precise version specified in the description file, or null if no single
	 * version was specified
	 */
	public String getVersion() {
		return version;
	}

	@Override
  public String toString() {
    return (name + "  " + versionRange).trim();
  }

  public boolean isOptional() {
    return optional;
  }

  public static Iterable<PackageDependency> parseList(String scope, String list) {
		return Iterables.transform(Arrays.asList(list.trim().split("\\s*,\\s*")),
      new PackageDependencyParser("Suggests".equals(scope)));
	}

	private static class PackageDependencyParser implements Function<String, PackageDependency> {

    private final boolean optional;

    public PackageDependencyParser(boolean optional) {
      this.optional = optional;
    }

    @Override
		public PackageDependency apply(String spec) {
			if(spec == null) {
				throw new RuntimeException();
			}
			return new PackageDependency(spec, optional);
		}
	}
}

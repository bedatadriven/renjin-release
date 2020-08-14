package org.renjin.release.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * PackageVersion identifier, a composite of groupId, packageName,
 * and version (a typical GAV from Maven world)
 */
public class PackageVersionId implements Serializable, Comparable<PackageVersionId> {
  private String groupId;
  private String packageName;
  private String version;

  PackageVersionId() {
  }

  @JsonCreator
  public PackageVersionId(String groupArtifactVersion) {
    String gav[] = groupArtifactVersion.split(":");
    this.groupId = gav[0];
    this.packageName = gav[1];
    this.version = gav[2];
  }

  public PackageVersionId(String groupId, String packageName, String version) {
    this.groupId = groupId;
    this.packageName = packageName;
    this.version = version;
  }

  public PackageVersionId(PackageId packageId, String version) {
    this.groupId = packageId.getGroupId();
    this.packageName = packageId.getPackageName();
    this.version = version;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getVersionString() {
    return version;
  }

  public String getVersion() {
    return version;
  }

  public static PackageVersionId fromTriplet(String id) {
    String gav[] = id.split(":");
    if(gav.length != 3) {
      throw new IllegalArgumentException("Malformed package id: " + id);
    }
    return new PackageVersionId(gav[0], gav[1], gav[2]);
  }
  
  public boolean isNewer(PackageVersionId other) {
    return compareTo(other) > 0;
  }

  public String getPath() {
    return "/package/" + getGroupId() + "/" + getPackageName() + "/" + getVersionString();
  }

  @JsonValue
  @Override
  public String toString() {
    return groupId + ":" + packageName + ":" + version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PackageVersionId that = (PackageVersionId) o;

    if (!groupId.equals(that.groupId)) return false;
    if (!packageName.equals(that.packageName)) return false;
    if (!version.equals(that.version)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + packageName.hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }
  
  @Override
  public int compareTo(PackageVersionId o) {
    if(!groupId.equals(o.groupId)) {
      return groupId.compareTo(o.groupId);
    }
    if(!packageName.equals(o.packageName)) {
      return packageName.compareTo(o.packageName);
    }
    return compareVersions(this.version, o.version);
  }

  public PackageId getPackageId() {
    return new PackageId(groupId, packageName);
  }

  public static int compareVersions(String x, String y) {
    int[] xn = parse(x);
    int[] yn = parse(y);

    for (int i = 0; i < xn.length && i < yn.length; i++) {
      if(xn[i] < yn[i]) {
        return -1;
      }
      if(xn[i] > yn[i]) {
        return +1;
      }
    }

    if(xn.length < yn.length) {
      return -1;
    }
    if(xn.length > yn.length) {
      return +1;
    }
    return 0;
  }

  private static int[] parse(String versionString) {
    String[] stringParts = versionString.split("[^0-9]");
    int[] parts = new int[stringParts.length];
    for (int i = 0; i < parts.length; i++) {
      parts[i] = Integer.parseInt(stringParts[i]);
    }
    return parts;
  }
}

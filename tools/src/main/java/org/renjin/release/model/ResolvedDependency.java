package org.renjin.release.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The resolution of an unqualified package dependency (e.g. "survey") to a fully qualified {@code PackageVersionId} and 
 * a build number, if one is available.
 */
@JsonAutoDetect(
    isGetterVisibility = JsonAutoDetect.Visibility.NONE, 
    getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolvedDependency {
  
  @JsonProperty 
  private String name;
  
  @JsonProperty
  private boolean optional;
  
  @JsonProperty
  private PackageVersionId packageVersionId;
  
  @JsonProperty
  private Long buildNumber;
  
  @JsonProperty
  private String replacementVersion;


  public ResolvedDependency() {
  }

  public ResolvedDependency(String name) {
    this.name = name;
  }

  public ResolvedDependency(PackageVersionId packageVersionId) {
    this.name = packageVersionId.getPackageName();
    this.packageVersionId = packageVersionId;
  }
  
  /**
   *
   * @return the original name of the dependency, as specified in the POM.
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }

  public void setPackageVersionId(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public Long getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(Long buildNumber) {
    this.buildNumber = buildNumber;
  }

  @Override
  public String toString() {
    if(packageVersionId == null) {
      return name;
    } else if(buildNumber == null) {
      return packageVersionId.toString();
    } else {
      return packageVersionId + "-b" + buildNumber;
    }
  }

  @JsonIgnore
  public boolean hasBuild() {
    return buildNumber != null;
  }

  public boolean isVersionResolved() {
    return packageVersionId != null;
  }

  public String getReplacementVersion() {
    return replacementVersion;
  }

  public void setReplacementVersion(String replacementVersion) {
    this.replacementVersion = replacementVersion;
  }

  public boolean isReplaced() {
    return this.replacementVersion != null;
  }
}

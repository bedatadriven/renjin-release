package org.renjin.release.model;


import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

public class RenjinVersionId implements Serializable {

  public static final RenjinVersionId RELEASE = new RenjinVersionId("0.7.0-RC7");
  
  public static final RenjinVersionId FIRST_VERSION_WITH_CPP = new RenjinVersionId("0.8.2025");

  private final String version;

  public RenjinVersionId(String version) {
    this.version = version;
  }

  @JsonValue
  @Override
  public String toString() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RenjinVersionId that = (RenjinVersionId) o;

    if (!version.equals(that.version)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return version.hashCode();
  }

  public static RenjinVersionId valueOf(String string) {
    return new RenjinVersionId(string);
  }

}

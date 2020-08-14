package org.renjin.release.graph;

public class DependencyEdge {
  private PackageNode packageNode;
  private boolean optional;

  public DependencyEdge(PackageNode packageNode, boolean optional) {
    this.packageNode = packageNode;
    this.optional = optional;
  }

  public PackageNode getPackageNode() {
    return packageNode;
  }

  public boolean isOptional() {
    return optional;
  }
}

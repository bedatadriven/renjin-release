package org.renjin.release.graph;


import org.renjin.release.model.PackageVersionId;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class PackageNode implements Serializable {

  private final PackageVersionId packageVersionId;

  /**
   * Dependencies of this node that are to be built during
   * this workflow.
   */
  private Future<Set<DependencyEdge>> dependencies;

  private final Set<PackageNode> reverseDependencies = new HashSet<>();

  /**
   * true if we are reusing an existing build.
   */
  private boolean replaced;

  private String replacedVersion;

  private boolean blocked;


  public PackageNode(PackageVersionId packageVersionId, Future<Set<DependencyEdge>> dependencies) {
    this.packageVersionId = packageVersionId;
    this.dependencies = dependencies;
  }

  public PackageVersionId getId() {
    return packageVersionId;
  }

  public boolean isBlocked() {
    return blocked;
  }

  public void setBlocked(boolean blocked) {
    this.blocked = blocked;
  }

  @Override
  public String toString() {
    return packageVersionId.toString();
  }

  public boolean isReplaced() {
    if(packageVersionId.getPackageName().equals("testthat") ||
      packageVersionId.getPackageName().equals("Rcpp")) {
      return false;
    }
    return replaced;
  }

  public Set<DependencyEdge> getDependencies() {
    try {
      return dependencies.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public Set<PackageNode> getReverseDependencies() {
    return reverseDependencies;
  }

  public void addReverseDependency(PackageNode node) {
    reverseDependencies.add(node);
  }

  public void replaced(String version) {
    this.replaced = true;
    this.replacedVersion = version;
  }

  public String getReplacedVersion() {
    return replacedVersion;
  }

  public Collection<PackageNode> getDependencyNodes() {
    return getDependencies()
      .stream()
      .map(DependencyEdge::getPackageNode)
      .collect(Collectors.toSet());
  }
}

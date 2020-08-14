package org.renjin.release.graph;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.renjin.release.Blocklist;
import org.renjin.release.PackageDatabaseClient;
import org.renjin.release.model.PackageId;
import org.renjin.release.model.PackageVersionId;
import org.renjin.release.model.ResolvedDependency;
import org.renjin.release.model.ResolvedDependencySet;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Builds a graph of packages and their dependencies
 */
public class PackageGraphBuilder {

  private static final Logger LOGGER = Logger.getLogger(PackageGraphBuilder.class.getName());

  private final ExecutorService executorService;
  private final DependencyCache dependencyCache;
  private final ReplacedPackageProvider replacedPackages;
  private final Blocklist blocklist;

  private final Map<PackageId, PackageNode> nodes = new HashMap<>();

  public PackageGraphBuilder(ExecutorService executorService,
                             DependencyCache dependencyCache,
                             ReplacedPackageProvider replacedPackages,
                             Blocklist blocklist) {
    this.executorService = executorService;
    this.dependencyCache = dependencyCache;
    this.replacedPackages = replacedPackages;
    this.blocklist = blocklist;
  }

  public void add(String filter) throws InterruptedException {
    add(filter, null);
  }

  public void add(String filter, Double sample) throws InterruptedException {
    List<PackageVersionId> sampled = queryList(filter, sample);

    LOGGER.info("Building dependency graph...");

    for (PackageVersionId packageVersionId : sampled) {
      LOGGER.fine(packageVersionId.toString());
      add(packageVersionId);
    }
  }

  public static List<PackageVersionId> queryList(String filter, Double sample) {

    if(filter.contains(":")) {
      // consider as packageId
      PackageVersionId packageVersionId = PackageVersionId.fromTriplet(filter);
      return Collections.singletonList(packageVersionId);
    }

    LOGGER.info(String.format("Querying list of '%s' packages...", filter));
    List<PackageVersionId> packageVersionIds = PackageDatabaseClient.queryPackageList(filter);
    LOGGER.info(String.format("Found %d packages.", packageVersionIds.size()));

    return sample(packageVersionIds, sample);
  }

  /**
   * Samples a proportion or a fixed sample size of packages to build, generally for testing purposes.
   */
  private static List<PackageVersionId> sample(List<PackageVersionId> packageVersionIds, Double sample) {
    if(sample == null) {
      return packageVersionIds;
    } else {
      double fraction;
      if(sample > 1) {
        LOGGER.info(String.format("Sampling %.0f packages randomly", sample));
        fraction = sample / (double)packageVersionIds.size();
      } else {
        fraction = sample;
        LOGGER.info(String.format("Sampling %7.6f of packages randomly", sample));
      }

      List<PackageVersionId> sampled = new ArrayList<PackageVersionId>();
      Random random = new Random();
      for (PackageVersionId packageVersionId : packageVersionIds) {
        if(random.nextDouble() < fraction) {
          sampled.add(packageVersionId);
        }
      }
      LOGGER.info(String.format("Sampled %d packages", sampled.size()));

      return sampled;
    }
  }

  /**
   * Creates a packageNode for a specific packageVersion, and queues it for dependency resolution
   */
  private void add(PackageVersionId packageVersionId) {

    Preconditions.checkState(!nodes.containsKey(packageVersionId.getPackageId()),
        "%s has already been added to the graph.", packageVersionId);

    PackageNode node = new PackageNode(packageVersionId, resolveDependencies(packageVersionId));
    nodes.put(node.getId().getPackageId(), node);

  }

  private DependencyEdge getOrCreateNodeForDependency(ResolvedDependency resolvedDependency) {
    synchronized (nodes) {
      PackageVersionId pvid = resolvedDependency.getPackageVersionId();
      PackageNode node = nodes.get(pvid.getPackageId());
      if (node == null) {
        if (resolvedDependency.isReplaced() || replacedPackages.isReplaced(pvid)) {
          node = new PackageNode(resolvedDependency.getPackageVersionId(), Futures.immediateFuture(Collections.emptySet()));
          node.replaced(resolvedDependency.getReplacementVersion());
        } else {
          node = new PackageNode(pvid, resolveDependencies(resolvedDependency));
          node.setBlacklisted(blocklist.isBlacklisted(pvid.getPackageName()));
        }
        nodes.put(node.getId().getPackageId(), node);
      }
      return new DependencyEdge(node, resolvedDependency.isOptional());
    }
  }

  private DependencyEdge getOrCreateMissingDependency(ResolvedDependency resolvedDependency) {
    synchronized (nodes) {
      PackageId packageId = new PackageId("missing", resolvedDependency.getName());
      PackageNode node = nodes.get(packageId);
      if(node == null) {
        node = new PackageNode(new PackageVersionId(packageId, "0"), Futures.immediateFuture(Collections.emptySet()));
        node.setBlacklisted(true);
        nodes.put(packageId, node);
      }
      return new DependencyEdge(node, resolvedDependency.isOptional());
    }
  }

  private Future<Set<DependencyEdge>> resolveDependencies(ResolvedDependency resolvedDependency) {
    if(resolvedDependency.isReplaced()) {
      return Futures.immediateFuture(Collections.emptySet());
    } else {
      return resolveDependencies(resolvedDependency.getPackageVersionId());
    }
  }

  private Future<Set<DependencyEdge>> resolveDependencies(PackageVersionId pvid) {

    return executorService.submit(new Callable<Set<DependencyEdge>>() {
      @Override
      public Set<DependencyEdge> call() {

        LOGGER.info("Resolving " + pvid);

        ResolvedDependencySet resolution;

        resolution = dependencyCache.get(pvid);
        if(resolution == null) {
          try {
            resolution = PackageDatabaseClient.resolveDependencies(pvid);

            dependencyCache.cache(pvid, resolution);

          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to resolve dependencies of %s: %s", pvid, e.getMessage()), e);
            throw new RuntimeException("Failed to resolve dependency of " + pvid, e);
          }
        }

        Set<DependencyEdge> dependencies = new HashSet<>();

        for (ResolvedDependency resolvedDependency : resolution.getDependencies()) {
          if (resolvedDependency.isVersionResolved()) {
            dependencies.add(getOrCreateNodeForDependency(resolvedDependency));
          } else {
            dependencies.add(getOrCreateMissingDependency(resolvedDependency));
          }
        }

        return dependencies;
      }
    });
  }

  public PackageGraph build() {

    Set<PackageNode> resolved = new HashSet<>();
    ArrayDeque<PackageNode> queue = new ArrayDeque<>(nodes.values());
    while(!queue.isEmpty()) {
      PackageNode packageNode = queue.pop();
      if(!resolved.contains(packageNode)) {
        queue.addAll(packageNode.getDependencyNodes());
        resolved.add(packageNode);
      }
    }

    // Add reverseDependency links
    for (PackageNode node : nodes.values()) {
      for (DependencyEdge edge : node.getDependencies()) {
        if(!edge.isOptional()) {
          edge.getPackageNode().addReverseDependency(node);
        }
      }
    }

    // Propagate blacklisted flag
    List<PackageNode> blacklisted = nodes.values()
      .stream()
      .filter(n -> n.isBlacklisted())
      .collect(Collectors.toList());

    for (PackageNode node : blacklisted) {
      if(node.isBlacklisted()) {
        propagateBlacklistedStatus(node.getId().getPackageName(), node);
      }
    }

    // Remove blacklisted nodes from the graph
    nodes.values().removeIf(PackageNode::isBlacklisted);

    return new PackageGraph(nodes);
  }

  private void propagateBlacklistedStatus(String packageName, PackageNode node) {
    for (PackageNode reverseDependency : node.getReverseDependencies()) {
      if(!reverseDependency.isBlacklisted()) {
        System.out.println("Blacklisting " + reverseDependency.getId().getPackageName() + " because of (transitive) dependency on " + packageName);
        reverseDependency.setBlacklisted(true);
        propagateBlacklistedStatus(node.getId().getPackageName(), reverseDependency);
      }
    }
  }

}

package org.renjin.release;

import com.google.common.collect.Lists;
import org.renjin.release.graph.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UpdatePackageList {


  public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

    File universeRoot = new File(args[0]);

    System.out.println("Universe root: " + universeRoot.getAbsolutePath());

    ReplacedPackageProvider replacedPackages = new ReplacedPackageProvider(new File(universeRoot, "replacements"));

    File packageRootDir = new File(universeRoot, "packages");

    ExecutorService executorService = Executors.newFixedThreadPool(12);

    DependencyCache dependencyCache = new DependencyCache(packageRootDir, "cran");

    Blocklist blocklist = new Blocklist(packageRootDir);

    PackageGraphBuilder builder = new PackageGraphBuilder(executorService, dependencyCache, replacedPackages, blocklist);
    builder.add("org.renjin.cran:MASS:7.3-51.4", null);
    builder.add("org.renjin.cran:Matrix:1.2-17");
    builder.add("org.renjin.cran:ggplot2:3.1.1", null);
    builder.add("org.renjin.cran:testthat:2.1.1", null);
    builder.add("org.renjin.cran:knitr:1.23", null);
    builder.add("org.renjin.cran:bitops:1.0-6", null);
    PackageGraph graph = builder.build();

    System.out.println("Package count: " + graph.getNodes().size());

    File packageIndexFile = new File(packageRootDir, "packages.list");
    List<PackageNode> nodes = Lists.newArrayList(graph.getNodes());
    nodes.sort(Comparator.comparing(PackageNode::getId));
    try(FileWriter writer = new FileWriter(packageIndexFile)) {
      for (PackageNode node : nodes) {
        if(node.isReplaced()) {
          writer.write(node.getId().getPackageId() + ":" + node.getReplacedVersion() + "*");
        } else {
          writer.write(node.getId().toString());
        }
        writer.write('\n');
      }
    }

    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);
  }
}

package org.renjin.release;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.renjin.release.graph.ReplacedPackageProvider;
import org.renjin.release.model.PackageVersionId;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Reads the list of packages to build, downloads their sources, and
 * writes gradle build files for each.
 */
public class PackageSetup {


  public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

    File universeRoot = new File(args[0]);

    System.out.println("Universe root: " + universeRoot.getAbsolutePath());
    ReplacedPackageProvider replacedPackages = new ReplacedPackageProvider(new File(universeRoot, "replacements"));

    File packageRootDir = new File(universeRoot, "packages");

    ExecutorService executorService = Executors.newFixedThreadPool(12);

    PackageIndex packageIndex = new PackageIndex(packageRootDir);

    System.out.println("Packages to build: " + packageIndex.getToBuild().size());

    File subDir = new File(packageRootDir, "cran");

    List<Future<?>> tasks = new ArrayList<>();
    for (PackageVersionId id : packageIndex.getToBuild()) {
      File packageDir = new File(subDir, id.getPackageName());
      PackageSetupTask task = new PackageSetupTask(packageIndex, id, packageDir);
      tasks.add(executorService.submit(task));
    }

    for (Future<?> task : tasks) {
      task.get();
    }

    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);

    updateSettingsFile(packageRootDir, packageIndex, replacedPackages);
  }

  private static void updateSettingsFile(File rootDir, PackageIndex packageIndex, ReplacedPackageProvider replacedPackages) throws IOException {
    File settingsInFile = new File(rootDir, "settings.gradle.in");
    File settingsFile = new File(rootDir, "settings.gradle");

    CharSource settinsgsIn = Files.asCharSource(settingsInFile, Charsets.UTF_8);

    StringBuilder updated = new StringBuilder();
    settinsgsIn.copyTo(updated);
    updated.append("\n\n");

    replacedPackages.appendIncludeBuilds(updated);

    updated.append("\n\n");
    for (PackageVersionId packageVersionId : packageIndex.getToBuild()) {
      updated.append("include 'cran:" + packageVersionId.getPackageName()).append("'\n");
    }

    Files.write(updated.toString(), settingsFile, Charsets.UTF_8);
  }


}

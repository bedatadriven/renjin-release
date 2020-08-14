package org.renjin.release.graph;

import org.renjin.release.model.PackageVersionId;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ReplacedPackageProvider {

    private Set<String> replaced = new HashSet<>();

    public ReplacedPackageProvider(File rootDir) {
        for (File dir : rootDir.listFiles()) {
            if(dir.isDirectory()) {
                File buildScript = new File(dir, "build.gradle");
                if(buildScript.exists()) {
                    replaced.add(dir.getName());
                }
            }
        }
    }


  public boolean isReplaced(PackageVersionId pvid) {
    return replaced.contains(pvid.getPackageName());
  }

  public void appendIncludeBuilds(StringBuilder settingFile) {
    for (String packageName : replaced) {
      settingFile.append("includeBuild '../replacements/").append(packageName).append("'\n");
    }
  }
}

package org.renjin.release;


import org.renjin.release.model.PackageId;
import org.renjin.release.model.PackageVersionId;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Blocklist {

  private final Set<String> blocked;

  public Blocklist(File packageRootDirectory) throws IOException {

    File blockListFile = new File(packageRootDirectory, "packages.blocklist");
    this.blocked = parsePackageList(blockListFile);
  }

  public static Set<String> parsePackageList(File blockListFile) throws IOException {
    Set<String> set = new HashSet<>();
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(blockListFile), StandardCharsets.UTF_8))) {
      String line;
      while((line = reader.readLine()) != null) {
        line = line.replaceAll("#.*$", "").trim();
        if(!line.isEmpty()) {
          set.add(line);
        }
      }
    }
    return set;
  }

  public boolean isBlocked(PackageVersionId pvid) {
    return isBlocked(pvid.getPackageId());
  }

  public boolean isBlocked(PackageId id) {
    return id.getGroupId().equals(PackageId.CRAN_GROUP) && blocked.contains(id.getPackageName());
  }

  public boolean isBlocked(String name) {
    return blocked.contains(name);
  }

  public boolean isCompilationDisabled(PackageVersionId id) {
    return id.getGroupId().equals("org.renjin.cran") && blocked.contains(id.getPackageName());
  }

}

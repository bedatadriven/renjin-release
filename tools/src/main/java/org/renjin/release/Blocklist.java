package org.renjin.release;


import org.renjin.release.model.PackageId;
import org.renjin.release.model.PackageVersionId;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Blocklist {

  private final Set<String> certified;
  private final Set<String> blacklist;

  public Blocklist(File packageRootDirectory) throws IOException {

    File blackListFile = new File(packageRootDirectory, "packages.blocklist");
    this.blacklist = parsePackageList(blackListFile);

    File certifiedFile = new File(packageRootDirectory, "packages.certified");
    this.certified = parsePackageList(certifiedFile);
  }

  public static Set<String> parsePackageList(File blackListFile) throws IOException {
    Set<String> set = new HashSet<>();
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(blackListFile), StandardCharsets.UTF_8))) {
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

  public boolean isBlacklisted(PackageVersionId pvid) {
    return isBlacklisted(pvid.getPackageId());
  }

  public boolean isBlacklisted(PackageId id) {
    return id.getGroupId().equals(PackageId.CRAN_GROUP) && blacklist.contains(id.getPackageName());
  }

  public boolean isBlacklisted(String name) {
    return blacklist.contains(name);
  }

  public boolean isCompilationDisabled(PackageVersionId id) {
    return id.getGroupId().equals("org.renjin.cran") && blacklist.contains(id.getPackageName());
  }

  public boolean isCertified(PackageVersionId id) {
    return certified.contains(id.getPackageName());
  }
}

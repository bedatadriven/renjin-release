package org.renjin.release.model;

import java.io.Serializable;

/**
 * Combination of groupId and packageName that uniquely identifies a package
 */
public class PackageId implements Serializable {

    public static final String CRAN_GROUP = "org.renjin.cran";
    public static final String BIOC_GROUP = "org.renjin.bioconductor";


    private final String groupId;
    private final String packageName;

    public PackageId(String groupId, String packageName) {
        this.groupId = groupId;
        this.packageName = packageName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toString() {

        return groupId + ":"  + packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageId packageId = (PackageId) o;

        if (!groupId.equals(packageId.groupId)) return false;
        if (!packageName.equals(packageId.packageName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + packageName.hashCode();
        return result;
    }

    public static PackageId valueOf(String name) {
        String parts[] = name.split(":");
        return new PackageId(parts[0], parts[1]);
    }
    
    public static PackageId gitHub(String owner, String repo) {
        return new PackageId("org.renjin.github." + owner, repo);
    }
    
    public String getPath() {
        return "/package/" + groupId + "/" + packageName;
    }
}


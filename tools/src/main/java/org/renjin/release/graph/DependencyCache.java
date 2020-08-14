package org.renjin.release.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.renjin.release.model.PackageVersionId;
import org.renjin.release.model.ResolvedDependencySet;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyCache {

    private static final Logger LOGGER = Logger.getLogger(DependencyCache.class.getName());

    private final ObjectMapper objectMapper;
    private final File parentDir;

    public DependencyCache(File rootDir, String parentModel) {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        parentDir = new File(rootDir, parentModel);
    }

    public ResolvedDependencySet get(PackageVersionId pvid) {
        File cacheFile = cacheFile(pvid);
        if(!cacheFile.exists()) {
            return null;
        }
        try {
            return objectMapper.readValue(cacheFile, ResolvedDependencySet.class);
        } catch (IOException e) {
            return null;
        }
    }

    private File cacheFile(PackageVersionId pvid) {
        File packageDir = new File(parentDir, pvid.getPackageName());
        return new File(packageDir, pvid.getPackageName() + "_" + pvid.getVersion() + ".dependencies.json");
    }

    public void cache(PackageVersionId pvid, ResolvedDependencySet dependencySet) {
        File file = cacheFile(pvid);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            LOGGER.log(Level.WARNING, "Failed to create directory " + file.getParentFile().getAbsolutePath());
        }
        try {
            objectMapper.writeValue(file, dependencySet);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to cache " + pvid, e);
        }
    }
}

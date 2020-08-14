package org.renjin.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.renjin.release.model.PackageDependency;
import org.renjin.release.model.PackageVersionId;
import org.renjin.release.model.ResolvedDependencySet;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;


public class PackageDatabaseClient {

  private static final Logger LOGGER = Logger.getLogger(PackageDatabaseClient.class.getName());

  public static final String ROOT_URL = "https://10-dot-packages-dot-renjinci.appspot.com";


  private static WebTarget rootTarget() {
    return client().target(ROOT_URL);
  }

  private static Client client() {
    return ClientBuilder.newClient().register(JacksonJsonProvider.class);
  }


  public static ResolvedDependencySet resolveDependencies(PackageVersionId packageVersionId) {
    return packageVersion(packageVersionId)
        .path("resolveDependencies")
        .request()
        .get(ResolvedDependencySet.class);
  }
  
  public static List<PackageVersionId> resolveDependencies(List<PackageDependency> dependencies) {
    
    if(dependencies.isEmpty()) {
      return Collections.emptyList();
    }
    
    WebTarget path = rootTarget()
        .path("packages")
        .path("resolveDependencies");

    for (PackageDependency dependency : dependencies) {
      path = path.queryParam(dependency.getName(), dependency.getVersion());
    }
    
    ArrayNode versions = path.request().get(ArrayNode.class);

    List<PackageVersionId> versionIds = new ArrayList<>();
    for (JsonNode version : versions) {
      versionIds.add(PackageVersionId.fromTriplet(version.asText()));
    }
    return versionIds;
  }

  public static ResolvedDependencySet resolveSuggests(final List<PackageDependency> dependencies) {

    return withRetries(new Callable<ResolvedDependencySet>() {
      @Override
      public ResolvedDependencySet call() throws Exception {
        WebTarget target = client().target(ROOT_URL)
            .path("packages")
            .path("resolveSuggests");

        for (PackageDependency dependency : dependencies) {
          target = target.queryParam("p", dependency.getName());
        }

        return target.request().get(ResolvedDependencySet.class);
      }
    });
  }
  
  public static ListenableFuture<ResolvedDependencySet> resolveDependencies(ListeningExecutorService service, 
                                                                            final PackageVersionId id) {
    return service.submit(new Callable<ResolvedDependencySet>() {
      @Override
      public ResolvedDependencySet call() throws Exception {
        return resolveDependencies(id);
      }
    });
  }


  private static WebTarget packageVersion(PackageVersionId packageVersionId) {
    return rootTarget()
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString());
  }


  public static List<PackageVersionId> queryPackageList(String filter) {
    
    String url = ROOT_URL + "/packages/" + filter;
    String[] ids = client().target(url).request().get(String[].class);

    List<PackageVersionId> packageVersionIds = new ArrayList<PackageVersionId>();
    for (String id : ids) {
      packageVersionIds.add(PackageVersionId.fromTriplet(id));
    }
    return packageVersionIds;
  }


  private static <T> T withRetries(Callable<T> callable) {
    int retries = 8;
    while(true) {
      try {
        return callable.call();
      } catch (InternalServerErrorException e) {
        if (retries <= 0) {
          throw e;
        }
        retries--;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static String getPatchedVersionId(PackageVersionId pvid) throws IOException {

    // Avoiding hitting the API to check whether the branch exists in order to avoid rate limits
    Response head = client()
        .target(String.format("https://github.com/bedatadriven/%s.%s/tree/patched-%s",
            pvid.getGroupId(),
            pvid.getPackageName(),
            pvid.getVersionString()))
        .request()
        .head();

    if(head.getStatus() == 404) {
      return null;
    }

    Response response = client()
        .target(String.format("https://api.github.com/repos/bedatadriven/%s.%s/branches/patched-%s",
            pvid.getGroupId(),
            pvid.getPackageName(),
            pvid.getVersionString()))
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();

    if(response.getStatus() == 404) {
      return null;
    }

    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode root = (ObjectNode) objectMapper.readTree(response.readEntity(String.class));
    ObjectNode commit = (ObjectNode) root.get("commit");
    return commit.get("sha").asText();
  }

  public static URL getPatchedVersionUrl(PackageVersionId pvid) {
    try {
      return new URL(String.format("https://github.com/bedatadriven/%s.%s/archive/patched-%s.zip",
          pvid.getGroupId(),
          pvid.getPackageName(),
          pvid.getVersionString()));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getPatchedArchiveFileName(PackageVersionId pvid) {
    return "patched-" + pvid.getVersionString() + ".zip";
  }

}

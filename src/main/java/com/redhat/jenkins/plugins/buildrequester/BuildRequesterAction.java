package com.redhat.jenkins.plugins.buildrequester;

import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Failure;
import hudson.security.ACL;
import hudson.security.Permission;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
@SuppressWarnings("unchecked")
public class BuildRequesterAction implements Action {
    public static final Permission BUILD_REQUEST = AbstractProject.BUILD;

    public static final String BUILD_CONFIG_ENDPOINT = "build-configurations";
    public static final String BUILD_TRIGGER_ENDPOINT = "build-configurations/%s/build";

    private MavenModuleSetBuild build;

    // Props
    private String name;
    private String buildScript;
    private String scmRepoURL;
    private List<String> scmRevisions;

    @RequirePOST
    public HttpResponse doBuildRequestSubmit(StaplerRequest req) {
        getACL().checkPermission(BUILD_REQUEST);

        try {
            JSONObject form = req.getSubmittedForm();
            final String oauthToken = form.getString("oauth");

            // Remove keys that are empty (i.e. inputs without name)
            form.remove("");
            // Remove oauth as that is sent as http header
            form.remove("oauth");

            // Add project id and environment id
            form.put("projectId", 1);
            form.put("environmentId", 1);

            // Ncl url
            URL nclUrl = Utils.normalize(new URL(getUrl()));

            // Default headers
            Map<String, String> defaultHeaders = new HashMap<String, String>() {{
                put("Authorization", "Bearer " + oauthToken);
                put("Content-Type", "application/json");
                put("Accept", "application/json");
            }};

            // Send request for build configuration id
            String query = "?q=name==" + form.getString("name");
            URL buildConfigUrl = new URL(nclUrl, BUILD_CONFIG_ENDPOINT + query);
            HttpUtils.Response buildConfigResponse = HttpUtils.get(buildConfigUrl, defaultHeaders);

            // Handle errors
            if (buildConfigResponse.getResponseCode() / 100 != 2) {
                handleHttpError("Build config lookup error", buildConfigResponse);
            }

            // Handle response and get build id
            int buildId;
            if (buildConfigResponse.getResponseCode() == 204) {
                URL newBuildConfig = new URL(nclUrl, BUILD_CONFIG_ENDPOINT);
                HttpUtils.Response newBuildConfigResponse = HttpUtils.post(
                        newBuildConfig, form.toString(), defaultHeaders);
                if (newBuildConfigResponse.getResponseCode() / 100 != 2) {
                    handleHttpError("Build config creation error", newBuildConfigResponse);
                }

                JSONObject newbuildConfigJson = JSONObject.fromObject(newBuildConfigResponse.getContent());
                buildId = newbuildConfigJson.getJSONObject("content").getInt("id");
            } else {
                JSONObject buildConfigJson = JSONObject.fromObject(buildConfigResponse.getContent());
                buildId = buildConfigJson.getJSONArray("content").getJSONObject(0).getInt("id");
            }

            // Send the request
            URL buildRequestUrl = new URL(nclUrl, String.format(BUILD_TRIGGER_ENDPOINT, buildId));
            HttpUtils.Response buildRequestResponse = HttpUtils.post(buildRequestUrl, null, defaultHeaders);
            if (buildRequestResponse.getResponseCode() / 100 != 2) {
                handleHttpError("Build request error", buildRequestResponse);
            }
        } catch (ServletException e) {
            throw new Failure("Error: " + e.getClass() + ": " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new Failure("Error: " + e.getClass() + ": " + e.getMessage());
        } catch (IOException e) {
            throw new Failure("Error: " + e.getClass() + ": " + e.getMessage());
        }
        return new HttpRedirect("..");
    }

    public HttpResponse doKeycloakSettings(StaplerRequest req) {
        return new HttpResponse() {
            @Override
            public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                rsp.setContentType("application/json");
                rsp.setStatus(200);
                rsp.getWriter().write(getKeycloakSettings());
                rsp.getWriter().flush();
                rsp.getWriter().close();
            }
        };
    }

    public HttpResponse doFail(StaplerRequest req) {
        throw new Failure(req.getParameter("message"));
    }

    @Override
    public String getIconFileName() {
        return "/images/24x24/redo.png";
    }

    @Override
    public String getDisplayName() {
        return "Handover to Prod";
    }

    @Override
    public String getUrlName() {
        return "handover";
    }

    public BuildRequesterPublisher getPublisher() {
        BuildRequesterPublisher publisher = (BuildRequesterPublisher) build.getProject().getPublishers()
                .get(build.getDescriptorByName(BuildRequesterPublisher.class.getSimpleName()));
        return publisher;
    }

    public String getUrl() {
        BuildRequesterPublisher publisher = this.getPublisher();
        return publisher != null ? publisher.getUrl() : null;
    }

    public String getKeycloakSettings() {
        BuildRequesterPublisher publisher = this.getPublisher();
        return publisher != null ? publisher.getKeycloakSettings() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuildScript() {
        return buildScript;
    }

    public void setBuildScript(String buildScript) {
        this.buildScript = buildScript;
    }

    public String getScmRepoURL() {
        return scmRepoURL;
    }

    public void setScmRepoURL(String scmRepoURL) {
        this.scmRepoURL = scmRepoURL;
    }

    public List<String> getScmRevisions() {
        return scmRevisions;
    }

    public void setScmRevisions(List<String> scmRevisions) {
        this.scmRevisions = scmRevisions;
    }

    public void setBuild(MavenModuleSetBuild build) {
        this.build = build;
    }

    public MavenModuleSetBuild getBuild() {
        return this.build;
    }

    public ACL getACL() {
        return this.build.getACL();
    }

    private void handleHttpError(String title, HttpUtils.Response response) {
        try {
            String errMessage = JSONObject.fromObject(response.getContent()).getString("errorMessage");
            throw new Failure(title + ": " + errMessage);
        } catch (JSONException e) {
            throw new Failure(title + ": " + response.getResponseCode() + " " + response.getResponseMessage());
        }
    }
}

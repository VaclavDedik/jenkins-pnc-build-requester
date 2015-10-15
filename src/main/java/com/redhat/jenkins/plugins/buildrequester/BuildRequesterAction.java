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
    private String gav;
    private String javaVersion;
    private String mavenVersion;
    private String buildCommand;
    private String commandLineParameters;
    private String scm;
    private List<String> tags;

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

            // Ncl url
            URL nclUrl = Utils.normalize(new URL(getUrl()));

            // Send request for build configuration id
            String query = "?q=name==" + form.getString("BuildConfigName");
            URL buildConfigUrl = new URL(nclUrl, BUILD_CONFIG_ENDPOINT + query);
            HttpUtils.Response buildConfigResponse = HttpUtils.get(buildConfigUrl, new HashMap<String, String>() {{
                put("Authentication", "Bearer " + oauthToken);
                put("Content-Type", "application/json");
                put("Accept", "application/json");
            }});

            // Handle response
            if (buildConfigResponse.getResponseCode() == 204) {
                throw new Failure("Build config '" + form.getString("BuildConfigName") + "' not found.");
            } else if (buildConfigResponse.getResponseCode() / 100 != 2) {
                try {
                    String errMessage = JSONObject.fromObject(
                            buildConfigResponse.getContent()).getString("errorMessage");
                    throw new Failure("Build config lookup error: " + errMessage);
                } catch (JSONException e) {
                    throw new Failure("Build config lookup error: " + buildConfigResponse.getResponseCode() +
                            " " + buildConfigResponse.getResponseMessage());
                }
            }
            JSONObject buildConfigJson = JSONObject.fromObject(buildConfigResponse.getContent());
            String buildId = buildConfigJson.getJSONArray("content").getJSONObject(0).getString("id");

            // Send the request
            URL buildRequestUrl = new URL(nclUrl, String.format(BUILD_TRIGGER_ENDPOINT, buildId));
            HttpUtils.Response buildRequestResponse = HttpUtils.post(buildRequestUrl, form.toString(),
                    new HashMap<String, String>() {{
                put("Authentication", "Bearer " + oauthToken);
                put("Content-Type", "application/json");
                put("Accept", "application/json");
            }});
            if (buildRequestResponse.getResponseCode() / 100 != 2) {
                try {
                    String errMessage = JSONObject.fromObject(
                            buildRequestResponse.getContent()).getString("errorMessage");
                    throw new Failure("Build request error: " + errMessage);
                } catch (JSONException e) {
                    throw new Failure("Build request error: " + buildConfigResponse.getResponseCode() +
                            " " + buildConfigResponse.getResponseMessage());
                }
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

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getJavaVersion() {
        return this.javaVersion;
    }

    public String getGav() {
        return gav;
    }

    public void setGav(String gav) {
        this.gav = gav;
    }

    public String getMavenVersion() {
        return mavenVersion;
    }

    public void setMavenVersion(String mavenVersion) {
        this.mavenVersion = mavenVersion;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }

    public String getCommandLineParameters() {
        return commandLineParameters;
    }

    public void setCommandLineParameters(String commandLineParameters) {
        this.commandLineParameters = commandLineParameters;
    }

    public String getScm() {
        return scm;
    }

    public void setScm(String scm) {
        this.scm = scm;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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
}

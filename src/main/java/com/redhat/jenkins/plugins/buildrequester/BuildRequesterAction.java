package com.redhat.jenkins.plugins.buildrequester;

import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Failure;
import hudson.security.ACL;
import hudson.security.Permission;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author vdedik@redhat.com
 */
@SuppressWarnings("unchecked")
public class BuildRequesterAction implements Action {
    public static final Permission BUILD_REQUEST = AbstractProject.BUILD;

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

            // Remove keys that are empty (i.e. inputs without name)
            form.remove("");
            // Remove oauth as that is sent as http header
            form.remove("oauth");

            // Send the request
            URL nclUrl = new URL(getUrl());
            HttpURLConnection conn = (HttpURLConnection) nclUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authentication", "Bearer " + form.getString("oauth"));
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(form.toString());
            wr.flush();
            wr.close();

            int responseCode = conn.getResponseCode();

            if (responseCode / 100 != 2) {
                Object content = conn.getContent();
                String stringContent = null;
                if (content != null) {
                    stringContent = content.toString();
                }
                throw new Failure("Failed to send the build request: " + conn.getResponseMessage() + ", content: " + stringContent);
            }
        } catch (ServletException e) {
            throw new Failure("Error: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new Failure("Error: " + e.getMessage());
        } catch (IOException e) {
            throw new Failure("Error: " + e.getMessage());
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

package com.redhat.jenkins.plugins.buildrequester;

import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Failure;
import hudson.security.ACL;
import hudson.security.Permission;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
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
public class BuildRequesterAction implements Action {
    public static final Permission BUILD_REQUEST = AbstractProject.BUILD;

    private MavenModuleSetBuild build;
    private String url;

    // Props
    private String name;
    private String gav;
    private String javaVersion;
    private String mavenVersion;
    private String buildCommand;
    private String commandLineParameters;
    private String scm;
    private List<String> tags;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    @RequirePOST
    public HttpResponse doBuildRequestSubmit(StaplerRequest req) {
        getACL().checkPermission(BUILD_REQUEST);

        try {
            JSONObject form = req.getSubmittedForm();

            // Remove keys that are empty (i.e. inputs without name)
            form.remove("");

            // Send the request
            URL nclUrl = new URL(this.url);
            HttpURLConnection conn = (HttpURLConnection) nclUrl.openConnection();
            conn.setRequestMethod("POST");
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
                throw new Failure("Failed to send the build request: " + conn.getResponseMessage());
            }
        } catch (ServletException e) {
            throw new Failure("Exception: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new Failure("Exception: " + e.getMessage());
        } catch (IOException e) {
            throw new Failure("Exception: " + e.getMessage());
        }
        return new HttpRedirect("..");
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
}

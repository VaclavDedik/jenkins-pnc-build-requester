package com.redhat.jenkins.plugins.buildrequester;

import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.util.FormApply;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * @author vdedik@redhat.com
 */
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
    private String tag;

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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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
        return FormApply.success("..");
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

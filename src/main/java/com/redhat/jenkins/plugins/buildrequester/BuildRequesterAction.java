package com.redhat.jenkins.plugins.buildrequester;

import hudson.maven.AbstractMavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.EnvironmentList;
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

    private AbstractMavenBuild build;

    public void setBuild(AbstractMavenBuild build) {
        this.build = build;
    }

    public AbstractMavenBuild getBuild() {
        return this.build;
    }

    public MavenModuleSetBuild getMavenModuleSetBuild() {
        return (MavenModuleSetBuild) this.build;
    }

    public MavenModule getRootPOM() {
        return getMavenModuleSetBuild().getProject().getRootModule();
    }

    public String getGAV() {
        return String.format("%s:%s:%s", getRootPOM().getGroupId(),
                getRootPOM().getArtifactId(), getRootPOM().getVersion());
    }

    public String getMavenCommand() {
        return "mvn " + getMavenModuleSetBuild().getProject().getGoals();
    }

    public String getEnvironment() {
        EnvironmentList envVars = getMavenModuleSetBuild().getEnvironments();
        return "Env Vars: " + envVars.toString();
    }

    public String getJavaVersion() {
        return getMavenModuleSetBuild().getProject().getJDK().getBinDir().getAbsolutePath();
    }

    public String getScmUrl() {
        return getMavenModuleSetBuild().getProject().getScm().getKey();
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

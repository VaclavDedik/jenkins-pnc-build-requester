package com.redhat.jenkins.plugins.buildrequester;

import com.redhat.jenkins.plugins.buildrequester.scm.GitRepository;
import com.redhat.jenkins.plugins.buildrequester.scm.Repository;
import com.redhat.jenkins.plugins.buildrequester.scm.SubversionRepository;
import hudson.*;
import hudson.maven.AbstractMavenProject;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.search.Search;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vdedik@redhat.com
 */
public class BuildRequesterPublisher extends Recorder {
    private BuildRequesterAction action;
    private String url;
    private String keycloakSettings;

    @DataBoundConstructor
    public BuildRequesterPublisher(String url, String keycloakSettings) {
        this.url = url;
        this.keycloakSettings = keycloakSettings;
    }

    public String getUrl() {
        return url;
    }

    public String getKeycloakSettings() {
        return keycloakSettings;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
        if (build instanceof MavenModuleSetBuild) {
            action = new BuildRequesterAction();
            action.setBuild((MavenModuleSetBuild) build);
        }
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (action != null) {
            MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
            MavenModule rootPom = mavenBuild.getProject().getRootModule();
            File projectDir = new File(build.getWorkspace().getRemote(),
                    mavenBuild.getProject().getRootPOM(build.getEnvironment(listener))).getParentFile();
            Repository repo = getRepository(
                    mavenBuild.getProject().getScm(), listener, build.getEnvironment(listener), projectDir);


            // Set name
            action.setName(rootPom.getArtifactId());

            // Set name
            String gav = String.format("%s-%s-%s", rootPom.getGroupId(), rootPom.getArtifactId(),
                    rootPom.getVersion());
            action.setName(gav.replaceAll("[^A-Za-z0-9_.-]", ""));

            // Set SCM Url
            action.setScmRepoURL(repo.getUrl());

            // Set Tags
            String headCommitId = repo.getHeadCommitId();
            List<String> tags = repo.getTagsByCommitId(headCommitId);
            tags.add(headCommitId);
            action.setScmRevisions(tags);

            // Set Build Script
            action.setBuildScript("mvn " + mavenBuild.getProject().getGoals());

            mavenBuild.addAction(action);
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String defaultUrl;
        private String defaultKeycloakSettings;

        public DescriptorImpl() {
            load();
            if (defaultKeycloakSettings == null) {
                defaultKeycloakSettings = getKeycloakSettingsTemplate();
                save();
            }
        }

        public String getDefaultUrl() {
            return defaultUrl;
        }

        public String getDefaultKeycloakSettings() {
            return defaultKeycloakSettings;
        }

        public String getKeycloakSettingsTemplate() {
            return "{\n  \"realm\": \"\",\n  \"realm-public-key\": \"\",\n  \"auth-server-url\": \"\",\n  "
                    + "\"ssl-required\": \"\",\n  \"resource\": \"\",\n  \"credentials\": {\n    "
                    + "\"secret\": \"\"\n  }\n}\n";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject form) throws FormException {
            defaultUrl = form.getString("defaultUrl");
            defaultKeycloakSettings = form.getString("defaultKeycloakSettings");
            save();
            return super.configure(req, form);
        }

        @Override
        public BuildRequesterPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(BuildRequesterPublisher.class, formData);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return AbstractMavenProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Configure Handover to Productization";
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Url must not be empty.");
            }

            try {
                URL u = new URL(value);
                u.toURI();
            } catch (Exception e) {
                return FormValidation.error("Url must be a valid.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckKeycloakSettings(@QueryParameter String value) {
            JSONObject keycloakSettings;
            try {
                keycloakSettings = JSONObject.fromObject(value);
            } catch (JSONException e) {
                return FormValidation.error("Malformed JSON.");
            }

            if (!keycloakSettings.containsKey("realm")) {
                return FormValidation.error("Keycloak Settings must contain realm.");
            }

            if (!keycloakSettings.containsKey("resource")) {
                return FormValidation.error("Keycloak Settings must contain resource.");
            }

            if (!keycloakSettings.containsKey("realm-public-key")) {
                return FormValidation.error("Keycloak Settings must contain realm-public-key.");
            }

            if (!keycloakSettings.containsKey("auth-server-url")) {
                return FormValidation.error("Keycloak Settings must contain auth-server-url.");
            }

            if (!keycloakSettings.containsKey("credentials")) {
                return FormValidation.error("Keycloak Settings must contain credentials.");
            }

            return FormValidation.ok();
        }
    }

    private Repository getRepository(SCM scm, TaskListener taskListener, EnvVars envVars, File repoDir) {
        Repository repository = null;
        if (scm instanceof GitSCM) {
            repository = new GitRepository(taskListener, envVars, repoDir);
        } else if (scm instanceof SubversionSCM) {
            repository = new SubversionRepository(repoDir);
        }

        return repository;
    }

}

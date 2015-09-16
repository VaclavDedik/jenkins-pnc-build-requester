package com.redhat.jenkins.plugins.buildrequester;

import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.maven.AbstractMavenProject;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vdedik@redhat.com
 */
public class BuildRequesterPublisher extends Recorder {
    private BuildRequesterAction action;

    @DataBoundConstructor
    public BuildRequesterPublisher() {
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

            // Set name
            action.setName(rootPom.getArtifactId());

            // Set GAV
            String gav = String.format("%s:%s:%s", rootPom.getGroupId(), rootPom.getArtifactId(),
                    rootPom.getVersion());
            action.setGav(gav);

            // Set SCM Url
            SCM scm = mavenBuild.getProject().getScm();
            action.setScm(getScmUrl(scm));
            System.out.println("************************");
            System.out.println(mavenBuild.getEnvironment(listener).get("GIT_URL", "Null"));
            System.out.println(mavenBuild.getEnvironment(listener).get("SVN_URL", "Null"));

            // Set java version
            Proc proc = launcher.launch()
                    .cmds("$JAVA_HOME/bin/java", "-version")
                    .envs(mavenBuild.getEnvironment(listener))
                    .readStdout()
                    .start();
            if (proc.join() == 0) {
                String javaVersionOut = IOUtils.toString(proc.getStdout(), build.getCharset());
                Pattern pattern = Pattern.compile("java version \"(.*)\"");
                Matcher matcher = pattern.matcher(javaVersionOut);
                if (matcher.find()) {
                    action.setJavaVersion(matcher.group(1));
                }
            }

            // Set Maven Version
            action.setMavenVersion(mavenBuild.getMavenVersionUsed());

            // Set Maven Command
            action.setBuildCommand("mvn " + mavenBuild.getProject().getGoals());

            // Set Command Line Params (MAVEN_OPTS)
            action.setCommandLineParameters(mavenBuild.getProject().getMavenOpts());

            mavenBuild.addAction(action);
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return AbstractMavenProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Build Requester Publisher";
        }
    }

    private String getScmUrl(SCM scm) {
        String scmUrl = "";

        if (scm instanceof GitSCM) {
            GitSCM gitSCM = (GitSCM) scm;
            List<UserRemoteConfig> remoteConfigs = gitSCM.getUserRemoteConfigs();
            if (!remoteConfigs.isEmpty()) {
                // Select first repository url
                scmUrl = remoteConfigs.get(0).getUrl();
            }
        } else if (scm instanceof SubversionSCM) {
            SubversionSCM svmSCM = (SubversionSCM) scm;
            SubversionSCM.ModuleLocation[] locations = svmSCM.getLocations();
            if (locations != null && locations.length > 0) {
                // Select first repository url
                scmUrl = locations[0].getURL();
            }
        }

        return scmUrl;
    }

}

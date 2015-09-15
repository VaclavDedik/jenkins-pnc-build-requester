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
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
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

            // Set java version
            Proc proc = launcher.launch().cmds("java", "-version").readStdout().start();
            if (proc.join() == 0) {
                String javaVersionOut = IOUtils.toString(proc.getStdout(), build.getCharset());
                Pattern pattern = Pattern.compile("java version \"(.*)\"");
                Matcher matcher = pattern.matcher(javaVersionOut);
                if (matcher.matches()) {
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

}

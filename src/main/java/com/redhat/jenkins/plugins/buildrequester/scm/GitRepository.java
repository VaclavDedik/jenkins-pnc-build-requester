package com.redhat.jenkins.plugins.buildrequester.scm;

import hudson.EnvVars;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author vdedik@redhat.com
 */
public class GitRepository implements Repository {
    private GitClient gitClient;

    public GitRepository(TaskListener taskListener, EnvVars envVars, File repoDir) {
        try {
            this.gitClient = Git.with(taskListener, envVars).in(repoDir).getClient();
        } catch (Exception e) {
            this.gitClient = null;
        }
    }

    @Override
    public String getUrl() {
        if (gitClient == null) {
            return "";
        }

        try {
            return gitClient.getRemoteUrl("origin");
        } catch (InterruptedException e) {
            return "";
        }
    }

    @Override
    public String getHeadCommitId() {
        try {
            return gitClient.revParse("origin/master").getName();
        } catch (InterruptedException e) {
            return "";
        }
    }

    @Override
    public List<String> getTagsByCommitId(String commitId) {
        List<String> stringRefs = new ArrayList<String>();
        try {
            Set<String> refs = gitClient.getRefNames("refs/tags/");

            for (String ref : refs) {
                if (commitId.equals(gitClient.revParse(ref).getName())) {
                    stringRefs.add(ref.replace("refs/tags/", ""));
                }
            }

            return stringRefs;
        } catch (InterruptedException e) {
            return stringRefs;
        }
    }
}

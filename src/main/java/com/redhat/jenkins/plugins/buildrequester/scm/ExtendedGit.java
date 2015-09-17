package com.redhat.jenkins.plugins.buildrequester.scm;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;

import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public class ExtendedGit implements ExtendedSCM {
    private GitSCM gitSCM;

    public ExtendedGit(GitSCM gitSCM) {
        this.gitSCM = gitSCM;
    }

    @Override
    public String getUrl() {
        String scmUrl = "";
        List<UserRemoteConfig> remoteConfigs = gitSCM.getUserRemoteConfigs();
        if (!remoteConfigs.isEmpty()) {
            // Select first repository url
            scmUrl = remoteConfigs.get(0).getUrl();
        }

        return scmUrl;
    }

    @Override
    public String getHeadCommitId() {
        return null;
    }

    @Override
    public String getCommitIdByRef(String ref) {
        return null;
    }

    @Override
    public Map<String, String> getTags() {
        return null;
    }
}

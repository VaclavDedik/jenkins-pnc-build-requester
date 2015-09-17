package com.redhat.jenkins.plugins.buildrequester.scm;

import hudson.scm.SubversionSCM;

import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public class ExtendedSubversion implements ExtendedSCM {
    private SubversionSCM subversionSCM;

    public ExtendedSubversion(SubversionSCM subversionSCM) {
        this.subversionSCM = subversionSCM;
    }

    @Override
    public String getUrl() {
        String scmUrl = "";
        SubversionSCM.ModuleLocation[] locations = subversionSCM.getLocations();
        if (locations != null && locations.length > 0) {
            // Select first repository url
            scmUrl = locations[0].getURL();
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

package com.redhat.jenkins.plugins.buildrequester.scm;

import hudson.scm.SubversionSCM;

import java.util.List;

/**
 * @author vdedik@redhat.com
 */
public class SubversionRepository implements Repository {
    private SubversionSCM subversionSCM;
    private Integer index;
    private SubversionSCM.ModuleLocation module;

    public SubversionRepository(SubversionSCM subversionSCM) {
        this(subversionSCM, 0);
    }

    public SubversionRepository(SubversionSCM subversionSCM, Integer index) {
        this.subversionSCM = subversionSCM;
        this.index = index;

        SubversionSCM.ModuleLocation[] locations = subversionSCM.getLocations();
        if (locations != null && locations.length >= this.index) {
            module = locations[this.index];
        }
    }

    @Override
    public String getUrl() {
        if (module == null) {
            return "";
        }

        return module.getURL();
    }

    @Override
    public String getHeadCommitId() {
        return null;
    }

    @Override
    public List<String> getTagsByCommitId(String commitId) {
        return null;
    }
}

package com.redhat.jenkins.plugins.buildrequester;

import hudson.model.AbstractBuild;
import hudson.model.Action;

/**
 * @author vdedik@redhat.com
 */
public class BuildRequesterAction implements Action {
    private AbstractBuild build;

    public void setBuild(AbstractBuild build) {
        this.build = build;
    }

    public AbstractBuild getBuild() {
        return this.build;
    }

    @Override
    public String getIconFileName() {
        return "/images/24x24/next.png";
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

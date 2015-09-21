package com.redhat.jenkins.plugins.buildrequester.scm;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author vdedik@redhat.com
 */
public class SubversionRepository implements Repository {
    private SVNStatus status;

    public SubversionRepository(File repoDir) {
        try {
            status = SVNClientManager.newInstance().getStatusClient().doStatus(repoDir, false);
        } catch (SVNException e) {
            status = null;
        }
    }

    @Override
    public String getUrl() {
        if (status == null) {
            return "";
        }

        return status.getURL().toString();
    }

    @Override
    public String getHeadCommitId() {
        return String.valueOf(status.getRevision().getNumber());
    }

    /**
     * There is no easy way to get the list of tags from a subversion repository, so return empty list for now
     */
    @Override
    public List<String> getTagsByCommitId(String commitId) {
        return new ArrayList<String>();
    }
}

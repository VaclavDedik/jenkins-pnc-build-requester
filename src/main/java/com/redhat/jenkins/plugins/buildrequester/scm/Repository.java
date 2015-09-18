package com.redhat.jenkins.plugins.buildrequester.scm;

import java.util.List;

/**
 * Unfortunatelly, hudson.scm.SCM does not support the methods we need. So here is an extended SCM interface that does.
 *
 * @author vdedik@redhat.com
 */
public interface Repository {

    String getUrl();

    String getHeadCommitId();

    List<String> getTagsByCommitId(String commitId);
}

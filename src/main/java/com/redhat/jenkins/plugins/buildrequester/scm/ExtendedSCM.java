package com.redhat.jenkins.plugins.buildrequester.scm;

import java.util.Map;

/**
 * Unfortunatelly, hudson.scm.SCM does not support the methods we need. So here is an extended SCM interface that does.
 *
 * @author vdedik@redhat.com
 */
public interface ExtendedSCM {

    String getUrl();

    String getHeadCommitId();

    String getCommitIdByRef(String ref);

    Map<String, String> getTags();
}

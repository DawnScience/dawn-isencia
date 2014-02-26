package org.dawnsci.drmaa.executor.test;

import static org.junit.Assert.*;

import org.dawnsci.drmaa.executor.JobTemplateImpl;
import org.dawnsci.drmaa.executor.SessionImpl;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.NoActiveSessionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSessionImpl {
  private SessionImpl session;

  @Before
  public void setUp() throws Exception {
    session = new SessionImpl();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testInit() throws DrmaaException {
    session.init(null);
    assertEquals("localhost", session.getContact());
  }

  @Test
  public void testExit() {
    fail("Not yet implemented");
  }

  @Test
  public void testCreateJobTemplateNoInit() throws DrmaaException {
    try {
      session.createJobTemplate();
      fail("createJobTemplate should fail for uninitialized session");
    } catch (NoActiveSessionException e) {
      // this is as it should be
    }
  }

  @Test
  public void testDeleteJobTemplateNoInit() throws DrmaaException {
    try {
      session.deleteJobTemplate(null);
      fail("deleteJobTemplate should fail for uninitialized session");
    } catch (NoActiveSessionException e) {
      // this is as it should be
    }
  }

  @Test
  public void testCreateJobTemplate() throws DrmaaException {
    session.init(null);
    JobTemplate jobTemplate = session.createJobTemplate();
    assertTrue("Session must create an instance of our JobTemplateImpl", jobTemplate instanceof JobTemplateImpl);
  }

  @Test
  public void testDeleteJobTemplate() {
    fail("Not yet implemented");
  }

  @Test
  public void testRunJob() throws DrmaaException, InterruptedException {
    session.init(null);
    JobTemplate jobTemplate = session.createJobTemplate();
    jobTemplate.setRemoteCommand("notepad");
    session.runJob(jobTemplate);
    Thread.sleep(1000);
  }

  @Test
  public void testGetContact() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetVersion() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetDrmSystem() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetDrmaaImplementation() {
    fail("Not yet implemented");
  }

}

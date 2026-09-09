/*-
 * #%L
 * OTP Field Add-On
 * %%
 * Copyright (C) 2026 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.flowingcode.vaadin.addons.otpfield.it;

import com.vaadin.testbench.ScreenshotOnFailureRule;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.parallel.ParallelTest;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Base class for ITs
 *
 * <p>The tests use Chrome driver (see pom.xml for integration-tests profile) to run integration
 * tests on a headless Chrome. If a property {@code test.use .hub} is set to true, {@code
 * AbstractViewTest} will assume that the TestBench test is running in a CI environment. In order to
 * keep the this class light, it makes certain assumptions about the CI environment (such as
 * available environment variables). It is not advisable to use this class as a base class for you
 * own TestBench tests.
 *
 * <p>To learn more about TestBench, visit <a
 * href="https://vaadin.com/docs/v10/testbench/testbench-overview.html">Vaadin TestBench</a>.
 */
public abstract class AbstractViewTest extends ParallelTest {
  /** How long to wait for the frontend build on the first navigation. */
  private static final int STARTUP_TIMEOUT_SECONDS = 60;

  /** How many times to reload before giving up on the demo becoming ready. */
  private static final int NAVIGATION_ATTEMPTS = 3;

  /** The port the demo is served on, overridable with {@code -Dtest.server.port}. */
  private static final int SERVER_PORT = Integer.getInteger("test.server.port", 8080);

  private final String route;

  @Rule public ScreenshotOnFailureRule rule = new ScreenshotOnFailureRule(this, true);

  public AbstractViewTest() {
    this("");
  }

  protected AbstractViewTest(String route) {
    this.route = route;
  }

  @BeforeClass
  public static void setupClass() {
    WebDriverManager.chromedriver().setup();
  }

  @Override
  @Before
  public void setup() throws Exception {
    if (isUsingHub()) {
      super.setup();
    } else {
      setDriver(TestBench.createDriver(new ChromeDriver(chromeOptions())));
    }
    open(route);
  }

  /**
   * Navigates to a route of the demo and waits until the Flow client has bootstrapped.
   * <p>
   * In development mode the frontend dev server may still be compiling when the first test
   * navigates, which leaves the browser on a page whose bundle never loaded. Reloading is the only
   * way out of that, so the navigation is retried a few times.
   *
   * @param route the route to open, relative to the deployment
   */
  protected void open(String route) {
    for (int attempt = 0; attempt < NAVIGATION_ATTEMPTS; attempt++) {
      getDriver().get(getURL(route));
      try {
        waitUntil(driver -> isClientBootstrapped(), STARTUP_TIMEOUT_SECONDS);
        return;
      } catch (TimeoutException e) {
        // The frontend was probably still being built; navigate again.
      }
    }
    throw new IllegalStateException("The demo did not become ready at " + getURL(route));
  }

  private boolean isClientBootstrapped() {
    return Boolean.TRUE.equals(((JavascriptExecutor) getDriver()).executeScript(
        "const flow = window.Vaadin && window.Vaadin.Flow;"
            + "return !!(flow && flow.clients && Object.keys(flow.clients).length > 0)"));
  }

  /**
   * Chrome runs headless unless {@code -Dtest.headed=true} is passed, so that the tests behave the
   * same on a CI runner without a display.
   *
   * @return the options to create the driver with
   */
  protected ChromeOptions chromeOptions() {
    ChromeOptions options = new ChromeOptions();
    if (!Boolean.getBoolean("test.headed")) {
      options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox",
          "--disable-dev-shm-usage", "--window-size=1280,1024");
    }
    return options;
  }

  /**
   * Returns deployment host name concatenated with route.
   *
   * @return URL to route
   */
  private static String getURL(String route) {
    return String.format("http://%s:%d/%s", getDeploymentHostname(), SERVER_PORT, route);
  }

  /** Property set to true when running on a test hub. */
  private static final String USE_HUB_PROPERTY = "test.use.hub";

  /**
   * Returns whether we are using a test hub. This means that the starter is running tests in
   * Vaadin's CI environment, and uses TestBench to connect to the testing hub.
   *
   * @return whether we are using a test hub
   */
  private static boolean isUsingHub() {
    return Boolean.TRUE.toString().equals(System.getProperty(USE_HUB_PROPERTY));
  }

  /**
   * If running on CI, get the host name from environment variable HOSTNAME
   *
   * @return the host name
   */
  private static String getDeploymentHostname() {
    return isUsingHub() ? System.getenv("HOSTNAME") : "localhost";
  }
}

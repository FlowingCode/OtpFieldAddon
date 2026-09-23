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

import static org.junit.Assert.assertTrue;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Runs axe-core over the demo views.
 * <p>
 * Accessibility is not a requirement of this component (§11 of the specification is withdrawn), so
 * this scan is kept for the signal it gives rather than as a gate. It is scoped to the field itself:
 * a whole-page scan reports mostly on the demo scaffolding and on the palette of whichever theme is
 * active, neither of which this add-on controls.
 */
public class OtpFieldAxeIT extends AbstractViewTest {

  /** Findings at or above this impact fail the test. */
  private static final List<String> BLOCKING = Arrays.asList("serious", "critical");

  public OtpFieldAxeIT() {
    super("otpfield/basic");
  }

  private void scan(String route) {
    open(route);
    OtpFieldElement otp = $(OtpFieldElement.class).waitForFirst();

    Results results = new AxeBuilder().analyze(getDriver(), otp);
    if (results.isErrored()) {
      throw new IllegalStateException("axe failed to run: " + results.getErrorMessage());
    }

    System.out.println("AXE " + route + ": " + results.getViolations().size() + " violation(s), "
        + results.getPasses().size() + " pass(es), " + results.getIncomplete().size()
        + " incomplete");
    results.getViolations().forEach(OtpFieldAxeIT::describe);
    results.getIncomplete().forEach(rule -> System.out.println("AXE   incomplete: " + rule.getId()));

    List<String> blocking = results.getViolations().stream()
        .filter(rule -> BLOCKING.contains(String.valueOf(rule.getImpact()).toLowerCase()))
        .map(Rule::getId)
        .collect(Collectors.toList());
    assertTrue(route + " has blocking accessibility violations: " + blocking, blocking.isEmpty());
  }

  private static void describe(Rule rule) {
    System.out.println("AXE   " + rule.getImpact() + " [" + rule.getId() + "] " + rule.getHelp());
    for (CheckedNode node : rule.getNodes()) {
      System.out.println("AXE     target=" + node.getTarget() + " html=" + node.getHtml());
    }
  }

  @Test
  public void theBasicFieldIsFreeOfBlockingViolations() {
    scan("otpfield/basic");
  }

  @Test
  public void theBinderFieldIsFreeOfBlockingViolations() {
    // Carries a required indicator and, once loaded, an error message.
    scan("otpfield/binder");
  }

  @Test
  public void theThemedFieldsAreFreeOfBlockingViolations() {
    // The banking variant overrides the palette, which is where a contrast finding would surface.
    scan("otpfield/theming");
  }
}

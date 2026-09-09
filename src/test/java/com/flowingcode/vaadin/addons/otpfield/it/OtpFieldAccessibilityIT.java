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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * The accessibility contract of the field: one accessible node, presentational slots, and the
 * label, helper and error wiring that the shared field controllers provide.
 */
public class OtpFieldAccessibilityIT extends AbstractViewTest {

  private OtpFieldElement otp;

  public OtpFieldAccessibilityIT() {
    super("otpfield/basic");
  }

  @Before
  public void findField() {
    otp = $(OtpFieldElement.class).waitForFirst();
    System.out.println("A11Y " + otp.describeAccessibility());
  }

  @Test
  public void theFieldExposesExactlyOneFocusableNode() {
    // The slots are painted decoration; a screen reader must meet one field, not six.
    assertEquals(1, otp.countFocusableNodes());
    assertEquals("true", otp.getSlotsAriaHidden());
  }

  @Test
  public void theLabelIsAssociatedWithTheInput() {
    assertEquals(otp.getInputElement().getAttribute("id"), otp.getLabelFor());
    assertNotEquals("", otp.getInputElement().getAttribute("id"));
  }

  @Test
  public void theHelperTextIsAnnouncedWithTheField() {
    assertTrue("the helper text should be referenced by aria-describedby",
        otp.isDescribedByTheHelperText());
  }

  @Test
  public void theErrorMessageIsAnnouncedWhenTheFieldBecomesInvalid() {
    otp.setElementProperty("errorMessage", "Something is wrong");
    otp.setElementProperty("invalid", true);

    assertEquals("true", otp.getInputElement().getAttribute("aria-invalid"));
    // Vaadin 25 links the message for context and speaks it through a shared live region, rather
    // than putting role="alert" on the node, which made NVDA announce it twice.
    waitUntil(driver -> otp.isDescribedByTheErrorMessage());
  }

  @Test
  public void theRequiredStateIsExposedToAssistiveTechnology() {
    otp.setElementProperty("required", true);

    // The native required attribute is what assistive technology reads on a real <input>;
    // FieldAriaController deliberately leaves aria-required off in that case.
    assertNotEquals(null, otp.getInputElement().getAttribute("required"));
    assertEquals(null, otp.getInputElement().getAttribute("aria-required"));
  }

  @Test
  public void theInputCarriesTheHintsThatMobileAndPasswordManagersRelyOn() {
    assertEquals("one-time-code", otp.getInputElement().getAttribute("autocomplete"));
    assertEquals("numeric", otp.getInputElement().getAttribute("inputmode"));
    assertEquals("off", otp.getInputElement().getAttribute("autocorrect"));
    assertEquals("false", otp.getInputElement().getAttribute("spellcheck"));
  }
}

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
import static org.junit.Assert.assertTrue;

import com.vaadin.testbench.TestBenchElement;
import org.junit.Before;
import org.junit.Test;

/** Binder integration and case conversion, exercised on the binder demo. */
public class OtpFieldBinderIT extends AbstractViewTest {

  private OtpFieldElement otp;
  private TestBenchElement save;

  public OtpFieldBinderIT() {
    super("otpfield/binder");
  }

  @Before
  public void findComponents() {
    otp = $(OtpFieldElement.class).waitForFirst();
    save = $("vaadin-button").id("save");
  }

  @Test
  public void anEmptyRequiredFieldDoesNotPassValidation() {
    save.click();
    waitUntil(driver -> "The form is not valid yet".equals(statusText()));
  }

  @Test
  public void anIncompleteTokenDoesNotPassValidation() {
    otp.type("ABC");
    save.click();
    waitUntil(driver -> "The form is not valid yet".equals(statusText()));
    assertEquals("The token has 8 characters", otp.getErrorMessage());
  }

  @Test
  public void aCompleteTokenIsWrittenToTheBean() {
    otp.type("abcdef12");
    waitUntil(driver -> "ABCDEF12".equals(otp.getOtpValue()));
    assertTrue(otp.isSlotFilled(7));

    save.click();
    waitUntil(driver -> "Saved token: ABCDEF12".equals(statusText()));
  }

  @Test
  public void aStoredTokenThatNoLongerFitsIsReportedRatherThanThrown() {
    // Regression: setValue used to throw, which propagated out of Binder.setBean and left the
    // view broken instead of showing the constraint.
    $("vaadin-button").id("legacy").click();

    waitUntil(driver -> "Loaded a stored token that no longer fits this field".equals(statusText()));
    waitUntil(driver -> otp.isFieldInvalid());
    assertEquals("This token predates the current format", otp.getErrorMessage());

    // The value survives intact on the client; it is reported, not silently repaired.
    assertEquals("OLD-TOKEN-2019", otp.getOtpValue());
  }

  private String statusText() {
    return $("span").id("status").getText().trim();
  }
}

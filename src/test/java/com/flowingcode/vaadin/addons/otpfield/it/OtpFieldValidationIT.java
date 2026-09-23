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
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;

/** The built-in incomplete constraint, exercised on the basic demo. */
public class OtpFieldValidationIT extends AbstractViewTest {

  private OtpFieldElement otp;

  public OtpFieldValidationIT() {
    super("otpfield/basic");
  }

  @Before
  public void findField() {
    otp = $(OtpFieldElement.class).waitForFirst();
  }

  @Test
  public void anIncompleteCodeIsNotFlaggedWhileItIsBeingTyped() {
    otp.type("123");
    assertFalse(otp.isFieldInvalid());
  }

  @Test
  public void anIncompleteCodeIsFlaggedOnBlur() {
    otp.type("123");
    otp.type(Keys.TAB);

    waitUntil(driver -> otp.isFieldInvalid());
    assertEquals("The code is incomplete", otp.getErrorMessage());
  }

  @Test
  public void completingTheCodeClearsTheError() {
    otp.type("123");
    otp.type(Keys.TAB);
    waitUntil(driver -> otp.isFieldInvalid());

    otp.focusField();
    otp.type(Keys.END, "456");
    waitUntil(driver -> !otp.isFieldInvalid());
    assertEquals("123456", otp.getOtpValue());
  }
}

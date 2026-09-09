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

import org.junit.Before;
import org.junit.Test;

/**
 * The complete event and application-driven errors, exercised on the verification demo. That demo
 * uses {@code ValueChangeMode.ON_CHANGE}, so it also covers that partial codes are not transmitted
 * while the complete event still arrives.
 */
public class OtpFieldCompleteIT extends AbstractViewTest {

  private OtpFieldElement otp;

  public OtpFieldCompleteIT() {
    super("otpfield/verification");
  }

  @Before
  public void findField() {
    otp = $(OtpFieldElement.class).waitForFirst();
  }

  @Test
  public void aCompleteCodeReachesTheServer() {
    otp.type("123456");
    waitUntil(driver -> "Code accepted".equals(resultText()));
  }

  @Test
  public void anIncompleteCodeIsNotTransmitted() {
    otp.type("12345");
    assertEquals("", resultText());
  }

  @Test
  public void aWrongCodeIsReportedByTheApplication() {
    otp.type("999999");
    waitUntil(driver -> otp.isFieldInvalid());

    assertEquals("Incorrect code", otp.getErrorMessage());
    assertEquals("", otp.getOtpValue());
    assertEquals("", resultText());
  }

  @Test
  public void theCompleteEventFiresAgainAfterTheCodeBecomesIncomplete() {
    otp.type("999999");
    waitUntil(driver -> otp.isFieldInvalid());

    otp.type("123456");
    waitUntil(driver -> "Code accepted".equals(resultText()));
  }

  private String resultText() {
    return $("span").id("result").getText().trim();
  }
}

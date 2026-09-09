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

import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Two fields on one view must not steal each other's clicks. */
public class OtpFieldThemingIT extends AbstractViewTest {

  private OtpFieldElement compact;
  private OtpFieldElement banking;

  public OtpFieldThemingIT() {
    super("otpfield/theming");
  }

  @Before
  public void findFields() {
    List<OtpFieldElement> fields = $(OtpFieldElement.class).all();
    compact = fields.get(0);
    banking = fields.get(1);
    System.out.println("THEMING " + compact.describeLayering());
  }

  @Test
  public void theInputCoversItsOwnSlotsAndNothingElse() {
    // The input is layered over the slots, so it must be bounded by them. An input that escapes
    // its field spreads across the view and swallows every click behind it.
    assertTrue("the compact input is not bounded by its own field", compact.isInputBoundedByField());
    assertTrue("the banking input is not bounded by its own field", banking.isInputBoundedByField());
  }

  @Test
  public void clickingTheCompactFieldReachesTheCompactField() {
    // Regression: the second field's input used to cover the first one.
    assertEquals("fc-otp-field", compact.getTopmostElementAtCentre());
    compact.clickSlot(1);
    compact.type("7");

    assertEquals("7", compact.getOtpValue());
    assertEquals("", banking.getOtpValue());
  }

  @Test
  public void clickingTheBankingFieldReachesTheBankingField() {
    banking.clickSlot(0);
    banking.type("A");

    assertEquals("A", banking.getOtpValue());
    assertEquals("", compact.getOtpValue());
  }
}

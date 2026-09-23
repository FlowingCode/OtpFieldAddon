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

/** A prefix and a suffix share the field with the slots, exercised on the prefix/suffix demo. */
public class OtpFieldPrefixSuffixIT extends AbstractViewTest {

  private OtpFieldElement otp;

  public OtpFieldPrefixSuffixIT() {
    super("otpfield/prefix-suffix");
  }

  @Before
  public void findField() {
    otp = $(OtpFieldElement.class).waitForFirst();
  }

  @Test
  public void everySlotIsStillRenderedBesideThePrefixAndSuffix() {
    assertEquals(6, otp.getSlotCount());

    // An empty prefix slot must not take part in the flex layout, and a filled one must not push
    // any slot out of the field.
    for (Number width : otp.getSlotWidths()) {
      assertTrue("a slot collapsed to " + width, width.doubleValue() > 1);
    }
    assertTrue("a slot fell outside the field", otp.areAllSlotsWithinTheField());
  }

  @Test
  public void thePrefixAndSuffixDoNotInterceptTyping() {
    otp.type("123456");

    assertEquals("123456", otp.getOtpValue());
    assertEquals(List.of("1", "2", "3", "4", "5", "6"), otp.getSlotTexts());
  }

  @Test
  public void clickingASlotStillPlacesTheCaretDespiteThePrefix() {
    // The prefix shifts the slots to the right, so this also covers the slot-to-caret mapping
    // being derived from measured geometry rather than an assumed origin.
    otp.type("123456");
    otp.clickSlot(3);

    assertEquals(3, otp.getSelectionStart());
    assertEquals(3, otp.getActiveSlotIndex());
  }
}

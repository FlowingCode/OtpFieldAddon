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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;

/** Client-side behaviour of the field, exercised on the basic demo. */
public class OtpFieldIT extends AbstractViewTest {

  /** The default mask glyph of the element. */
  private static final String MASK = "\u2022";

  private OtpFieldElement otp;

  public OtpFieldIT() {
    super("otpfield/basic");
  }

  @Before
  public void findField() {
    otp = $(OtpFieldElement.class).waitForFirst();
  }

  // -- Structure -------------------------------------------------------------------------------

  @Test
  public void rendersOneSlotPerCharacter() {
    assertEquals(6, otp.getSlotCount());
    assertEquals(List.of("", "", "", "", "", ""), otp.getSlotTexts());
  }

  @Test
  public void changingTheLengthReRendersTheSlots() {
    otp.setElementProperty("length", 4);
    assertEquals(4, otp.getSlotCount());
  }

  @Test
  public void theWholeComponentIsOneTabStop() {
    otp.type("12");
    assertTrue(otp.isFieldFocused());

    otp.type(Keys.TAB);
    assertFalse(otp.isFieldFocused());
  }

  @Test
  public void theSlotsAreHiddenFromAssistiveTechnology() {
    assertEquals("true", otp.getSlotsAriaHidden());
    // The single real input is the only focusable node, and it is labelled by the field.
    assertEquals("one-time-code", otp.getInputElement().getAttribute("autocomplete"));
    assertNotEquals("", otp.getInputElement().getAttribute("id"));
  }

  // -- Typing (FR-8, FR-9, FR-10, FR-11) -------------------------------------------------------

  @Test
  public void typingFillsTheSlotsAndAdvances() {
    otp.type("1");
    assertEquals(List.of("1", "", "", "", "", ""), otp.getSlotTexts());
    assertTrue(otp.isSlotFilled(0));
    assertEquals(1, otp.getActiveSlotIndex());

    otp.type("23");
    assertEquals(List.of("1", "2", "3", "", "", ""), otp.getSlotTexts());
    assertEquals(3, otp.getActiveSlotIndex());
    assertEquals("123", otp.getOtpValue());
  }

  @Test
  public void typingBeyondTheLastSlotHasNoEffect() {
    otp.type("1234567");
    assertEquals("123456", otp.getOtpValue());
    assertEquals(6, otp.getSelectionStart());
  }

  @Test
  public void typingOverwritesTheActiveSlot() {
    otp.type("123456");
    otp.type(Keys.HOME);
    otp.type("9");
    assertEquals("923456", otp.getOtpValue());
    assertEquals(1, otp.getSelectionStart());
  }

  @Test
  public void disallowedCharactersAreRejectedSilently() {
    otp.type("1a2");
    assertEquals("12", otp.getOtpValue());
  }

  @Test
  public void backspaceClearsTheCharacterBeforeTheCaret() {
    otp.type("123");
    otp.type(Keys.BACK_SPACE);
    assertEquals("12", otp.getOtpValue());
    assertEquals(2, otp.getActiveSlotIndex());

    otp.type(Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE);
    assertEquals("", otp.getOtpValue());
    assertEquals(0, otp.getActiveSlotIndex());
  }

  @Test
  public void deleteClearsTheCharacterAtTheCaret() {
    otp.type("123");
    otp.type(Keys.HOME, Keys.DELETE);
    assertEquals("23", otp.getOtpValue());
    assertEquals(0, otp.getSelectionStart());
  }

  // -- Caret navigation (FR-12, FR-13) ---------------------------------------------------------

  @Test
  public void arrowKeysAndHomeEndMoveTheActiveSlot() {
    otp.type("1234");

    otp.type(Keys.ARROW_LEFT);
    assertEquals(3, otp.getActiveSlotIndex());

    otp.type(Keys.ARROW_RIGHT);
    assertEquals(4, otp.getActiveSlotIndex());

    otp.type(Keys.HOME);
    assertEquals(0, otp.getActiveSlotIndex());

    otp.type(Keys.END);
    assertEquals(4, otp.getActiveSlotIndex());
  }

  @Test
  public void selectAllSelectsTheWholeCode() {
    otp.type("123456");
    otp.type(Keys.chord(Keys.CONTROL, "a"));
    assertEquals(0, otp.getSelectionStart());
    assertEquals(6, otp.getSelectionEnd());
  }

  @Test
  public void clickingASlotPlacesTheCaretThere() {
    otp.type("123456");
    otp.clickSlot(2);
    assertEquals(2, otp.getSelectionStart());
    assertEquals(2, otp.getActiveSlotIndex());
  }

  @Test
  public void typingRightAfterAClickFillsConsecutiveSlots() {
    // A click must leave focus and the caret in a state that the very next keystrokes can build
    // on, without the component racing the browser for either.
    otp.clickSlot(0);
    otp.type("12");

    assertEquals("12", otp.getOtpValue());
    assertEquals(List.of("1", "2", "", "", "", ""), otp.getSlotTexts());
    assertEquals(2, otp.getSelectionStart());
  }

  @Test
  public void typingBeforeTheButtonIsReleasedStillFillsConsecutiveSlots() {
    // Press, type, release is an ordinary mouse gesture, so the release arrives after the first
    // keystroke. Nothing may move the caret at that point, or the next character would overwrite
    // the one just entered.
    otp.pressAndHoldSlot(0);
    otp.type("1");
    otp.releasePointer();
    otp.type("2");

    assertEquals("12", otp.getOtpValue());
    assertEquals(List.of("1", "2", "", "", "", ""), otp.getSlotTexts());
  }

  @Test
  public void movingThePointerBetweenKeystrokesDoesNotMoveTheCaret() {
    otp.clickSlot(0);
    new Actions(getDriver()).sendKeys("1").perform();
    new Actions(getDriver()).moveToElement(otp.getInputElement(), 1, 0).perform();
    new Actions(getDriver()).sendKeys("2").perform();

    assertEquals("12", otp.getOtpValue());
  }

  @Test
  public void clickingPastTheLastFilledSlotLandsAfterTheLastCharacter() {
    otp.type("12");
    otp.clickSlot(5);
    assertEquals(2, otp.getSelectionStart());
  }

  // -- Paste and drop (FR-16, FR-17, FR-18) ----------------------------------------------------

  @Test
  public void pasteDistributesTheCodeOverTheSlots() {
    otp.focusField();
    otp.paste("123456");
    assertEquals("123456", otp.getOtpValue());
    assertEquals(List.of("1", "2", "3", "4", "5", "6"), otp.getSlotTexts());
  }

  @Test
  public void pasteStripsSeparatorsAndDisallowedCharacters() {
    otp.focusField();
    otp.paste("123-456");
    assertEquals("123456", otp.getOtpValue());

    otp.type(Keys.chord(Keys.CONTROL, "a"));
    otp.paste("Code: 98 76 54");
    assertEquals("987654", otp.getOtpValue());
  }

  @Test
  public void pasteWithoutAcceptedCharactersLeavesTheSelectionAlone() {
    otp.type("123456");
    otp.type(Keys.chord(Keys.CONTROL, "a"));
    otp.paste("abc");
    assertEquals("123456", otp.getOtpValue());
  }

  @Test
  public void pasteDiscardsWhatDoesNotFit() {
    otp.focusField();
    otp.paste("1234567890");
    assertEquals("123456", otp.getOtpValue());
  }

  @Test
  public void pasteStartsAtTheCaret() {
    otp.type("12");
    otp.paste("99");
    assertEquals("1299", otp.getOtpValue());
  }

  @Test
  public void droppedTextIsHandledLikeAPaste() {
    otp.dropText("246");
    assertEquals("246", otp.getOtpValue());
  }

  // -- Complete event (FR-24) ------------------------------------------------------------------

  @Test
  public void completingTheCodeFiresTheCompleteEvent() {
    otp.recordCompleteEvents();
    otp.type("12345");
    assertEquals(List.of(), otp.getRecordedCompleteEvents());

    otp.type("6");
    assertEquals(List.of("123456"), otp.getRecordedCompleteEvents());
  }

  @Test
  public void replacingACharacterOfACompleteCodeFiresItAgain() {
    otp.type("123456");
    otp.recordCompleteEvents();

    otp.type(Keys.HOME);
    otp.type("9");
    assertEquals(List.of("923456"), otp.getRecordedCompleteEvents());
  }

  @Test
  public void pastingOverACompleteCodeFiresItAgain() {
    otp.type("123456");
    otp.recordCompleteEvents();

    otp.type(Keys.chord(Keys.CONTROL, "a"));
    otp.paste("654321");
    assertEquals(List.of("654321"), otp.getRecordedCompleteEvents());
  }

  @Test
  public void anEditThatLeavesTheCodeUnchangedDoesNotFireIt() {
    otp.type("123456");
    otp.recordCompleteEvents();

    // Overwriting a character with the one already there, and typing past the last slot, both
    // leave the code as it was, so there is nothing to announce.
    otp.type(Keys.HOME);
    otp.type("1");
    otp.type(Keys.END);
    otp.type("7");

    assertEquals("123456", otp.getOtpValue());
    assertEquals(List.of(), otp.getRecordedCompleteEvents());
  }

  @Test
  public void aProgrammaticValueDoesNotFireIt() {
    otp.recordCompleteEvents();
    otp.setElementProperty("value", "123456");
    assertEquals(List.of(), otp.getRecordedCompleteEvents());
  }

  // -- Presentation (FR-4, FR-5, FR-6, FR-29) ---------------------------------------------------

  @Test
  public void maskedSlotsRenderTheMaskGlyph() {
    otp.type("123456");
    otp.setElementProperty("masked", true);

    assertEquals(List.of(MASK, MASK, MASK, MASK, MASK, MASK), otp.getSlotTexts());
    assertTrue(otp.isSlotMasked(0));
    // Masking is visual only.
    assertEquals("123456", otp.getOtpValue());

    otp.setElementProperty("maskGlyph", "*");
    assertEquals("*", otp.getSlotTexts().get(0));
  }

  @Test
  public void caseConversionAppliesToTypedText() {
    otp.setElementProperty("allowedCharPattern", "[A-Za-z]");
    otp.setElementProperty("caseConversion", "upper");
    otp.type("abc");
    assertEquals("ABC", otp.getOtpValue());
    assertEquals(List.of("A", "B", "C", "", "", ""), otp.getSlotTexts());
  }

  @Test
  public void aOneCharacterPlaceholderIsRepeatedInEveryEmptySlot() {
    otp.setElementProperty("placeholder", "_");
    assertEquals(List.of("_", "_", "_", "_", "_", "_"), otp.getSlotTexts());

    otp.type("12");
    assertEquals(List.of("1", "2", "_", "_", "_", "_"), otp.getSlotTexts());
  }

  @Test
  public void aFullLengthPlaceholderGivesOneCharacterPerSlot() {
    otp.setElementProperty("placeholder", "YYMMDD");
    assertEquals(List.of("Y", "Y", "M", "M", "D", "D"), otp.getSlotTexts());
  }

  @Test
  public void everySlotCarriesTheErrorStyling() {
    otp.type("12");
    otp.setElementProperty("invalid", true);

    for (int i = 0; i < otp.getSlotCount(); i++) {
      assertTrue("slot " + i + " should be invalid", otp.isSlotInvalid(i));
    }
  }
}

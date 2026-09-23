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
package com.flowingcode.vaadin.addons.otpfield.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.flowingcode.vaadin.addons.otpfield.OtpCaseConversion;
import com.flowingcode.vaadin.addons.otpfield.OtpCompleteEvent;
import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.flowingcode.vaadin.addons.otpfield.OtpFieldI18n;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Server-side behaviour of {@link OtpField}. Everything that needs a browser (typing, caret
 * movement, paste, masking, styling) is covered by the integration tests instead.
 */
public class OtpFieldTest {

  // -- Structure and configuration (FR-1, FR-2, FR-3, FR-4, FR-6) ------------------------------

  @Test
  public void defaultLengthIsSix() {
    assertEquals(6, new OtpField().getLength());
    assertEquals(6, new OtpField().getElement().getProperty("length", 0.0), 0);
  }

  @Test
  public void constructorsApplyLabelAndLength() {
    assertEquals(4, new OtpField(4).getLength());
    assertEquals("Code", new OtpField("Code").getLabel());

    OtpField field = new OtpField("Code", 8);
    assertEquals("Code", field.getLabel());
    assertEquals(8, field.getLength());
  }

  @Test
  public void constructorWithListenerReceivesValueChanges() {
    List<String> values = new ArrayList<>();
    OtpField field = new OtpField("Code", event -> values.add(event.getValue()));
    field.setValue("123456");
    assertEquals(List.of("123456"), values);
  }

  @Test
  public void setLengthTruncatesTheValueAndFiresAValueChange() {
    List<String> values = new ArrayList<>();
    OtpField field = new OtpField();
    field.setValue("123456");
    field.addValueChangeListener(event -> values.add(event.getValue()));

    field.setLength(4);

    assertEquals("1234", field.getValue());
    assertEquals(List.of("1234"), values);
  }

  @Test
  public void setLengthKeepsAShorterValue() {
    OtpField field = new OtpField();
    field.setValue("12");
    field.setLength(4);
    assertEquals("12", field.getValue());
  }

  @Test
  public void setLengthRejectsValuesOutOfRange() {
    OtpField field = new OtpField();
    assertThrows(IllegalArgumentException.class, () -> field.setLength(0));
    assertThrows(IllegalArgumentException.class, () -> field.setLength(-1));
    assertThrows(IllegalArgumentException.class, () -> field.setLength(OtpField.MAX_LENGTH + 1));

    field.setLength(OtpField.MIN_LENGTH);
    assertEquals(OtpField.MIN_LENGTH, field.getLength());
    field.setLength(OtpField.MAX_LENGTH);
    assertEquals(OtpField.MAX_LENGTH, field.getLength());
  }

  @Test
  public void numericIsTheDefaultPatternAndTheOnlyOneImplyingAnInputMode() {
    OtpField field = new OtpField();
    assertEquals(OtpField.NUMERIC, field.getAllowedCharPattern());
    assertEquals("numeric", field.getInputMode());

    field.setAllowedCharPattern(OtpField.ALPHANUMERIC);
    assertEquals("[0-9A-Za-z]", field.getAllowedCharPattern());
    assertEquals("", field.getInputMode());

    field.setAllowedCharPattern(OtpField.NUMERIC);
    assertEquals("numeric", field.getInputMode());
  }

  @Test
  public void explicitInputModeOverridesTheDefaultOfThePattern() {
    OtpField field = new OtpField();
    field.setAllowedCharPattern("[0-9A-Fa-f]");
    assertEquals("", field.getInputMode());

    // The pattern assigns the default, so an explicit input mode is stated after it.
    field.setInputMode("text");
    assertEquals("text", field.getInputMode());
  }

  @Test
  public void allowedCharPatternLeavesCaseConversionAlone() {
    OtpField field = new OtpField();
    field.setCaseConversion(OtpCaseConversion.UPPERCASE);
    field.setAllowedCharPattern("[0-9A-Fa-f]");

    assertEquals(OtpCaseConversion.UPPERCASE, field.getCaseConversion());
    field.setValue("abc");
    assertEquals("ABC", field.getValue());
  }

  @Test
  public void maskingIsAVisualPropertyOnly() {
    OtpField field = new OtpField();
    field.setValue("123456");
    assertFalse(field.isMasked());

    field.setMasked(true);
    assertTrue(field.isMasked());
    assertEquals("123456", field.getValue());

    field.setMaskGlyph("*");
    assertEquals("*", field.getMaskGlyph());
    assertThrows(IllegalArgumentException.class, () -> field.setMaskGlyph("**"));
    assertThrows(IllegalArgumentException.class, () -> field.setMaskGlyph(""));
  }

  @Test
  public void placeholderMustBeOneOrLengthCharacters() {
    OtpField field = new OtpField();

    field.setPlaceholder("_");
    assertEquals("_", field.getPlaceholder());

    field.setPlaceholder("YYMMDD");
    assertEquals("YYMMDD", field.getPlaceholder());

    field.setPlaceholder(null);
    assertThrows(IllegalArgumentException.class, () -> field.setPlaceholder("__"));
    assertThrows(IllegalArgumentException.class, () -> field.setPlaceholder("1234567"));
  }

  // -- Value and events (FR-5, FR-21, FR-22, FR-23, FR-24, FR-25) ------------------------------

  @Test
  public void emptyValueIsAnEmptyString() {
    OtpField field = new OtpField();
    assertNotNull(field.getValue());
    assertEquals("", field.getValue());
    assertEquals("", field.getEmptyValue());
    assertTrue(field.isEmpty());
  }

  @Test
  public void partialValuesAreLegal() {
    OtpField field = new OtpField();
    field.setValue("12");
    assertEquals("12", field.getValue());
    assertFalse(field.isEmpty());
  }

  @Test
  public void aTooLongValueIsAcceptedAndReportedByTheValidator() {
    OtpField field = new OtpField(4);
    field.setValue("12345");

    assertEquals("12345", field.getValue());
    assertTrue(field.getDefaultValidator().apply("12345", null).isError());
  }

  @Test
  public void disallowedCharactersAreAcceptedAndReportedByTheValidator() {
    OtpField field = new OtpField();
    field.setValue("12a");

    assertEquals("12a", field.getValue());
    assertTrue(field.getDefaultValidator().apply("12a", null).isError());
    assertTrue(field.getDefaultValidator().apply("12 3", null).isError());
  }

  @Test
  public void anUnacceptableValueDoesNotBreakBinder() {
    // Throwing from setValue would propagate out of readBean whenever a stored code no longer
    // fits the configuration of the field.
    OtpField field = new OtpField();
    Binder<Bean> binder = new Binder<>();
    binder.forField(field).bind(Bean::getCode, Bean::setCode);

    Bean bean = new Bean();
    bean.setCode("abcdef");
    binder.readBean(bean);

    assertEquals("abcdef", field.getValue());
    assertFalse(binder.validate().isOk());
  }

  @Test
  public void setValueAppliesCaseConversionBeforeValidating() {
    OtpField field = new OtpField();
    field.setAllowedCharPattern("[A-Za-z]");
    field.setCaseConversion(OtpCaseConversion.UPPERCASE);

    field.setValue("abcdef");
    assertEquals("ABCDEF", field.getValue());

    field.setCaseConversion(OtpCaseConversion.LOWERCASE);
    assertEquals("abcdef", field.getValue());
  }

  @Test
  public void customAllowedCharPatternRestrictsTheValue() {
    OtpField field = new OtpField();
    field.setAllowedCharPattern("[0-9A-F]");
    field.setValue("1A2B3C");
    assertEquals("1A2B3C", field.getValue());
    assertTrue(field.getDefaultValidator().apply("1a2b3c", null).isError());
  }

  @Test
  public void valueChangeModeDefaultsToEager() {
    assertEquals(ValueChangeMode.EAGER, new OtpField().getValueChangeMode());
  }

  @Test
  public void valueChangeModeCanBeChanged() {
    OtpField field = new OtpField();
    field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
    assertEquals(ValueChangeMode.ON_CHANGE, field.getValueChangeMode());

    field.setValueChangeMode(ValueChangeMode.TIMEOUT);
    field.setValueChangeTimeout(500);
    assertEquals(ValueChangeMode.TIMEOUT, field.getValueChangeMode());
    assertEquals(500, field.getValueChangeTimeout());
  }

  @Test
  public void completeListenerReceivesTheCode() {
    List<String> completed = new ArrayList<>();
    OtpField field = new OtpField();
    field.addCompleteListener(event -> completed.add(event.getValue()));

    ComponentUtil.fireEvent(field, new OtpCompleteEvent(field, true, "123456"));
    assertEquals(List.of("123456"), completed);
  }

  @Test
  public void completeListenerCanBeRemoved() {
    List<String> completed = new ArrayList<>();
    OtpField field = new OtpField();
    field.addCompleteListener(event -> completed.add(event.getValue())).remove();

    ComponentUtil.fireEvent(field, new OtpCompleteEvent(field, true, "123456"));
    assertTrue(completed.isEmpty());
  }

  @Test
  public void programmaticValueChangeDoesNotFireACompleteEvent() {
    List<String> completed = new ArrayList<>();
    OtpField field = new OtpField();
    field.addCompleteListener(event -> completed.add(event.getValue()));

    field.setValue("123456");
    assertTrue(completed.isEmpty());
  }

  @Test
  public void setBeanWithAnUnacceptableValueDoesNotThrow() {
    OtpField field = new OtpField();
    Binder<Bean> binder = new Binder<>();
    binder.forField(field).bind(Bean::getCode, Bean::setCode);

    Bean bean = new Bean();
    bean.setCode("1234567");
    binder.setBean(bean);

    assertEquals("1234567", field.getValue());
    assertFalse(binder.validate().isOk());
  }

  @Test
  public void theInvalidConstraintUsesItsOwnI18nMessage() {
    TestableOtpField field = new TestableOtpField();
    field.setI18n(new OtpFieldI18n().setInvalidErrorMessage("That code cannot be entered here"));

    field.setValue("12a");
    field.validate();

    assertTrue(field.isInvalid());
    assertEquals("That code cannot be entered here", field.getErrorMessage());
  }

  @Test
  public void anUnacceptableValueIsReportedAheadOfTheIncompleteConstraint() {
    OtpField field = new OtpField();
    field.setI18n(new OtpFieldI18n().setIncompleteErrorMessage("incomplete")
        .setInvalidErrorMessage("invalid"));

    // "12a" is both incomplete and unacceptable; the more specific message wins.
    assertEquals("invalid",
        field.getDefaultValidator().apply("12a", null).getErrorMessage());
  }

  @Test
  public void requiredIndicatorDrivesTheRequiredConstraintMessage() {
    TestableOtpField field = new TestableOtpField();
    field.setI18n(new OtpFieldI18n().setRequiredErrorMessage("A code is required"));

    field.setRequiredIndicatorVisible(true);
    field.validate();

    assertTrue(field.isRequiredIndicatorVisible());
    assertTrue(field.isInvalid());
    assertEquals("A code is required", field.getErrorMessage());
  }

  @Test
  public void caseConversionAppliesToAProgrammaticValue() {
    OtpField field = new OtpField();
    field.setAllowedCharPattern("[A-Za-z]");
    field.setCaseConversion(OtpCaseConversion.UPPERCASE);

    // The conversion is a guarantee about the value, not a hint like autocapitalize.
    field.setValue("abcdef");
    assertEquals("ABCDEF", field.getValue());
    assertEquals("characters", field.getElement().getProperty("autocapitalize"));
  }

  @Test
  public void autocapitalizeIsNeverAskedForOnANumericField() {
    OtpField field = new OtpField();
    field.setCaseConversion(OtpCaseConversion.UPPERCASE);

    // A numeric keyboard has no case to capitalise.
    assertEquals("none", field.getElement().getProperty("autocapitalize"));
  }

  @Test
  public void clearEmptiesTheField() {
    OtpField field = new OtpField();
    field.setValue("123456");
    field.clear();
    assertEquals("", field.getValue());
    assertTrue(field.isEmpty());
  }

  // -- Validation (FR-26, FR-27, FR-28) --------------------------------------------------------

  @Test
  public void incompleteValueIsInvalidOnItsOwn() {
    OtpField field = new OtpField();

    assertFalse(field.getDefaultValidator().apply("123456", null).isError());
    assertFalse(field.getDefaultValidator().apply("", null).isError());
    assertTrue(field.getDefaultValidator().apply("123", null).isError());
  }

  @Test
  public void requiredIsOnlyReportedToTheComponentItself() {
    OtpField field = new OtpField();
    field.setRequiredIndicatorVisible(true);

    // Called by the component (no value context).
    assertTrue(field.getDefaultValidator().apply("", null).isError());
  }

  @Test
  public void validateUpdatesTheInvalidState() {
    TestableOtpField field = new TestableOtpField();
    assertFalse(field.isInvalid());

    field.setValue("123");
    field.validate();
    assertTrue(field.isInvalid());

    field.setValue("123456");
    assertFalse(field.isInvalid());
  }

  @Test
  public void manualValidationHandsControlToTheApplication() {
    TestableOtpField field = new TestableOtpField();
    field.setManualValidation(true);

    field.setValue("123");
    field.validate();
    assertFalse(field.isInvalid());

    field.setInvalid(true);
    field.setValue("123456");
    assertTrue(field.isInvalid());
  }

  @Test
  public void errorMessagesComeFromI18n() {
    TestableOtpField field = new TestableOtpField();
    field.setI18n(new OtpFieldI18n().setIncompleteErrorMessage("Six digits, please")
        .setRequiredErrorMessage("A code is required"));

    field.setValue("123");
    field.validate();
    assertEquals("Six digits, please", field.getErrorMessage());

    field.clear();
    field.setRequiredIndicatorVisible(true);
    field.validate();
    assertEquals("A code is required", field.getErrorMessage());
  }

  @Test
  public void customErrorMessageTakesPriorityOverI18n() {
    TestableOtpField field = new TestableOtpField();
    field.setErrorMessage("Incorrect code");

    field.setValue("123");
    field.validate();
    assertTrue(field.isInvalid());
    assertEquals("Incorrect code", field.getErrorMessage());
  }

  @Test
  public void binderReportsTheIncompleteConstraint() {
    OtpField field = new OtpField();
    Binder<Bean> binder = new Binder<>();
    binder.forField(field).bind(Bean::getCode, Bean::setCode);
    binder.setBean(new Bean());

    field.setValue("123456");
    assertTrue(binder.validate().isOk());

    field.setValue("123");
    assertFalse(binder.validate().isOk());
  }

  @Test
  public void binderRoundTripsTheValue() {
    OtpField field = new OtpField();
    Binder<Bean> binder = new Binder<>();
    binder.forField(field).bind(Bean::getCode, Bean::setCode);

    Bean bean = new Bean();
    bean.setCode("654321");
    binder.setBean(bean);
    assertEquals("654321", field.getValue());

    field.setValue("123456");
    assertEquals("123456", bean.getCode());
  }

  @Test
  public void binderReportsRequiredThroughAsRequired() {
    OtpField field = new OtpField();
    Binder<Bean> binder = new Binder<>();
    binder.forField(field).asRequired("Please enter the code").bind(Bean::getCode, Bean::setCode);
    binder.setBean(new Bean());

    assertFalse(binder.validate().isOk());
    field.setValue("123456");
    assertTrue(binder.validate().isOk());
  }

  // -- States (FR-30, FR-31, FR-32) ------------------------------------------------------------

  @Test
  public void readOnlyAndDisabledAreForwardedToTheElement() {
    OtpField field = new OtpField();

    field.setReadOnly(true);
    assertTrue(field.isReadOnly());
    assertTrue(field.getElement().getProperty("readonly", false));

    field.setEnabled(false);
    assertFalse(field.isEnabled());
  }

  @Test
  public void autofocusIsForwardedToTheElement() {
    OtpField field = new OtpField();
    assertFalse(field.isAutofocus());
    field.setAutofocus(true);
    assertTrue(field.isAutofocus());
  }

  @Test
  public void ariaLabelUsesTheAccessibleNameProperties() {
    OtpField field = new OtpField();
    field.setAriaLabel("Verification code");
    assertEquals("Verification code", field.getAriaLabel().orElse(null));

    field.setAriaLabelledBy("hint-id");
    assertEquals("hint-id", field.getAriaLabelledBy().orElse(null));
  }

  /** Widens the protected {@code validate()} so that the tests can drive it directly. */
  private static class TestableOtpField extends OtpField {
    @Override
    public void validate() {
      super.validate();
    }
  }

  /** The bean bound by the {@link Binder} tests. */
  public static class Bean {
    private String code = "";

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }
  }
}

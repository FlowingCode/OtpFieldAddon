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
package com.flowingcode.vaadin.addons.otpfield;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;
import com.vaadin.flow.component.shared.HasPrefix;
import com.vaadin.flow.component.shared.HasSuffix;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.shared.InputField;
import com.vaadin.flow.component.shared.ValidationUtil;
import com.vaadin.flow.component.shared.internal.ValidationController;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A field for entering a short verification code: a one-time password, a 2FA or SMS code, an e-mail
 * confirmation code or a banking token.
 * <p>
 * The field renders one visual slot per character, advances as the user types, accepts a pasted or
 * auto-filled code, and integrates with {@link Binder} like any other Vaadin input field.
 *
 * <h2>Value</h2>
 * <p>
 * The value is a {@link String} that may be shorter than {@link #getLength()}: what the user typed
 * is always the value, and "not finished yet" is a constraint violation rather than a hidden value.
 * Applications that only care about the finished code use
 * {@link #addCompleteListener(ComponentEventListener)} instead of inspecting the length of every
 * value change.
 * <p>
 * The value change mode defaults to {@link ValueChangeMode#EAGER}, so the server sees every
 * keystroke. Applications that would rather not transmit partial codes can switch to
 * {@link ValueChangeMode#ON_CHANGE}, which leaves the complete event intact.
 *
 * <h2>Validation</h2>
 * <p>
 * The field has three built-in constraints:
 * <ul>
 * <li><b>required</b> &mdash; the value is empty while {@link #setRequiredIndicatorVisible(boolean)}
 * is enabled
 * <li><b>incomplete</b> &mdash; the value is non-empty and shorter than {@link #getLength()}
 * <li><b>invalid</b> &mdash; the value is longer than {@link #getLength()}, or contains characters
 * the field does not accept. A user cannot enter such a value, so this reports a value that was set
 * programmatically, typically a stored code that no longer fits the configuration of the field
 * </ul>
 * <p>
 * {@link #getDefaultValidator()} reports all three, so a {@link Binder} picks them up
 * automatically, and
 * the error messages come from {@link OtpFieldI18n}. Verifying a code against a back end is the
 * responsibility of the application: use
 * {@link #addCompleteListener(ComponentEventListener) addCompleteListener} together with
 * {@link #setInvalid(boolean)} and {@link #setErrorMessage(String)} for that, or hand validation
 * over entirely with {@link #setManualValidation(boolean)}.
 * <p>
 * The field generates, sends, verifies or expires nothing: it is an input control, not a security
 * control. Masking is a shoulder-surfing measure only.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * OtpField otp = new OtpField("Verification code", 6);
 * otp.addCompleteListener(e -&gt; verifyCode(e.getValue()));
 * </pre>
 *
 * @author Flowing Code
 * @since 1.0.0
 */
@Tag("fc-otp-field")
@JsModule("./fc-otp-field/otp-field.ts")
@NpmPackage(value = "@vaadin/field-base", version = "25.2.7")
@NpmPackage(value = "@vaadin/component-base", version = "25.2.7")
@SuppressWarnings("serial")
public class OtpField extends AbstractSinglePropertyField<OtpField, String>
    implements InputField<AbstractField.ComponentValueChangeEvent<OtpField, String>, String>,
    HasAllowedCharPattern, HasAriaLabel, HasPlaceholder, HasPrefix, HasSuffix,
    HasValidationProperties, HasValidator<String>, HasValueChangeMode, Focusable<OtpField> {

  /** The smallest supported number of slots. */
  public static final int MIN_LENGTH = 1;

  /** The largest supported number of slots. */
  public static final int MAX_LENGTH = 24;

  /** The default number of slots, the dominant convention for TOTP and SMS codes. */
  public static final int DEFAULT_LENGTH = 6;

  /** The default mask glyph, a bullet. */
  public static final String DEFAULT_MASK_GLYPH = "\u2022";

  /**
   * The allowed character pattern that accepts digits only: the default, and the convention for
   * TOTP and SMS codes. It is the only pattern that implies an {@code inputmode} of
   * {@code numeric}.
   *
   * @see #setAllowedCharPattern(String)
   */
  public static final String NUMERIC = "[0-9]";

  /**
   * The allowed character pattern that accepts digits and Latin letters in either case.
   *
   * @see #setAllowedCharPattern(String)
   */
  public static final String ALPHANUMERIC = "[0-9A-Za-z]";

  private int length = DEFAULT_LENGTH;
  private OtpCaseConversion caseConversion = OtpCaseConversion.NONE;
  private OtpFieldI18n i18n = new OtpFieldI18n();

  private ValueChangeMode valueChangeMode;
  private int valueChangeTimeout = DEFAULT_CHANGE_TIMEOUT;

  /** The pattern last compiled by {@link #isAllowed(String)}, and the source it was compiled from. */
  private Pattern valuePattern;
  private String valuePatternSource;

  private final Validator<String> defaultValidator = (value, context) -> {
    // Binder has its own required handling, so the required constraint is only reported when the
    // validator is called by the component itself.
    if (context == null) {
      ValidationResult requiredResult = ValidationUtil.validateRequiredConstraint(
          getI18nErrorMessage(OtpFieldI18n::getRequiredErrorMessage), isRequiredIndicatorVisible(),
          value, getEmptyValue());
      if (requiredResult.isError()) {
        return requiredResult;
      }
    }

    if (value != null && (value.length() > length || !isAllowed(value))) {
      return ValidationResult.error(getI18nErrorMessage(OtpFieldI18n::getInvalidErrorMessage));
    }

    if (isIncomplete(value)) {
      return ValidationResult
          .error(getI18nErrorMessage(OtpFieldI18n::getIncompleteErrorMessage));
    }

    return ValidationResult.ok();
  };

  private final ValidationController<OtpField, String> validationController =
      new ValidationController<>(this);

  /**
   * Constructs an empty field with the default length of {@value #DEFAULT_LENGTH} and the
   * {@link #NUMERIC} allowed character pattern.
   */
  public OtpField() {
    super("value", "", false);

    // The server owns the invalid state; see the validation section of the class javadoc.
    getElement().setProperty("manualValidation", true);
    setInvalid(false);

    setValueChangeMode(ValueChangeMode.EAGER);
    setAllowedCharPattern(NUMERIC);
    setCaseConversion(OtpCaseConversion.NONE);
    getElement().setProperty("length", length);

    addValueChangeListener(event -> {
      // An incomplete code is not an error while the user is still entering it: revalidate eagerly
      // only to clear an error the user is fixing, and otherwise wait until the code is committed
      // (on blur, or when it becomes complete).
      if (isInvalid() || getValue().isEmpty() || getValue().length() == getLength()) {
        validate();
      }
    });
    addBlurListener(event -> validate());
  }

  /**
   * Constructs an empty field with the given length.
   *
   * @param length the number of slots, between {@value #MIN_LENGTH} and {@value #MAX_LENGTH}
   * @throws IllegalArgumentException if the length is out of range
   */
  public OtpField(int length) {
    this();
    setLength(length);
  }

  /**
   * Constructs an empty field with the given label.
   *
   * @param label the text to set as the label
   */
  public OtpField(String label) {
    this();
    setLabel(label);
  }

  /**
   * Constructs an empty field with the given label and length.
   *
   * @param label the text to set as the label
   * @param length the number of slots, between {@value #MIN_LENGTH} and {@value #MAX_LENGTH}
   * @throws IllegalArgumentException if the length is out of range
   */
  public OtpField(String label, int length) {
    this(label);
    setLength(length);
  }

  /**
   * Constructs an empty field with the given label and a value change listener.
   *
   * @param label the text to set as the label
   * @param listener the value change listener
   * @see #addValueChangeListener(com.vaadin.flow.component.HasValue.ValueChangeListener)
   */
  public OtpField(String label,
      ValueChangeListener<? super ComponentValueChangeEvent<OtpField, String>> listener) {
    this(label);
    addValueChangeListener(listener);
  }

  /**
   * Sets the number of slots, which is also the length at which the code is complete.
   * <p>
   * If the current value is longer than the new length it is truncated, which fires a value change
   * event. A multi-character placeholder that no longer matches the new length is kept but not
   * rendered, so set the placeholder after the length.
   *
   * @param length the number of slots, between {@value #MIN_LENGTH} and {@value #MAX_LENGTH}
   * @throws IllegalArgumentException if the length is out of range
   */
  public void setLength(int length) {
    if (length < MIN_LENGTH || length > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "length must be between " + MIN_LENGTH + " and " + MAX_LENGTH + ", got " + length);
    }

    this.length = length;
    getElement().setProperty("length", length);

    String value = getValue();
    if (value.length() > length) {
      setValue(value.substring(0, length));
    }
  }

  /**
   * Gets the number of slots.
   *
   * @return the number of slots
   * @see #setLength(int)
   */
  public int getLength() {
    return length;
  }

  /**
   * Sets the characters the field accepts, as a single-character regular expression.
   * <p>
   * {@link #NUMERIC} and {@link #ALPHANUMERIC} cover the common cases, and any other
   * single-character expression works as well, such as {@code [0-9A-Fa-f]} for hexadecimal.
   * <p>
   * The pattern also assigns the default {@code inputmode}: {@link #NUMERIC} implies
   * {@code numeric}, so that mobile browsers show a digit keypad, and every other pattern leaves
   * the input mode empty. Call {@link #setInputMode(String)} <em>after</em> this method to state a
   * different one.
   *
   * @param pattern the allowed character pattern, or {@code null} to accept every character
   */
  @Override
  public void setAllowedCharPattern(String pattern) {
    HasAllowedCharPattern.super.setAllowedCharPattern(pattern);
    setInputMode(NUMERIC.equals(pattern) ? "numeric" : "");
    updateAutocapitalize();
  }

  /**
   * Sets the {@code inputmode} of the field, the hint that decides which on-screen keyboard mobile
   * browsers show.
   * <p>
   * {@link #setAllowedCharPattern(String)} assigns a default input mode, so call this method
   * <em>after</em> it.
   *
   * @param inputMode an {@code inputmode} value such as {@code numeric} or {@code text}, or the
   *        empty string to state none; not {@code null}
   */
  public void setInputMode(String inputMode) {
    Objects.requireNonNull(inputMode, "inputMode cannot be null; use an empty string for none");
    getElement().setProperty("inputMode", inputMode);
  }

  /**
   * Gets the {@code inputmode} of the field.
   *
   * @return the input mode, or the empty string if the field states none
   * @see #setInputMode(String)
   */
  public String getInputMode() {
    return getElement().getProperty("inputMode", "");
  }

  /**
   * Sets the case conversion applied to typed, pasted and programmatically set values.
   *
   * @param caseConversion the case conversion, not {@code null}
   */
  public void setCaseConversion(OtpCaseConversion caseConversion) {
    this.caseConversion = Objects.requireNonNull(caseConversion, "caseConversion cannot be null");
    getElement().setProperty("caseConversion", caseConversion.getClientName());
    updateAutocapitalize();

    String value = getValue();
    String converted = caseConversion.apply(value);
    if (!converted.equals(value)) {
      setValue(converted);
    }
  }

  /**
   * Gets the case conversion applied to the value.
   *
   * @return the case conversion, never {@code null}
   * @see #setCaseConversion(OtpCaseConversion)
   */
  public OtpCaseConversion getCaseConversion() {
    return caseConversion;
  }

  /**
   * Hints an upper-case on-screen keyboard when the field converts to upper case and accepts
   * letters in the first place; a numeric field has nothing to capitalize.
   */
  private void updateAutocapitalize() {
    getElement().setProperty("autocapitalize",
        !NUMERIC.equals(getAllowedCharPattern()) && caseConversion == OtpCaseConversion.UPPERCASE
            ? "characters"
            : "none");
  }

  /**
   * Sets whether the entered characters are hidden behind a mask glyph.
   * <p>
   * Masking is visual only: it does not change the value, and it does not hide the code from
   * assistive technology. It is a shoulder-surfing measure, not a security control.
   *
   * @param masked {@code true} to render the mask glyph instead of the characters
   */
  public void setMasked(boolean masked) {
    getElement().setProperty("masked", masked);
  }

  /**
   * Gets whether the entered characters are hidden behind a mask glyph.
   *
   * @return {@code true} if the field is masked
   * @see #setMasked(boolean)
   */
  public boolean isMasked() {
    return getElement().getProperty("masked", false);
  }

  /**
   * Sets the glyph rendered in filled slots while the field is masked.
   *
   * @param maskGlyph a single character, not {@code null}
   * @throws IllegalArgumentException if the glyph is not exactly one character
   */
  public void setMaskGlyph(String maskGlyph) {
    Objects.requireNonNull(maskGlyph, "maskGlyph cannot be null");
    if (maskGlyph.codePointCount(0, maskGlyph.length()) != 1) {
      throw new IllegalArgumentException("maskGlyph must be a single character, got " + maskGlyph);
    }
    getElement().setProperty("maskGlyph", maskGlyph);
  }

  /**
   * Gets the glyph rendered in filled slots while the field is masked.
   *
   * @return the mask glyph
   * @see #setMaskGlyph(String)
   */
  public String getMaskGlyph() {
    return getElement().getProperty("maskGlyph", DEFAULT_MASK_GLYPH);
  }

  /**
   * Sets the placeholder shown in empty slots.
   * <p>
   * A one-character placeholder is repeated in every empty slot; a placeholder that is exactly
   * {@link #getLength()} characters long provides one character per slot.
   *
   * @param placeholder the placeholder, {@code null} or empty for none
   * @throws IllegalArgumentException if the placeholder is neither one nor {@link #getLength()}
   *         characters long
   */
  @Override
  public void setPlaceholder(String placeholder) {
    if (placeholder != null && !placeholder.isEmpty() && placeholder.length() != 1
        && placeholder.length() != length) {
      throw new IllegalArgumentException("placeholder must be 1 or " + length
          + " characters long, got " + placeholder.length());
    }
    HasPlaceholder.super.setPlaceholder(placeholder);
  }

  /**
   * Sets whether the user is required to provide a value. When required, an indicator appears next
   * to the label and the field invalidates if the value is cleared.
   * <p>
   * NOTE: the required indicator is only visible when the field has a label, see
   * {@link #setLabel(String)}.
   *
   * @param required {@code true} to make the field required, {@code false} otherwise
   * @see OtpFieldI18n#setRequiredErrorMessage(String)
   */
  @Override
  public void setRequiredIndicatorVisible(boolean required) {
    super.setRequiredIndicatorVisible(required);
  }

  /**
   * Adds a listener that is notified when the entered code becomes complete.
   *
   * @param listener the listener to add, not {@code null}
   * @return a registration for removing the listener
   * @see OtpCompleteEvent
   */
  public Registration addCompleteListener(ComponentEventListener<OtpCompleteEvent> listener) {
    return addListener(OtpCompleteEvent.class, listener);
  }

  /**
   * {@inheritDoc}
   * <p>
   * The default value is {@link ValueChangeMode#EAGER}, so that every keystroke reaches the server.
   * {@link ValueChangeMode#ON_CHANGE} avoids transmitting partial codes; the complete event is
   * fired in either case.
   */
  @Override
  public ValueChangeMode getValueChangeMode() {
    return valueChangeMode;
  }

  @Override
  public void setValueChangeMode(ValueChangeMode valueChangeMode) {
    this.valueChangeMode = valueChangeMode;
    setSynchronizedEvent(ValueChangeMode.eventForMode(valueChangeMode, "input"));
    applyChangeTimeout();
  }

  @Override
  public void setValueChangeTimeout(int valueChangeTimeout) {
    this.valueChangeTimeout = valueChangeTimeout;
    applyChangeTimeout();
  }

  @Override
  public int getValueChangeTimeout() {
    return valueChangeTimeout;
  }

  private void applyChangeTimeout() {
    ValueChangeMode.applyChangeTimeout(getValueChangeMode(), getValueChangeTimeout(),
        getSynchronizationRegistration());
  }

  /**
   * Sets the value of the field, after applying the configured case conversion.
   * <p>
   * A value that is longer than {@link #getLength()}, or that contains characters the field does
   * not accept, is not rejected: it is set and reported by the validator, exactly as an
   * out-of-range value is on the built-in Vaadin fields. Throwing here would instead propagate out
   * of {@link Binder#readBean(Object)} and {@link Binder#setBean(Object)} whenever a stored code no
   * longer fits the configuration of the field.
   *
   * @param value the new value, not {@code null}
   */
  @Override
  public void setValue(String value) {
    Objects.requireNonNull(value, "value cannot be null; use clear() to empty the field");
    super.setValue(caseConversion.apply(value));
  }

  /**
   * Returns the code entered so far, which may be shorter than {@link #getLength()}.
   *
   * @return the current value, never {@code null}
   */
  @Override
  public String getValue() {
    return super.getValue();
  }

  @Override
  public String getEmptyValue() {
    return "";
  }

  /**
   * Empties the field and returns the caret to the first slot.
   */
  @Override
  public void clear() {
    setValue(getEmptyValue());
  }

  /**
   * Sets whether the field should automatically receive focus when the page loads.
   *
   * @param autofocus {@code true} to focus the field on attach
   */
  public void setAutofocus(boolean autofocus) {
    getElement().setProperty("autofocus", autofocus);
  }

  /**
   * Gets whether the field automatically receives focus when the page loads.
   *
   * @return {@code true} if the field is focused on attach
   * @see #setAutofocus(boolean)
   */
  public boolean isAutofocus() {
    return getElement().getProperty("autofocus", false);
  }

  @Override
  public void setAriaLabel(String ariaLabel) {
    getElement().setProperty("accessibleName", ariaLabel);
  }

  @Override
  public Optional<String> getAriaLabel() {
    return Optional.ofNullable(getElement().getProperty("accessibleName"));
  }

  @Override
  public void setAriaLabelledBy(String labelledBy) {
    getElement().setProperty("accessibleNameRef", labelledBy);
  }

  @Override
  public Optional<String> getAriaLabelledBy() {
    return Optional.ofNullable(getElement().getProperty("accessibleNameRef"));
  }

  /**
   * {@inheritDoc}
   * <p>
   * The returned validator reports the required and the incomplete constraint, using the error
   * messages of the {@link OtpFieldI18n} object of this field.
   */
  @Override
  public Validator<String> getDefaultValidator() {
    return defaultValidator;
  }

  @Override
  public void setManualValidation(boolean enabled) {
    validationController.setManualValidation(enabled);
  }

  /**
   * Validates the current value against the built-in constraints and updates the invalid state and
   * the error message accordingly. A custom error message set with {@link #setErrorMessage(String)}
   * takes priority over the messages of the {@link OtpFieldI18n} object.
   * <p>
   * The method does nothing while manual validation is enabled.
   */
  protected void validate() {
    validationController.validate(getValue());
  }

  /**
   * Sets the internationalization object of this field.
   *
   * @param i18n the i18n object, not {@code null}
   */
  public void setI18n(OtpFieldI18n i18n) {
    this.i18n = Objects.requireNonNull(i18n, "i18n cannot be null");
  }

  /**
   * Gets the internationalization object of this field.
   * <p>
   * NOTE: updating the returned instance does not update the field unless it is set again with
   * {@link #setI18n(OtpFieldI18n)}.
   *
   * @return the i18n object, never {@code null}
   */
  public OtpFieldI18n getI18n() {
    return i18n;
  }

  private String getI18nErrorMessage(Function<OtpFieldI18n, String> getter) {
    return Optional.ofNullable(getter.apply(i18n)).orElse("");
  }

  /** Returns whether the given value is non-empty and shorter than the length of the field. */
  private boolean isIncomplete(String value) {
    return value != null && !value.isEmpty() && value.length() != length;
  }

  /**
   * Returns whether every character of the value is accepted by the allowed character pattern.
   * <p>
   * The pattern is a JavaScript regular expression, so it may use syntax that Java cannot compile.
   * In that case the check is skipped and the client-side filtering remains the only guard.
   */
  private boolean isAllowed(String value) {
    String pattern = getAllowedCharPattern();
    if (pattern == null || pattern.isEmpty() || value.isEmpty()) {
      return true;
    }

    if (!pattern.equals(valuePatternSource)) {
      valuePatternSource = pattern;
      try {
        valuePattern = Pattern.compile("^(?:" + pattern + ")*$");
      } catch (PatternSyntaxException e) {
        valuePattern = null;
      }
    }

    return valuePattern == null || valuePattern.matcher(value).matches();
  }
}

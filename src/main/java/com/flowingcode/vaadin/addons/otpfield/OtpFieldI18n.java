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

import java.io.Serializable;

/**
 * The internationalization properties of an {@link OtpField}: the error messages of its three
 * built-in constraints.
 * <p>
 * All three messages have an English default, so an unconfigured field never shows an empty error.
 * Note that an error message set with {@link OtpField#setErrorMessage(String)} takes priority over
 * these messages.
 *
 * @author Flowing Code
 * @since 1.0.0
 * @see OtpField#setI18n(OtpFieldI18n)
 */
@SuppressWarnings("serial")
public class OtpFieldI18n implements Serializable {

  private String requiredErrorMessage = "Enter the code";
  private String incompleteErrorMessage = "The code is incomplete";
  private String invalidErrorMessage = "The code is not valid";

  /**
   * Gets the error message displayed when the field is required but empty.
   *
   * @return the error message, or {@code null} if it was cleared
   * @see OtpField#setRequiredIndicatorVisible(boolean)
   */
  public String getRequiredErrorMessage() {
    return requiredErrorMessage;
  }

  /**
   * Sets the error message to display when the field is required but empty.
   *
   * @param errorMessage the error message, or {@code null} to clear it
   * @return this instance, for method chaining
   * @see OtpField#setRequiredIndicatorVisible(boolean)
   */
  public OtpFieldI18n setRequiredErrorMessage(String errorMessage) {
    this.requiredErrorMessage = errorMessage;
    return this;
  }

  /**
   * Gets the error message displayed when the code is non-empty but shorter than the length of the
   * field.
   *
   * @return the error message, or {@code null} if it was cleared
   * @see OtpField#setLength(int)
   */
  public String getIncompleteErrorMessage() {
    return incompleteErrorMessage;
  }

  /**
   * Sets the error message to display when the code is non-empty but shorter than the length of the
   * field.
   *
   * @param errorMessage the error message, or {@code null} to clear it
   * @return this instance, for method chaining
   * @see OtpField#setLength(int)
   */
  public OtpFieldI18n setIncompleteErrorMessage(String errorMessage) {
    this.incompleteErrorMessage = errorMessage;
    return this;
  }

  /**
   * Gets the error message displayed when the code is longer than the length of the field, or
   * contains characters the field does not accept.
   *
   * @return the error message, or {@code null} if it was cleared
   * @see OtpField#setValue(String)
   */
  public String getInvalidErrorMessage() {
    return invalidErrorMessage;
  }

  /**
   * Sets the error message to display when the code is longer than the length of the field, or
   * contains characters the field does not accept.
   * <p>
   * A user cannot enter such a value, so this message reports a value that was set
   * programmatically, typically a stored code that no longer fits the configuration of the field.
   *
   * @param errorMessage the error message, or {@code null} to clear it
   * @return this instance, for method chaining
   * @see OtpField#setValue(String)
   */
  public OtpFieldI18n setInvalidErrorMessage(String errorMessage) {
    this.invalidErrorMessage = errorMessage;
    return this;
  }
}

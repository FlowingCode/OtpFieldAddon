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

import java.util.Locale;

/**
 * The case conversion applied by an {@link OtpField} to typed, pasted and programmatically set
 * values, so that the server-side value and the displayed code always agree.
 *
 * @author Flowing Code
 * @since 1.0.0
 * @see OtpField#setCaseConversion(OtpCaseConversion)
 */
public enum OtpCaseConversion {

  /** The value is used as entered. */
  NONE("none"),

  /** The value is converted to upper case. */
  UPPERCASE("upper"),

  /** The value is converted to lower case. */
  LOWERCASE("lower");

  private final String clientName;

  private OtpCaseConversion(String clientName) {
    this.clientName = clientName;
  }

  /**
   * Returns the name of this conversion as understood by the client-side element.
   *
   * @return the client-side name of this conversion
   */
  String getClientName() {
    return clientName;
  }

  /**
   * Applies this conversion to the given value.
   * <p>
   * The conversion is locale independent, because a verification code is a technical token rather
   * than natural-language text.
   *
   * @param value the value to convert, not {@code null}
   * @return the converted value
   */
  String apply(String value) {
    switch (this) {
      case UPPERCASE:
        return value.toUpperCase(Locale.ROOT);
      case LOWERCASE:
        return value.toLowerCase(Locale.ROOT);
      default:
        return value;
    }
  }
}

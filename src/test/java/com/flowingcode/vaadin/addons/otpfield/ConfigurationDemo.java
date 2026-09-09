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

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Configuration")
@SuppressWarnings("serial")
@Route(value = "otpfield/configuration", layout = OtpFieldDemoView.class)
public class ConfigurationDemo extends VerticalLayout {

  private static final String HEXADECIMAL = "[0-9A-Fa-f]";

  public ConfigurationDemo() {
    OtpField otp = new OtpField("Code");

    IntegerField length = new IntegerField("Length");
    length.setMin(OtpField.MIN_LENGTH);
    length.setMax(OtpField.MAX_LENGTH);
    length.setStepButtonsVisible(true);
    length.setValue(otp.getLength());
    length.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        // Shortening the length truncates the value, which fires a value change event.
        otp.setLength(event.getValue());
      }
    });

    // The accepted characters are a single-character regular expression. NUMERIC and
    // ALPHANUMERIC are provided as constants; anything else is spelled out, as HEXADECIMAL is
    // here. NUMERIC also gives the field an inputmode of "numeric", for a digit keypad on mobile.
    Select<String> allowedChars = new Select<>();
    allowedChars.setLabel("Allowed characters");
    allowedChars.setItems(OtpField.NUMERIC, OtpField.ALPHANUMERIC, HEXADECIMAL);
    allowedChars.setItemLabelGenerator(ConfigurationDemo::describe);
    allowedChars.setValue(otp.getAllowedCharPattern());
    allowedChars.addValueChangeListener(event -> otp.setAllowedCharPattern(event.getValue()));

    // Case conversion is independent of the pattern, so the two selects do not interact.
    Select<OtpCaseConversion> caseConversion = new Select<>();
    caseConversion.setLabel("Case conversion");
    caseConversion.setItems(OtpCaseConversion.values());
    caseConversion.setValue(otp.getCaseConversion());
    caseConversion.addValueChangeListener(event -> otp.setCaseConversion(event.getValue()));

    Checkbox masked = new Checkbox("Masked");
    masked.addValueChangeListener(event -> otp.setMasked(event.getValue()));

    Checkbox placeholder = new Checkbox("Placeholder");
    placeholder.addValueChangeListener(event -> otp.setPlaceholder(event.getValue() ? "_" : null));

    Checkbox readOnly = new Checkbox("Read-only");
    readOnly.addValueChangeListener(event -> otp.setReadOnly(event.getValue()));

    Checkbox enabled = new Checkbox("Enabled", true);
    enabled.addValueChangeListener(event -> otp.setEnabled(event.getValue()));

    add(otp, new HorizontalLayout(length, allowedChars, caseConversion),
        new HorizontalLayout(masked, placeholder, readOnly, enabled));
  }

  private static String describe(String allowedCharPattern) {
    if (OtpField.NUMERIC.equals(allowedCharPattern)) {
      return "Numeric";
    }
    if (OtpField.ALPHANUMERIC.equals(allowedCharPattern)) {
      return "Alphanumeric";
    }
    return "Hexadecimal";
  }
}

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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Custom characters")
@SuppressWarnings("serial")
@Route(value = "otpfield/characters", layout = OtpFieldDemoView.class)
public class CharacterPatternDemo extends VerticalLayout {

  public CharacterPatternDemo() {
    // Beyond the OtpField.NUMERIC and OtpField.ALPHANUMERIC constants, any single-character
    // regular expression works as an allowed character pattern.
    OtpField hex = new OtpField("Hexadecimal, custom pattern", 4);
    hex.setAllowedCharPattern("[0-9A-Fa-f]");
    hex.setCaseConversion(OtpCaseConversion.UPPERCASE);
    hex.setHelperText("Accepts 0-9 and A-F; lower case is converted");

    // The pattern also filters what may be typed, before case conversion is applied, so a pattern
    // combined with case conversion should accept both cases.
    OtpField letters = new OtpField("Consonants only", 5);
    letters.setAllowedCharPattern("[B-DF-HJ-NP-TV-Zb-df-hj-np-tv-z]");
    letters.setCaseConversion(OtpCaseConversion.UPPERCASE);
    letters.setPlaceholder("-");

    // Masking is visual only, and the glyph is configurable.
    OtpField masked = new OtpField("Masked with a custom glyph", 6);
    masked.setMasked(true);
    masked.setMaskGlyph("*");
    masked.setHelperText("The value is unchanged; masking only hides it on screen");

    add(new Paragraph("Disallowed characters are dropped silently, on typing and on paste alike. "
        + "Try pasting \"12-34\" into the hexadecimal field."), hex, letters, masked);
  }
}

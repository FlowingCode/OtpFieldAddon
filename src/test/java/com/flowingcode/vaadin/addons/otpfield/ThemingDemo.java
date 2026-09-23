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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@DemoSource(value = "/src/test/resources/META-INF/frontend/styles/otp-field-add-on-demo-styles.css",
    caption = "Styles", language = "css")
@PageTitle("Theming")
@SuppressWarnings("serial")
@CssImport("./styles/otp-field-add-on-demo-styles.css")
@Route(value = "otpfield/theming", layout = OtpFieldDemoView.class)
public class ThemingDemo extends VerticalLayout {

  public ThemingDemo() {
    // The field is styled entirely through documented custom properties and shadow parts, so the
    // same CSS works under base styles, Aura and Lumo.
    OtpField compact = new OtpField("Compact");
    compact.addClassName("otp-compact");

    OtpField banking = new OtpField("Banking token", 8);
    banking.setAllowedCharPattern("[0-9A-Fa-f]");
    banking.setCaseConversion(OtpCaseConversion.UPPERCASE);
    banking.setPlaceholder("0");
    banking.addClassName("otp-banking");

    add(compact, banking);
  }
}

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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Prefix and suffix")
@SuppressWarnings("serial")
@Route(value = "otpfield/prefix-suffix", layout = OtpFieldDemoView.class)
public class PrefixSuffixDemo extends VerticalLayout {

  public PrefixSuffixDemo() {
    OtpField otp = new OtpField("Verification code");

    // Prefix and suffix components sit inline with the slots, as on the built-in input fields.
    otp.setPrefixComponent(new Icon(VaadinIcon.MOBILE));
    otp.setSuffixComponent(new Span("SMS"));

    // The tooltip and the accessible name behave as on any other Vaadin field.
    otp.setTooltipText("The six-digit code from the text message");
    otp.setAriaLabel("Verification code from the text message");

    // The whole component is a single tab stop, and focus() moves to the active slot.
    Button focus = new Button("Focus the field", event -> otp.focus());

    add(otp, focus);
  }
}

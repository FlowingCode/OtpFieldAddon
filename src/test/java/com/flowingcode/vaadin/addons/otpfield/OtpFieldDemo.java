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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Basic")
@SuppressWarnings("serial")
@Route(value = "otpfield/basic", layout = OtpFieldDemoView.class)
public class OtpFieldDemo extends VerticalLayout {

  public OtpFieldDemo() {
    Span value = new Span();
    value.setId("value");

    Span complete = new Span();
    complete.setId("complete");

    OtpField otp = new OtpField("Verification code");
    otp.setHelperText("Enter the 6-digit code we sent to your phone");

    // The value is whatever the user typed so far, and may be shorter than the length.
    otp.addValueChangeListener(event -> value.setText("Value: \"" + event.getValue() + "\""));

    // The complete event fires when the code reaches its full length, so applications never have
    // to check the length themselves.
    otp.addCompleteListener(event -> complete.setText("Complete: " + event.getValue()));

    add(otp, value, complete);
  }
}

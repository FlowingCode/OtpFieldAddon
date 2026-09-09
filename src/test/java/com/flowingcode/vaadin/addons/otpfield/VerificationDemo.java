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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Verification")
@SuppressWarnings("serial")
@Route(value = "otpfield/verification", layout = OtpFieldDemoView.class)
public class VerificationDemo extends VerticalLayout {

  /** The only code this demo accepts. Verifying a real code is the application's job. */
  private static final String EXPECTED_CODE = "123456";

  public VerificationDemo() {
    Span result = new Span();
    result.setId("result");

    OtpField otp = new OtpField("Verification code");
    otp.setAutofocus(true);

    // Partial codes never reach the server in this mode; the complete event still does.
    otp.setValueChangeMode(ValueChangeMode.ON_CHANGE);

    otp.addCompleteListener(event -> {
      if (EXPECTED_CODE.equals(event.getValue())) {
        result.setText("Code accepted");
        otp.setInvalid(false);
        otp.setReadOnly(true);
      } else {
        // A wrong code is a server-side error, not a constraint violation, so it is reported with
        // setErrorMessage()/setInvalid() rather than through a validator. Emptying the field first
        // matters: clear() revalidates the constraints, which would otherwise reset the error that
        // was just set.
        result.setText("");
        otp.clear();
        otp.focus();
        otp.setErrorMessage("Incorrect code");
        otp.setInvalid(true);
      }
    });

    add(new Paragraph("The code for this demo is " + EXPECTED_CODE + "."), otp, result);
  }
}

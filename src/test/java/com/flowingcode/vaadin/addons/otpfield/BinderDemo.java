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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@DemoSource(clazz = Enrollment.class)
@PageTitle("Binder")
@SuppressWarnings("serial")
@Route(value = "otpfield/binder", layout = OtpFieldDemoView.class)
public class BinderDemo extends VerticalLayout {

  public BinderDemo() {
    Span status = new Span();
    status.setId("status");

    OtpField otp = new OtpField("Security token", 8);
    otp.setAllowedCharPattern(OtpField.ALPHANUMERIC);
    otp.setCaseConversion(OtpCaseConversion.UPPERCASE);
    otp.setPlaceholder("_");
    otp.setI18n(new OtpFieldI18n().setRequiredErrorMessage("Please enter the token")
        .setIncompleteErrorMessage("The token has 8 characters")
        .setInvalidErrorMessage("This token predates the current format"));

    Binder<Enrollment> binder = new Binder<>(Enrollment.class);

    // The required and the incomplete constraints are reported by getDefaultValidator(), so the
    // binder picks them up without any extra configuration.
    binder.forField(otp).asRequired("Please enter the token").bind(Enrollment::getToken,
        Enrollment::setToken);

    Enrollment enrollment = new Enrollment();
    binder.setBean(enrollment);

    Button save = new Button("Save", event -> {
      if (binder.validate().isOk()) {
        status.setText("Saved token: " + enrollment.getToken());
      } else {
        status.setText("The form is not valid yet");
      }
    });
    save.setId("save");

    Button legacy = new Button("Load a stored token", event -> {
      // A code kept from before the field was configured this way: too long, and with characters
      // the field no longer accepts. setValue takes it and the invalid constraint reports it, so
      // that loading the bean does not fail outright.
      Enrollment stored = new Enrollment();
      stored.setToken("old-token-2019");
      binder.setBean(stored);
      binder.validate();
      status.setText("Loaded a stored token that no longer fits this field");
    });
    legacy.setId("legacy");

    add(otp, new HorizontalLayout(save, legacy), status);
  }
}

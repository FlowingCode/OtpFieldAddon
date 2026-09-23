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
@PageTitle("Value change modes")
@SuppressWarnings("serial")
@Route(value = "otpfield/value-change-mode", layout = OtpFieldDemoView.class)
public class ValueChangeModeDemo extends VerticalLayout {

  public ValueChangeModeDemo() {
    // EAGER is the default: the server sees every keystroke, so getValue() is always current.
    add(new Paragraph("Each field counts the value change events the server receives, so the "
        + "trade-off between a live value and not transmitting partial codes is visible."));
    add(field("EAGER (default): one event per keystroke", ValueChangeMode.EAGER));

    // ON_CHANGE only transmits a committed code: on blur, or when it becomes complete. The
    // complete event still arrives either way.
    add(field("ON_CHANGE: only a committed code is transmitted", ValueChangeMode.ON_CHANGE));

    // LAZY waits for a pause in typing, TIMEOUT sends at most one event per interval.
    add(field("LAZY: one event once typing pauses", ValueChangeMode.LAZY));
  }

  private VerticalLayout field(String label, ValueChangeMode mode) {
    Span events = new Span("value changes: 0");
    Span complete = new Span();

    OtpField otp = new OtpField(label);
    otp.setValueChangeMode(mode);

    int[] count = {0};
    otp.addValueChangeListener(
        event -> events.setText("value changes: " + ++count[0] + ", value \"" + event.getValue() + "\""));
    otp.addCompleteListener(event -> complete.setText("complete: " + event.getValue()));

    VerticalLayout layout = new VerticalLayout(otp, events, complete);
    layout.setPadding(false);
    return layout;
  }
}

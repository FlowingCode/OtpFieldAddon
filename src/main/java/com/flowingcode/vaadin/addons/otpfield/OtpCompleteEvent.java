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

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import lombok.Getter;

/**
 * Fired when the code entered in an {@link OtpField} becomes complete, that is, when its length
 * reaches {@link OtpField#getLength()}.
 * <p>
 * The event is fired regardless of the {@link com.vaadin.flow.data.value.ValueChangeMode} of the
 * field, and it is fired again if the code becomes incomplete and then complete once more. It is
 * not fired for a programmatic {@link OtpField#setValue(String)}.
 *
 * @author Flowing Code
 * @since 1.0.0
 * @see OtpField#addCompleteListener(com.vaadin.flow.component.ComponentEventListener)
 */
@DomEvent("otp-complete")
@SuppressWarnings("serial")
public class OtpCompleteEvent extends ComponentEvent<OtpField> {

  /** The complete code. */
  @Getter
  private final String value;

  /**
   * Creates a new complete event.
   *
   * @param source the field that fired the event
   * @param fromClient {@code true} if the event originated from the client side
   * @param value the complete code
   */
  public OtpCompleteEvent(OtpField source, boolean fromClient,
      @EventData("event.detail.value") String value) {
    super(source, fromClient);
    this.value = value;
  }
}

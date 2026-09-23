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
package com.flowingcode.vaadin.addons.otpfield.test;

import com.flowingcode.vaadin.addons.otpfield.OtpCaseConversion;
import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.flowingcode.vaadin.addons.otpfield.OtpFieldI18n;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Assert;
import org.junit.Test;

public class SerializationTest {

  private void testSerializationOf(Object obj) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(obj);
    }
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      obj.getClass().cast(in.readObject());
    }
  }

  @Test
  public void testSerialization() throws ClassNotFoundException, IOException {
    try {
      testSerializationOf(new OtpField());
    } catch (Exception e) {
      Assert.fail("Problem while testing serialization: " + e.getMessage());
    }
  }

  @Test
  public void testSerializationOfConfiguredField() throws ClassNotFoundException, IOException {
    OtpField field = new OtpField("Security token", 8);
    field.setAllowedCharPattern(OtpField.ALPHANUMERIC);
    field.setCaseConversion(OtpCaseConversion.UPPERCASE);
    field.setMasked(true);
    field.setPlaceholder("_");
    field.setI18n(new OtpFieldI18n().setIncompleteErrorMessage("Incomplete"));
    field.setValue("ABCD1234");
    field.addCompleteListener(event -> {
    });

    try {
      testSerializationOf(field);
    } catch (Exception e) {
      Assert.fail("Problem while testing serialization: " + e.getMessage());
    }
  }
}

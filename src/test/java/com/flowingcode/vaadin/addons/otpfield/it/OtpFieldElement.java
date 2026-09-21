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
package com.flowingcode.vaadin.addons.otpfield.it;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/** Page object for {@code <fc-otp-field>}. */
@Element("fc-otp-field")
public class OtpFieldElement extends TestBenchElement {

  /** Returns the single real input that carries all the editing. */
  public TestBenchElement getInputElement() {
    return (TestBenchElement) findElement(By.tagName("input"));
  }

  /** Returns the current value of the field. */
  public String getOtpValue() {
    return getPropertyString("value");
  }

  /** Focuses the field and types the given text into it. */
  public void type(CharSequence... keys) {
    getInputElement().sendKeys(keys);
  }

  /** Focuses the field without changing the caret position. */
  public void focusField() {
    executeScript("arguments[0].focus()", this);
  }

  /** Simulates pasting the given text at the current caret position. */
  public void paste(String text) {
    executeScript("const input = arguments[0].querySelector('input');"
        + "const data = new DataTransfer();"
        + "data.setData('text', arguments[1]);"
        + "input.dispatchEvent(new ClipboardEvent('paste',"
        + "  { clipboardData: data, bubbles: true, cancelable: true, composed: true }));", this,
        text);
  }

  /** Simulates dropping the given text on the field. */
  public void dropText(String text) {
    executeScript("const input = arguments[0].querySelector('input');"
        + "const data = new DataTransfer();"
        + "data.setData('text', arguments[1]);"
        + "input.dispatchEvent(new DragEvent('drop',"
        + "  { dataTransfer: data, bubbles: true, cancelable: true, composed: true }));", this,
        text);
  }

  /** Returns the rendered text of every slot, in visual order. */
  @SuppressWarnings("unchecked")
  public List<String> getSlotTexts() {
    return (List<String>) executeScript(
        "return Array.from(arguments[0].shadowRoot.querySelectorAll('[part~=\"slot\"]'))"
            + ".map(slot => slot.textContent.trim())",
        this);
  }

  /** Returns the number of rendered slots. */
  public int getSlotCount() {
    return getSlotTexts().size();
  }

  /** Returns the index of the slot that carries the focus affordance, or -1 if there is none. */
  public int getActiveSlotIndex() {
    return ((Number) executeScript(
        "return Array.from(arguments[0].shadowRoot.querySelectorAll('[part~=\"slot\"]'))"
            + ".findIndex(slot => slot.hasAttribute('active'))",
        this)).intValue();
  }

  /** Returns whether the slot at the given index holds a character. */
  public boolean isSlotFilled(int index) {
    return hasSlotAttribute(index, "filled");
  }

  /** Returns whether the slot at the given index carries the error styling. */
  public boolean isSlotInvalid(int index) {
    return hasSlotAttribute(index, "invalid");
  }

  /** Returns whether the slot at the given index renders the mask glyph. */
  public boolean isSlotMasked(int index) {
    return hasSlotAttribute(index, "masked");
  }

  private boolean hasSlotAttribute(int index, String attribute) {
    return Boolean.TRUE.equals(executeScript(
        "return arguments[0].shadowRoot.querySelectorAll('[part~=\"slot\"]')[arguments[1]]"
            + ".hasAttribute(arguments[2])",
        this, index, attribute));
  }

  /** Returns the start of the current selection in the real input. */
  public int getSelectionStart() {
    return ((Number) executeScript("return arguments[0].querySelector('input').selectionStart",
        this)).intValue();
  }

  /** Returns the end of the current selection in the real input. */
  public int getSelectionEnd() {
    return ((Number) executeScript("return arguments[0].querySelector('input').selectionEnd", this))
        .intValue();
  }

  /** Returns whether the real input is the active element of the document. */
  public boolean isFieldFocused() {
    return Boolean.TRUE.equals(executeScript(
        "return arguments[0].querySelector('input') === arguments[0].getRootNode().activeElement",
        this));
  }

  /** Clicks the slot at the given index, which places the caret there. */
  public void clickSlot(int index) {
    // The slots do not take pointer events, so the click goes to the input layered over them.
    new Actions(getDriver()).moveToElement(getInputElement(), slotOffsetX(index), 0).click()
        .perform();
  }

  /** The horizontal offset of a slot's centre from the centre of the input. */
  private int slotOffsetX(int index) {
    WebElement slot = (WebElement) executeScript(
        "return arguments[0].shadowRoot.querySelectorAll('[part~=\"slot\"]')[arguments[1]]", this,
        index);
    Rectangle field = getInputElement().getRect();
    Rectangle target = slot.getRect();
    return target.getX() + target.getWidth() / 2 - (field.getX() + field.getWidth() / 2);
  }

  /** Presses and holds the primary button over the slot at the given index. */
  public void pressAndHoldSlot(int index) {
    new Actions(getDriver()).moveToElement(getInputElement(), slotOffsetX(index), 0).clickAndHold()
        .perform();
  }

  /** Releases the primary button. */
  public void releasePointer() {
    new Actions(getDriver()).release().perform();
  }

  /** Returns the rendered width of every slot, in visual order. */
  @SuppressWarnings("unchecked")
  public List<Number> getSlotWidths() {
    return (List<Number>) executeScript(
        "return Array.from(arguments[0].shadowRoot.querySelectorAll('[part~=\"slot\"]'))"
            + ".map(slot => slot.getBoundingClientRect().width)",
        this);
  }

  /** Returns whether every slot lies within the horizontal bounds of the field. */
  public boolean areAllSlotsWithinTheField() {
    return Boolean.TRUE.equals(executeScript(
        "const field = arguments[0].shadowRoot.querySelector('[part=\"field\"]');"
            + "const bounds = field.getBoundingClientRect();"
            + "return Array.from(arguments[0].shadowRoot.querySelectorAll('[part~=\"slot\"]'))"
            + "  .every(slot => {"
            + "    const rect = slot.getBoundingClientRect();"
            + "    return rect.left >= bounds.left - 1 && rect.right <= bounds.right + 1;"
            + "  })",
        this));
  }

  /** Counts the nodes inside the field that can take focus, light and shadow DOM alike. */
  public int countFocusableNodes() {
    return ((Number) executeScript(
        "const selector = 'input, textarea, select, button, a[href], [tabindex]:not([tabindex=\"-1\"])';"
            + "const light = Array.from(arguments[0].querySelectorAll(selector));"
            + "const shadow = Array.from(arguments[0].shadowRoot.querySelectorAll(selector));"
            + "return light.concat(shadow).filter(node => !node.disabled).length",
        this)).intValue();
  }

  /** Returns the {@code for} attribute of the slotted label. */
  public String getLabelFor() {
    return (String) executeScript(
        "const label = arguments[0].querySelector('[slot=\"label\"]');"
            + "return label ? label.getAttribute('for') : null",
        this);
  }

  /** Returns whether the helper text is referenced by the input's {@code aria-describedby}. */
  public boolean isDescribedByTheHelperText() {
    return isDescribedBy("[slot='helper']");
  }

  /** Returns whether the error message is referenced by the input's {@code aria-describedby}. */
  public boolean isDescribedByTheErrorMessage() {
    return isDescribedBy("[slot='error-message']");
  }

  private boolean isDescribedBy(String selector) {
    return Boolean.TRUE.equals(executeScript(
        "const node = arguments[0].querySelector(arguments[1]);"
            + "const input = arguments[0].querySelector('input');"
            + "const described = input.getAttribute('aria-describedby');"
            + "return !!(node && node.id && described && described.split(' ').includes(node.id))",
        this, selector));
  }

  /** A dump of the accessibility-relevant state, for diagnosing a failed expectation. */
  public String describeAccessibility() {
    return (String) executeScript(
        "const el = arguments[0];"
            + "const input = el.querySelector('input');"
            + "const attrs = {};"
            + "Array.from(input.attributes).forEach(a => attrs[a.name] = a.value);"
            + "return JSON.stringify({"
            + " input: attrs,"
            + " light: Array.from(el.children).map(n => n.nodeName + '[slot=' + (n.getAttribute('slot') || '')"
            + "   + ' id=' + (n.id || '') + ' role=' + (n.getAttribute('role') || '') + ']'),"
            + " slotsAriaHidden: el.shadowRoot.querySelector('[part=\"slots\"]').getAttribute('aria-hidden')"
            + "})",
        this);
  }

  /** Returns whether the real input stays within the bounds of the field it belongs to. */
  public boolean isInputBoundedByField() {
    return Boolean.TRUE.equals(executeScript(
        "const field = arguments[0].shadowRoot.querySelector('[part=\"field\"]').getBoundingClientRect();"
            + "const input = arguments[0].querySelector('input').getBoundingClientRect();"
            + "return input.left >= field.left - 1 && input.right <= field.right + 1"
            + "  && input.top >= field.top - 1 && input.bottom <= field.bottom + 1",
        this));
  }

  /** Returns the tag name of the topmost element at the centre of this field. */
  public String getTopmostElementAtCentre() {
    return (String) executeScript(
        "const rect = arguments[0].getBoundingClientRect();"
            + "const node = document.elementFromPoint(rect.left + rect.width / 2,"
            + "  rect.top + rect.height / 2);"
            + "return node ? node.localName : null",
        this);
  }

  /** A dump of the layering of every field on the page, for diagnosing a stolen click. */
  public String describeLayering() {
    return (String) executeScript(
        "return JSON.stringify(Array.from(document.querySelectorAll('fc-otp-field')).map(el => {"
            + " const wrapper = el.shadowRoot.querySelector('.slots-layer');"
            + " const input = el.querySelector('input');"
            + " return {"
            + "   label: el.label,"
            + "   wrapperClass: wrapper ? wrapper.className : null,"
            + "   wrapperDisplay: wrapper ? getComputedStyle(wrapper).display : null,"
            + "   wrapperPosition: wrapper ? getComputedStyle(wrapper).position : null,"
            + "   inputRect: input.getBoundingClientRect(),"
            + "   fieldRect: el.shadowRoot.querySelector('[part=\"field\"]').getBoundingClientRect()"
            + " };"
            + "}))",
        this);
  }

  /** Sets a property on the element, to exercise a client-side behaviour in isolation. */
  public void setElementProperty(String name, Object value) {
    executeScript("arguments[0][arguments[1]] = arguments[2]", this, name, value);
  }

  /** Returns whether the field is in the invalid state. */
  public boolean isFieldInvalid() {
    return hasAttribute("invalid");
  }

  /** Returns the currently displayed error message, or an empty string if there is none. */
  public String getErrorMessage() {
    Object message = executeScript(
        "const node = arguments[0].querySelector('[slot=\"error-message\"]');"
            + "return node ? node.textContent.trim() : ''",
        this);
    return message == null ? "" : message.toString();
  }

  /** Returns the {@code aria-hidden} attribute of the slot container. */
  public String getSlotsAriaHidden() {
    return (String) executeScript(
        "return arguments[0].shadowRoot.querySelector('[part=\"slots\"]')"
            + ".getAttribute('aria-hidden')",
        this);
  }

  /** Returns the computed writing direction of the slot container. */
  public String getSlotsDirection() {
    return (String) executeScript(
        "return getComputedStyle(arguments[0].shadowRoot.querySelector('[part=\"slots\"]'))"
            + ".direction",
        this);
  }

  /** Starts recording the {@code otp-complete} events the field fires. */
  public void recordCompleteEvents() {
    executeScript("const field = arguments[0];"
        + "field.__completeEvents = [];"
        + "field.addEventListener('otp-complete',"
        + "  event => field.__completeEvents.push(event.detail.value));", this);
  }

  /**
   * Returns the value carried by every {@code otp-complete} event fired since
   * {@link #recordCompleteEvents()}, in order.
   */
  @SuppressWarnings("unchecked")
  public List<String> getRecordedCompleteEvents() {
    return (List<String>) executeScript("return arguments[0].__completeEvents || []", this);
  }
}

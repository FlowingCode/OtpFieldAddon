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
import { css, html, LitElement, nothing } from 'lit';
import { PolylitMixin } from '@vaadin/component-base/src/polylit-mixin.js';
import { TooltipController } from '@vaadin/component-base/src/tooltip-controller.js';
import { InputController } from '@vaadin/field-base/src/input-controller.js';
import { InputFieldMixin } from '@vaadin/field-base/src/input-field-mixin.js';
import { LabelledInputController } from '@vaadin/field-base/src/labelled-input-controller.js';
import { inputFieldShared } from '@vaadin/field-base/src/styles/input-field-shared-styles.js';

/**
 * Whitespace and the separators commonly used to group a printed code (`123-456`, `123 456`).
 * They are dropped from typed, pasted and dropped text unless `allowedCharPattern` accepts them.
 */
const SEPARATOR = /[\s\u2013\u2014\u2212._\/-]/u;

/** Upper bound for `length`, mirrored by the Flow wrapper. */
const MAX_LENGTH = 24;

/**
 * The `field-base` mixins are published but not typed for external composition, so the mixin stack
 * is applied without types.
 *
 * Because of that, members declared here must not collide with the mixins' own `__`-prefixed
 * internals either: `InputControlMixin`, for instance, assigns `this.__allowedCharRegExp` as an
 * instance property, which would shadow a method of the same name declared on this class.
 */
const OtpFieldBase: any = InputFieldMixin(PolylitMixin(LitElement));

/**
 * `<fc-otp-field>` is a field for entering a short verification code (a one-time password, a 2FA
 * or SMS code, a banking token). It renders one visual slot per character over a single native
 * `<input>`, so editing, selection, undo, IME and `one-time-code` autofill all stay native while
 * the visible characters are painted by the slots.
 */
export class OtpField extends OtpFieldBase {
  static get is() {
    return 'fc-otp-field';
  }

  static get properties() {
    return {
      /** Number of slots, and the length at which the code is complete. */
      length: {
        type: Number,
        value: 6,
        sync: true,
        observer: '__lengthChanged',
      },

      /** When true, filled slots render `maskGlyph` instead of the character. */
      masked: {
        type: Boolean,
        value: false,
        reflectToAttribute: true,
      },

      /** The glyph rendered in filled slots while `masked`. */
      maskGlyph: {
        type: String,
        value: '\u2022',
      },

      /** Case conversion applied to typed, pasted and programmatically set values. */
      caseConversion: {
        type: String,
        value: 'none',
        observer: '__caseConversionChanged',
      },

      /** The `inputmode` of the native input, delegated as a property. */
      inputMode: {
        type: String,
        value: 'numeric',
      },

      /** Start of the current selection, used to render the active slot. */
      _caretStart: {
        type: Number,
        value: 0,
        state: true,
      },

      /** End of the current selection, used to render the selected slots. */
      _caretEnd: {
        type: Number,
        value: 0,
        state: true,
      },
    };
  }

  static get delegateProps() {
    return [...super.delegateProps, 'inputMode'];
  }

  static get styles() {
    return [
      inputFieldShared,
      css`
        :host {
          --_slot-border-width: var(--fc-otp-field-slot-border-width, var(--vaadin-input-field-border-width, 1px));
          --_slot-border-color: var(--fc-otp-field-slot-border-color, var(--vaadin-border-color, currentColor));
          --_slot-background: var(
            --fc-otp-field-slot-background,
            var(--vaadin-input-field-background, var(--vaadin-background-color, transparent))
          );
          --_slot-color: var(
            --fc-otp-field-slot-color,
            var(--vaadin-input-field-value-color, var(--vaadin-text-color, currentColor))
          );
        }

        [part='field'] {
          grid-area: input;
          display: flex;
          align-items: center;
          gap: var(--vaadin-input-field-gap, var(--vaadin-gap-s, 0.25em));
          width: max-content;
        }

        /*
         * An empty prefix or suffix slot must not become a flex item, or the gap after it would
         * push the slots out of the field.
         */
        [part='field'] > slot[name='prefix'],
        [part='field'] > slot[name='suffix'] {
          display: contents;
        }

        /*
         * Anchors the input over the slots without covering the prefix and suffix.
         *
         * The class must not end in "container". field-base-styles carries a
         * [class$='container'] rule that sets display:contents for its own
         * .vaadin-field-container, and a name matching it leaves this element generating no box at
         * all. Its position would then be ignored and the absolutely positioned input would
         * resolve against a containing block far up the tree, spreading across the view and
         * swallowing every click behind it. display:block is stated outright so that no future
         * rule of that shape can quietly take the box away again.
         */
        .slots-layer {
          display: block;
          position: relative;
          flex: none;
        }

        [part='slots'] {
          display: flex;
          gap: var(--fc-otp-field-slot-gap, var(--vaadin-gap-s, 0.25em));
          /* A code reads left to right even in an RTL context. */
          direction: ltr;
          /* All pointer interaction goes to the input layered on top. */
          pointer-events: none;
        }

        [part='slot'] {
          box-sizing: border-box;
          position: relative;
          display: flex;
          flex: none;
          align-items: center;
          justify-content: center;
          width: var(--fc-otp-field-slot-width, 2.25em);
          height: var(--fc-otp-field-slot-height, 2.5em);
          background: var(--_slot-background);
          border: var(--_slot-border-width) solid var(--_slot-border-color);
          border-radius: var(
            --fc-otp-field-slot-border-radius,
            var(--vaadin-input-field-border-radius, var(--vaadin-radius-m, 0.25em))
          );
          color: var(--_slot-color);
          font-size: var(--fc-otp-field-slot-font-size, var(--vaadin-input-field-value-font-size, inherit));
          line-height: var(--vaadin-input-field-value-line-height, normal);
          font-weight: var(--vaadin-input-field-value-font-weight, 400);
          font-variant-numeric: tabular-nums;
          overflow: hidden;
          user-select: none;
        }

        [part='slot']:not([filled]) {
          color: var(--vaadin-input-field-placeholder-color, var(--vaadin-text-color-secondary, inherit));
        }

        /* The focus affordance follows the caret (D3). */
        :host([focused]:not([readonly])) [part='slot'][active],
        :host([focused]:not([readonly])) [part='slot'][selected] {
          --_slot-border-width: var(
            --fc-otp-field-slot-border-width-active,
            var(--vaadin-focus-ring-width, 2px)
          );
          --_slot-border-color: var(
            --fc-otp-field-slot-border-color-active,
            var(--vaadin-focus-ring-color, currentColor)
          );
          --_slot-background: var(
            --fc-otp-field-slot-background-active,
            var(--vaadin-input-field-background, var(--vaadin-background-color, transparent))
          );
          z-index: 1;
        }

        /* Every slot carries the error styling, not only the active one (FR-29). */
        :host([invalid]) [part='slot'] {
          --_slot-border-width: var(
            --fc-otp-field-slot-border-width-invalid,
            var(--fc-otp-field-slot-border-width, var(--vaadin-input-field-border-width, 1px))
          );
          --_slot-border-color: var(
            --fc-otp-field-slot-border-color-invalid,
            var(--vaadin-input-field-error-color, var(--vaadin-text-color, currentColor))
          );
          --_slot-background: var(
            --fc-otp-field-slot-background-invalid,
            var(--vaadin-input-field-background, var(--vaadin-background-color, transparent))
          );
        }

        :host([readonly]) [part='slot'] {
          border-style: dashed;
        }

        :host([disabled]) [part='slot'] {
          --_slot-color: var(
            --vaadin-input-field-disabled-text-color,
            var(--vaadin-text-color-disabled, currentColor)
          );
          --_slot-background: var(
            --vaadin-input-field-disabled-background,
            var(--vaadin-background-container-strong, transparent)
          );
          --_slot-border-color: transparent;
        }

        /* The native caret is hidden, so the active slot paints one of its own. */
        [part='slot'][active]:not([filled])::after {
          content: '';
          position: absolute;
          width: 1px;
          height: 1lh;
          background: var(--fc-otp-field-caret-color, var(--_slot-color));
          opacity: 0;
        }

        :host([focused]:not([readonly]):not([disabled])) [part='slot'][active]:not([filled])::after {
          animation: fc-otp-field-blink 1.2s step-end infinite;
        }

        @keyframes fc-otp-field-blink {
          0%,
          50% {
            opacity: 1;
          }
          50.1%,
          100% {
            opacity: 0;
          }
        }

        @media (prefers-reduced-motion: reduce) {
          :host([focused]:not([readonly]):not([disabled])) [part='slot'][active]:not([filled])::after {
            animation: none;
            opacity: 1;
          }
        }

        /*
         * The real input covers the slots with transparent text and caret, so that editing,
         * selection, IME and autofill stay native while the slots paint the value.
         */
        ::slotted(input) {
          position: absolute;
          inset: 0;
          box-sizing: border-box;
          width: 100%;
          height: 100%;
          margin: 0;
          padding: 0;
          border: 0;
          outline: none;
          appearance: none;
          background: transparent;
          color: transparent;
          caret-color: transparent;
          font: inherit;
          direction: ltr;
          cursor: default;
        }

        :host([disabled]) ::slotted(input) {
          cursor: var(--vaadin-disabled-cursor, default);
        }

        @media (forced-colors: active) {
          [part='slot'] {
            --_slot-background: Field;
            --_slot-color: FieldText;
          }

          :host([focused]) [part='slot'][active] {
            --_slot-border-color: Highlight;
          }
        }
      `,
    ];
  }

  constructor() {
    super();
    this.allowedCharPattern = '[0-9]';
    this.autocomplete = 'one-time-code';
    this.autocorrect = 'off';
    this.autocapitalize = 'none';
    this.__composing = false;
    this.__pointerType = '';
    this.__boundUpdateCaret = () => this.__updateCaret();
  }

  /** @protected */
  render() {
    return html`
      <div class="vaadin-field-container">
        <div part="label">
          <slot name="label"></slot>
          <span part="required-indicator" aria-hidden="true" @click="${this.focus}"></span>
        </div>

        <div part="field">
          <slot name="prefix"></slot>
          <div class="slots-layer">
            <div part="slots" aria-hidden="true">${this.__renderSlots()}</div>
            <slot name="input"></slot>
          </div>
          <slot name="suffix"></slot>
        </div>

        <div part="helper-text">
          <slot name="helper"></slot>
        </div>

        <div part="error-message">
          <slot name="error-message"></slot>
        </div>
        <slot name="tooltip"></slot>
      </div>
    `;
  }

  /**
   * A clear button would compete with the slots for the field area, so the field does not have
   * one. Returning `null` opts out of `ClearButtonMixin` instead of triggering its warning.
   *
   * @protected
   */
  get clearElement() {
    return null;
  }

  /** @protected */
  get slotStyles() {
    const tag = this.localName;

    return [
      ...super.slotStyles,
      `
        /* The value is painted by the slots, so the input stays invisible even when autofilled. */
        ${tag} > input[slot='input']:autofill {
          -webkit-text-fill-color: transparent !important;
        }

        ${tag} > input[slot='input']::selection {
          background: transparent;
          color: transparent;
        }

        ${tag}:has(> input[slot='input']:autofill)::part(slot) {
          --fc-otp-field-slot-background: var(--vaadin-input-field-autofill-background, lightyellow);
        }
      `,
    ];
  }

  /** @protected */
  ready() {
    super.ready();

    this.addController(
      new InputController(this, (input: HTMLInputElement) => {
        this._setInputElement(input);
        this._setFocusElement(input);
        this.stateTarget = input;
        this.ariaTarget = input;

        input.spellcheck = false;
        input.maxLength = this.length;

        ['keyup', 'select', 'selectionchange', 'focus', 'blur'].forEach((type) => {
          input.addEventListener(type, this.__boundUpdateCaret);
        });
        input.addEventListener('pointerdown', (event: PointerEvent) => this.__onPointerDown(event));
        input.addEventListener('click', (event: MouseEvent) => this.__onClick(event));
        input.addEventListener('dblclick', () => this.__selectAll());
      }),
    );
    this.addController(new LabelledInputController(this.inputElement, this._labelController));

    this._tooltipController = new TooltipController(this);
    this._tooltipController.setPosition('top');
    this._tooltipController.setAriaTarget(this.inputElement);
    this.addController(this._tooltipController);

    this.addEventListener('compositionstart', () => {
      this.__composing = true;
    });
    this.addEventListener('compositionend', () => {
      this.__composing = false;
      // Commit whatever the composition produced through the regular input path, so that it is
      // sanitized exactly like typed text.
      this.inputElement.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
    });
  }

  /**
   * Returns true when the value satisfies the required and the incomplete constraint.
   *
   * @override
   */
  checkValidity() {
    const value = this.value || '';
    if (this.required && value.length === 0) {
      return false;
    }
    if (value.length === 0) {
      return true;
    }

    // Mirrors the server-side constraints: a complete code holding only characters the field
    // accepts. Only a programmatically set value can violate the latter, since typing, pasting and
    // autofill are all filtered.
    const pattern = this.__allowedRegExp();
    const accepted = !pattern || Array.from(value).every((char) => pattern.test(char));
    return value.length === this.length && accepted;
  }

  /**
   * Empties the field and returns the caret to the first slot.
   *
   * @override
   */
  clear() {
    super.clear();
    this.__setSelection(0, 0);
  }

  /**
   * Skips the round trip back to the input when it already holds the value, which would otherwise
   * drop the caret to the end of the code.
   *
   * @protected
   * @override
   */
  _forwardInputValue(value: string) {
    if (this.inputElement && this.inputElement.value === (value == null ? '' : value)) {
      return;
    }
    super._forwardInputValue(value);
  }

  /**
   * @protected
   * @override
   */
  _valueChanged(newValue: string, oldValue: string) {
    const raw = newValue == null ? '' : newValue;
    const converted = this.__convertCase(raw);
    if (converted !== raw) {
      // Re-enters this observer with the case the field displays (FR-5).
      this.value = converted;
      return;
    }

    // A value that is too long, or that holds characters the field does not accept, is kept as it
    // is: only typing, pasting and autofill are filtered. The field reports such a value through
    // its validator, so discarding it here would put the value out of step with the server.
    super._valueChanged(newValue, oldValue);

    if (!raw) {
      this.__setSelection(0, 0);
    }
  }

  /**
   * All insertion is overwrite-from-the-caret, so typing fills the active slot instead of pushing
   * the rest of the code to the right (FR-8).
   *
   * @protected
   * @override
   */
  _onBeforeInput(event: InputEvent) {
    if (this.readonly || this.disabled) {
      return;
    }

    const type = event.inputType;
    if (!type || !type.startsWith('insert')) {
      // Deletion, undo and redo keep their native behaviour (FR-10, FR-11).
      return;
    }

    if (type === 'insertCompositionText') {
      // Composed text is committed on `compositionend`, except for numeric codes, where an IME
      // cannot produce anything acceptable in the first place.
      if (this.inputMode === 'numeric') {
        event.preventDefault();
      }
      return;
    }

    event.preventDefault();
    this.__insert(event.data || (event.dataTransfer && event.dataTransfer.getData('text')) || '');
  }

  /**
   * A pasted code is filtered rather than rejected: disallowed characters and grouping separators
   * are dropped and the rest is distributed over the slots (FR-16, FR-17).
   *
   * @protected
   * @override
   */
  _onPaste(event: ClipboardEvent) {
    if (this.readonly || this.disabled) {
      return;
    }

    event.preventDefault();
    this.__insert((event.clipboardData && event.clipboardData.getData('text')) || '');
  }

  /**
   * Dropped text is handled like a paste (FR-18).
   *
   * @protected
   * @override
   */
  _onDrop(event: DragEvent) {
    if (this.readonly || this.disabled) {
      return;
    }

    event.preventDefault();
    this.inputElement.focus();
    this.__insert((event.dataTransfer && event.dataTransfer.getData('text')) || '');
  }

  /**
   * @protected
   * @override
   */
  _onInput(event: Event) {
    if (this.__composing) {
      // Do not let partially composed text reach the value.
      return;
    }

    const input = this.inputElement as HTMLInputElement;
    const sanitized = this.__normalize(input.value);
    if (sanitized !== input.value) {
      // Reached when the browser bypasses `beforeinput`, most notably on autofill.
      const selectionStart = input.selectionStart == null ? sanitized.length : input.selectionStart;
      input.value = sanitized;
      input.setSelectionRange(Math.min(selectionStart, sanitized.length), Math.min(selectionStart, sanitized.length));
    }

    const wasComplete = this.__isComplete(this.value);
    super._onInput(event);
    this.__updateCaret();

    if (!wasComplete && this.__isComplete(this.value)) {
      this.__notifyComplete();
    }
  }

  /** @private */
  __renderSlots() {
    const value = this.value || '';
    const placeholder = this.__placeholderChars();
    const activeIndex = this.__activeIndex();
    const slots = [];

    for (let i = 0; i < this.length; i++) {
      const filled = i < value.length;
      const selected = this._caretEnd > this._caretStart && i >= this._caretStart && i < this._caretEnd;
      slots.push(
        html`<div
          part="slot"
          ?filled="${filled}"
          ?active="${i === activeIndex}"
          ?selected="${selected}"
          ?masked="${filled && this.masked}"
          ?invalid="${this.invalid}"
        >
          ${filled ? (this.masked ? this.maskGlyph : value[i]) : placeholder ? placeholder[i] : nothing}
        </div>`,
      );
    }

    return slots;
  }

  /**
   * The placeholder expanded to one character per slot, or an empty string when there is none
   * (FR-6).
   *
   * @private
   */
  __placeholderChars(): string {
    const placeholder = this.placeholder || '';
    if (placeholder.length === 0) {
      return '';
    }
    if (placeholder.length === 1) {
      return placeholder.repeat(this.length);
    }
    if (placeholder.length === this.length) {
      return placeholder;
    }
    // Only reachable after a length change; the setter rejects any other length.
    return '';
  }

  /**
   * The slot that carries the focus affordance: the one at the caret, clamped to the last slot so
   * that a complete code still shows where the next edit would land. Returns -1 while a range is
   * selected, because then the whole range is highlighted instead.
   *
   * @private
   */
  __activeIndex(): number {
    if (this._caretEnd > this._caretStart) {
      return -1;
    }
    return Math.max(0, Math.min(this._caretStart, this.length - 1));
  }

  /** @private */
  __isComplete(value: string): boolean {
    return !!value && value.length === this.length;
  }

  /**
   * Applies case conversion and drops everything the field does not accept. Grouping separators
   * are dropped unless `allowedCharPattern` accepts them (FR-3, FR-5, FR-16).
   *
   * @private
   */
  __sanitize(text: string): string {
    const converted = this.__convertCase(text);
    const pattern = this.__allowedRegExp();
    let result = '';
    for (const char of converted) {
      if (pattern ? pattern.test(char) : !SEPARATOR.test(char)) {
        result += char;
      }
    }
    return result;
  }

  /**
   * Applies the configured case conversion, which alone is applied to a programmatically set value.
   *
   * @private
   */
  __convertCase(text: string): string {
    if (this.caseConversion === 'upper') {
      return text.toUpperCase();
    }
    if (this.caseConversion === 'lower') {
      return text.toLowerCase();
    }
    return text;
  }

  /** @private */
  __normalize(text: string): string {
    return this.__sanitize(text).slice(0, this.length);
  }

  /**
   * `InputControlMixin` compiles `allowedCharPattern` for its own keydown filtering but does not
   * expose the result, so the pattern is compiled once more here.
   *
   * @private
   */
  __allowedRegExp(): RegExp | null {
    const pattern = this.allowedCharPattern;
    if (!pattern) {
      this.__allowedRegExpSource = null;
      this.__allowedRegExpCache = null;
      return null;
    }
    if (pattern !== this.__allowedRegExpSource) {
      this.__allowedRegExpSource = pattern;
      try {
        this.__allowedRegExpCache = new RegExp(`^${pattern}$`, 'u');
      } catch (error) {
        console.error(error);
        this.__allowedRegExpCache = null;
      }
    }
    return this.__allowedRegExpCache;
  }

  /**
   * Replaces the characters at `[from, to)` with `text`, filling towards the end of the code and
   * discarding whatever does not fit (FR-8, FR-9, FR-16, FR-17).
   *
   * @private
   */
  __insert(text: string, from?: number, to?: number) {
    const input = this.inputElement as HTMLInputElement;
    const current = input.value;
    const start = from == null ? (input.selectionStart == null ? current.length : input.selectionStart) : from;
    const end = to == null ? (input.selectionEnd == null ? start : input.selectionEnd) : to;
    const inserted = this.__sanitize(text);

    if (!inserted && start === end) {
      return;
    }

    const head = current.slice(0, start);
    const tail = current.slice(Math.max(end, start + inserted.length));
    const next = (head + inserted + tail).slice(0, this.length);
    const caret = Math.min(head.length + inserted.length, next.length);

    if (next === current) {
      this.__setSelection(caret, caret);
      return;
    }

    input.value = next;
    input.setSelectionRange(caret, caret);
    input.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
  }

  /**
   * Commits the value and announces the finished code. `change` goes first, so that a consumer
   * reading the value from an `otp-complete` handler sees the complete code whatever the value
   * change mode is.
   *
   * @private
   */
  __notifyComplete() {
    this.inputElement.dispatchEvent(new Event('change', { bubbles: true }));
    this.dispatchEvent(new CustomEvent('otp-complete', { detail: { value: this.value } }));
  }

  /** @private */
  __setSelection(start: number, end: number) {
    const input = this.inputElement as HTMLInputElement;
    if (input) {
      input.setSelectionRange(start, end);
    }
    this._caretStart = start;
    this._caretEnd = end;
  }

  /** @private */
  __updateCaret() {
    const input = this.inputElement as HTMLInputElement;
    if (!input) {
      return;
    }
    this._caretStart = input.selectionStart == null ? 0 : input.selectionStart;
    this._caretEnd = input.selectionEnd == null ? this._caretStart : input.selectionEnd;
  }

  /** @private */
  __selectAll() {
    this.__setSelection(0, (this.inputElement as HTMLInputElement).value.length);
  }

  /**
   * Maps a viewport x coordinate to the slot under it. The caret goes *at* that slot, so clicking
   * a slot makes the next character overwrite it.
   *
   * @private
   */
  __indexFromX(x: number): number {
    const slots = this.shadowRoot.querySelectorAll('[part~="slot"]');
    for (let i = 0; i < slots.length; i++) {
      if (x <= slots[i].getBoundingClientRect().right) {
        return i;
      }
    }
    return slots.length;
  }

  /**
   * The caret of the layered input follows its own (invisible) text metrics, which have nothing to
   * do with the slot geometry, so the caret position is derived from the slot under the pointer
   * instead. Clicking past the last filled slot lands after the last character, so a code never
   * has holes in the middle (FR-13).
   *
   * @private
   */
  __caretFromX(x: number): number {
    return Math.min(this.__indexFromX(x), (this.inputElement as HTMLInputElement).value.length);
  }

  /**
   * Places the caret at the slot under a mouse pointer, on press.
   *
   * It has to be on press rather than on release: `click` and `pointerup` only arrive once the
   * button comes back up, and pressing, typing and then releasing is an ordinary way to use a
   * mouse. Correcting the caret at that point moves it away from the character the user has
   * already entered, which shows up as the next character overwriting the previous one.
   *
   * The default action is prevented so that the browser does not place the caret from the
   * (invisible) text metrics of the input instead, which means focus is taken over here.
   *
   * @private
   */
  __onPointerDown(event: PointerEvent) {
    this.__pointerType = event.pointerType;

    if (this.readonly || this.disabled || event.button !== 0 || event.pointerType !== 'mouse') {
      // Touch and pen keep the default behaviour, so that focus and the on-screen keyboard are
      // unaffected; they are corrected on `click`, which a tap cannot interleave with typing.
      return;
    }

    event.preventDefault();
    this.inputElement.focus();
    this.__placeCaret(event.clientX);
  }

  /**
   * Places the caret for a tap, which `__onPointerDown` deliberately leaves to the default
   * behaviour.
   *
   * @private
   */
  __onClick(event: MouseEvent) {
    if (this.readonly || this.disabled || event.detail > 1 || this.__pointerType === 'mouse') {
      // A double click selects the whole code, which `__selectAll` takes care of.
      return;
    }

    this.__placeCaret(event.clientX);
  }

  /** @private */
  __placeCaret(x: number) {
    const caret = this.__caretFromX(x);
    this.__setSelection(caret, caret);
  }

  /** @private */
  __lengthChanged(length: number, oldLength: number) {
    if (!Number.isInteger(length) || length < 1 || length > MAX_LENGTH) {
      console.error(`<${this.localName}> length must be an integer between 1 and ${MAX_LENGTH}, got ${length}.`);
      this.length = oldLength === undefined ? 6 : oldLength;
      return;
    }

    if (this.inputElement) {
      this.inputElement.maxLength = length;
    }

    if (this.value && this.value.length > length) {
      // Changing the length truncates the value if needed (FR-1).
      this.value = this.value.slice(0, length);
    }
  }

  /** @private */
  __caseConversionChanged() {
    if (this.value) {
      this.value = this.__convertCase(this.value);
    }
  }

  /**
   * Fired when the value reaches `length` because of user input, paste or autofill. It is not
   * fired for a programmatic value change.
   *
   * @event otp-complete
   * @param {Object} detail
   * @param {string} detail.value the complete code
   */
}

if (!customElements.get(OtpField.is)) {
  customElements.define(OtpField.is, OtpField);
}

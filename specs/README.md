# OTP Field Add-On — Specification

| | |
|---|---|
| **Issue** | [FlowingCode/AddonsInternal#155](https://github.com/FlowingCode/AddonsInternal/issues/155) |
| **Status** | Implemented — reconciled with the code on 2026-09-03 |
| **Coordinates** | `com.flowingcode.vaadin.addons:otp-field-addon:1.0.0-SNAPSHOT` |
| **Directory slug** | `otp-field-add-on` |
| **Target platform** | Vaadin 25.2.6, Java 21 |
| **Spec version** | 0.2 |

---

## 1. Purpose

Provide `OtpField`, a Vaadin Flow input component for entering short verification codes (one-time
passwords, 2FA codes, SMS/e-mail confirmation codes, banking tokens). The field renders one visual
slot per character, advances as the user types, accepts a pasted or auto-filled code, and integrates
with `Binder` like any other Vaadin input field.

The name follows @paodb's observation in the issue: the component is an
[OTP input field](https://developer.mozilla.org/en-US/docs/Web/Security/Authentication/OTP), not a
generic "token" field. All public naming (repo, artifact, tag, Java types) uses **OTP**.

## 2. Background and prior art

- A plain `TextField` can hold a code, but it lacks the segmented presentation, per-character focus
  affordance and code-oriented autofill hints that users expect from banking and 2FA flows.
- `PasswordField` masks input but is single-box and semantically about passwords, not codes.
- There is no OTP/verification-code field in the Vaadin Directory today; the existing Vaadin material
  on the topic covers the *authentication* side (Spring Security one-time tokens, TOTP with Google
  Authenticator), not the *input control*. This add-on fills the UI gap and composes with any of
  those back ends.
- Equivalent components are common elsewhere (shadcn `input-otp`, CoreUI OTP input, various
  Angular/React OTP inputs), which gives us a well-trodden UX and accessibility baseline to follow.

## 3. Goals

- **G1** A single, self-contained field component for fixed-length codes.
- **G2** Complete keyboard, paste and autofill behaviour with no application-side glue code.
- **G3** First-class `Binder` integration, including built-in *required* and *incomplete* constraints.
- **G5** Themeable through documented style properties, working under base styles, Aura and Lumo with
  no theme-specific code in the application.
- **G6** Zero required client-side code in the consuming application.

## 4. Non-goals

- **NG1** Generating, sending, verifying, expiring or rate-limiting codes. The component is an input
  control; TOTP/HOTP, SMS delivery, retry limits and lockout stay with the application.
- **NG2** Masked-input/format fields in general (dates, phone numbers, IBANs). Fixed-length code
  entry only.
- **NG3** Variable-length codes, or codes whose length is discovered at runtime from the value.
- **NG4** A Hilla/React binding for the element in v1 (the element is framework-agnostic, so this
  stays possible later).
- **NG5** Vaadin 24 support (see D2 — note that this repo is currently scaffolded for 24; see §15.2).

## 5. Design decisions

### D1 — Custom Lit element plus Flow wrapper, frontend shipped inside the Maven artifact

The TypeScript source lives in `src/main/resources/META-INF/frontend/fc-otp-field/`; there is no
separate npm release.

*Rationale.* Keystroke, paste and focus handling must not round-trip to the server. Shipping the
frontend inside the jar is the established Flowing Code pattern (see `XTermConsoleAddon`'s
`META-INF/frontend/fc-xterm/*.ts`, whose `.ts` sources are compiled by Vaadin's own frontend build)
and keeps one release artifact and one version number.

*Rejected.* (a) A published `@flowingcode/otp-field` npm package — a second release pipeline and
version axis for no benefit to Flow users; it can still be extracted later if there is demand
outside Flow. (b) A Java-only `Composite` of `TextField`s — fights `TextField` internals on focus and
paste, and either round-trips per keystroke or ends up carrying the same inline JS anyway.
(c) Wrapping a third-party OTP element — ties our roadmap and licensing to an upstream we do not
control.

### D2 — Vaadin 25 only (Java 21)

*Rationale.* This answers @paodb's question in the issue. A new add-on has no installed base to
support, and Vaadin 25's shared field interfaces (`InputField`, `HasValidationProperties`,
`ValidationUtil`) plus the base/Aura/Lumo styling model are exactly what this component needs;
supporting 24 as well would mean maintaining two styling stories.

*Consequence for this repo.* The repository was created from **AddonStarter24**, so the default build
is currently Vaadin 24.9.1 / Java 17 with an opt-in `v25` profile, and CI builds both. Adopting D2
means flipping that around — see §15.2.

*Rejected for now.* Dual 24 + 25 branches, or 24 first with a later port. Either remains possible as
a follow-up issue if customer demand appears — the element itself is version-neutral.

### D3 — One real `<input>` spanning the component, with N presentational slots behind it

Slots are `aria-hidden`; the "active slot" highlight follows the caret position.

*Rationale.* Every interaction requirement in the issue — advance on type, backspace to the previous
slot, distribute a pasted code, ignore disallowed characters — becomes *native text editing* in a
one-line input, so there is no focus state machine to get wrong. It also gives native IME/composition
handling, native undo, native selection, and the autofill behaviour that matters most here: browsers
reliably fill `autocomplete="one-time-code"` into a single input, whereas splitting across N inputs
is inconsistent across iOS and Android. Screen readers announce one field rather than "edit, 1 of 6"
six times over.

*Rejected.* N separate `<input>` elements, one per slot. That is the literal reading of the issue and
what most older OTP components do; it costs a hand-written focus/paste/backspace state machine,
degrades autofill, and forces per-slot ARIA labelling.

**This decision needs explicit sign-off**, because it changes the DOM the issue implies while keeping
the described UX identical.

### D4 — Build the element on the mixins in `@vaadin/field-base`

Specifically `InputFieldMixin` (which composes `InputControlMixin`) plus `field-base-styles` and
`input-field-shared-styles`.

*Rationale.* Gives label, helper text, error message, required indicator, `allowedCharPattern`
filtering, autofocus and `--vaadin-input-field-*` styling with the same behaviour and markup contract
as the built-in fields — including working under base styles, Aura and Lumo with no theme-specific
CSS from us.

*Rejected.* Hand-rolling the field chrome: more code, and guaranteed drift from built-in fields in
styling and accessibility details. See risk R1.

### D5 — The value is a `String` that may be shorter than `length`

Partial input is a legal value; completion is signalled by a separate `OtpCompleteEvent`.

*Rationale.* Keeps `HasValue`/`Binder` semantics honest: what the user typed is the value, and "not
finished yet" is a *constraint violation*, not a hidden value. Applications that only care about the
finished code use `addCompleteListener` instead of the `if (value.length() == n)` check sketched in
the issue.

*Rejected.* Keeping the value empty until the code is complete — it silently discards user input,
breaks `isEmpty()`/`clear()` semantics, and makes an incomplete-code error message impossible.

### D6 — Character restriction through `HasAllowedCharPattern` alone

`setAllowedCharPattern(String)`, the standard Vaadin mixin, is the only way to restrict characters.
`OtpField.NUMERIC` (`[0-9]`, the default) and `OtpField.ALPHANUMERIC` (`[0-9A-Za-z]`) are public
constants for the two common patterns; anything else, hexadecimal included, is written out as a
single-character regular expression.

*Rationale.* `allowedCharPattern` is the existing Vaadin idiom for exactly this
(`com.vaadin.flow.component.shared.HasAllowedCharPattern`), so we inherit its semantics and
client-side filtering. An enum over it would add a second, parallel way to say the same thing, and
its extra value — `inputmode` and case conversion — is better said directly by the two setters that
own those properties.

`setAllowedCharPattern` **assigns** the default `inputmode` (`numeric` for `NUMERIC`, empty
otherwise) rather than tracking whether the application had chosen one, so the ordering is explicit:
call `setInputMode` after `setAllowedCharPattern`. Case conversion is independent of the pattern and
is never reassigned by it. Note also that the keydown filter matches `allowedCharPattern` against
the raw key, before conversion, so a custom pattern combined with a conversion must accept both
cases.

*Rejected.* `setOnlyNumeric(boolean)` from the issue — see §12. Also rejected: an `OtpCharacterSet`
enum bundling pattern, input mode and case conversion, for the reason given above.

### D7 — Default `length` is 6

Six digits is the dominant convention for TOTP and SMS codes. The issue's example uses 4, which stays
one call away.

## 6. Terminology

- **Slot** — the visual box for a single character position. Presentational only (D3).
- **Active slot** — the slot at the caret position; carries the focus affordance.
- **Complete** — the value length equals `length`.
- **Incomplete** — the value is non-empty and shorter than `length`.

## 7. Functional requirements

Requirement IDs are referenced by the acceptance criteria (§17) and the test plan (§15.5).

### 7.1 Structure and configuration

- **FR-1** The field renders exactly `length` slots. `length` is settable at any time; changing it
  truncates the value if needed (firing a value change) and re-renders.
- **FR-2** `length` must be in `1..24`; anything else throws `IllegalArgumentException`.
- **FR-3** An allowed character pattern (D6) restricts what can be typed or pasted. Disallowed
  characters are rejected silently — never inserted, never shown.
- **FR-4** Masked mode renders a mask glyph (default `•`) in filled slots instead of the character.
  The value is unaffected.
- **FR-5** Case conversion (`NONE`, `UPPERCASE`, `LOWERCASE`) applies to typed, pasted and
  programmatically set values, so the server-side value and the display always agree.
- **FR-6** A placeholder may be shown in empty slots. If the string is 1 character long it is used
  for every empty slot; if it is exactly `length` characters long, character *i* is used for slot
  *i*; any other length throws `IllegalArgumentException`.
- **FR-7** Label, helper text, error message, required indicator and tooltip behave as on built-in
  Vaadin input fields.

### 7.2 Keyboard interaction

- **FR-8** Typing an allowed character fills the active slot and advances the active slot by one.
- **FR-9** Typing when all slots are filled and the caret is at the end has no effect.
- **FR-10** <kbd>Backspace</kbd> clears the character before the caret and moves the active slot
  back; on an empty field it does nothing.
- **FR-11** <kbd>Delete</kbd> clears the character at the caret without moving back.
- **FR-12** <kbd>Left</kbd>/<kbd>Right</kbd> move the active slot; <kbd>Home</kbd>/<kbd>End</kbd>
  jump to the first/last position; <kbd>Ctrl/Cmd</kbd>+<kbd>A</kbd> selects the whole code.
- **FR-13** Clicking or tapping a slot places the caret at that position. Clicking past the last
  filled slot places the caret after the last character, so a code never has holes in the middle.
  For a mouse the caret must be placed on *press*: `click` and `pointerup` arrive only when the
  button is released, which can be after the user has begun typing, and correcting the caret then
  moves it away from the character just entered.
- **FR-14** <kbd>Tab</kbd> moves out of the whole component, never between slots.
- **FR-15** <kbd>Enter</kbd> is not swallowed, so a surrounding form or shortcut still works.

### 7.3 Paste, drop and autofill

- **FR-16** Pasting distributes characters over the slots starting at the caret, dropping disallowed
  characters and applying case conversion. Whitespace and common separators (`-`, en dash, `.`,
  non-breaking space) are stripped before filling.
- **FR-17** Pasted characters beyond `length` are discarded.
- **FR-18** Text dropped on the field is handled like a paste.
- **FR-19** The input carries `autocomplete="one-time-code"` so browsers and password managers can
  fill SMS/authenticator codes; filling this way produces exactly one value change and, when the code
  is complete, one complete event.
- **FR-20** On mobile the input sets `inputmode` from the allowed character pattern (`numeric` for
  `NUMERIC`, otherwise empty, and overridable with `setInputMode`), plus `autocapitalize`, `autocorrect="off"` and `spellcheck="false"`.

### 7.4 Value and events

- **FR-21** `getValue()` never returns `null`; the empty value is `""`.
- **FR-22** `setValue(String)` applies case conversion and accepts the result. A value longer than
  `length`, or containing disallowed characters, is *not* rejected: it is reported by the
  **invalid** constraint (FR-26) instead. Throwing would propagate out of `Binder.readBean`/
  `setBean` whenever a stored code no longer fits the configuration of the field, which is how the
  built-in Vaadin fields avoid handling an out-of-range value. `null` is still rejected.
- **FR-23** `ValueChangeEvent` follows `ValueChangeMode`, defaulting to `EAGER` (each keystroke), and
  supports `ON_CHANGE`, `LAZY` and `TIMEOUT` for applications that would rather not send partial
  codes on every keystroke.
- **FR-24** `OtpCompleteEvent` fires when the value becomes complete, regardless of
  `ValueChangeMode`, and carries the value. It fires again if the value becomes incomplete and then
  complete once more. It does **not** fire for a programmatic `setValue`.
- **FR-25** `clear()` empties the field and returns the caret to the first slot.

### 7.5 Validation

- **FR-26** Built-in constraints: **required** (empty while `requiredIndicatorVisible`),
  **incomplete** (non-empty and shorter than `length`) and **invalid** (longer than `length`, or
  containing disallowed characters; only reachable through `setValue`, see FR-22).
- **FR-27** `getDefaultValidator()` reports both, so a `Binder` picks them up automatically;
  `setManualValidation(true)` hands control to the application, as on built-in fields.
- **FR-28** Error messages for the built-in constraints come from `OtpFieldI18n` and are overridable
  per instance.
- **FR-29** In the invalid state every slot gets the error styling, not only the active one, and the
  error message is announced.

### 7.6 States

- **FR-30** Read-only: value visible, no caret, not editable, matching built-in field behaviour.
- **FR-31** Disabled: not focusable, dimmed, no client-side interaction.
- **FR-32** `focus()`/`blur()` and `setTabIndex` work; `setAutofocus(true)` focuses the field on
  attach.

## 8. Client-side component

Custom element `<fc-otp-field>`, TypeScript + Lit, one module:
`META-INF/frontend/fc-otp-field/otp-field.ts`.

### 8.1 Structure

```
<fc-otp-field>
  <label slot="label">                              light DOM, created by field-base
  <div slot="helper">                               light DOM, created by field-base
  <div slot="error-message">                        light DOM, created by field-base
  <input slot="input">                              the single real input (D3), transparent text
  #shadow-root
    <div part="label"><slot name="label"></slot>...</div>
    <div part="field">
      <slot name="prefix"></slot>
      <div class="slots-container">
        <div part="slots" aria-hidden="true">
          <div part="slot" filled active>1</div>
          <div part="slot"></div> ...
        </div>
        <slot name="input"></slot>                  the input, layered over the slots
      </div>
      <slot name="suffix"></slot>
    </div>
    <div part="helper-text">...</div>               (from field-base)
    <div part="error-message">...</div>             (from field-base)
```

The real input is layered over the slots with transparent text and caret, so all editing, selection,
IME and autofill behaviour is native while the visible characters are painted by the slots. Slot
state is expressed as attributes (`filled`, `active`, `selected`, `invalid`, `masked`) for styling.

*As built.* The input lives in the **light DOM**, created by `field-base`'s `InputController`, and
is projected into the shadow root through `<slot name="input">` — the same arrangement as every
built-in Vaadin field. D4 requires the mixins, and the mixins own the input; a shadow-root `<input>`
would have meant re-implementing the label, helper and error controllers. The `part` names in §13
are unaffected.

### 8.2 Properties

| Property | Type | Default | Notes |
|---|---|---|---|
| `value` | `string` | `''` | May be shorter than `length` (D5) |
| `length` | `number` | `6` | Number of slots |
| `masked` | `boolean` | `false` | Render the mask glyph instead of the character |
| `maskGlyph` | `string` | `'•'` | Single character |
| `placeholder` | `string` | `''` | 1 char, or exactly `length` chars (FR-6) |
| `allowedCharPattern` | `string` | `'[0-9]'` | Single-character regex, `InputControlMixin` semantics |
| `caseConversion` | `'none' \| 'upper' \| 'lower'` | `'none'` | |
| `inputMode` | `string` | `'numeric'` | Assigned from the allowed character pattern, then overridable |
| `label`, `helperText`, `errorMessage`, `invalid`, `required`, `readonly`, `disabled`, `autofocus` | | | From `InputFieldMixin` |
| ~~`i18n`~~ | | | **Not implemented.** The element runs with `manualValidation` set by Flow and the server owns the invalid state and the message, exactly as `TextField` does in Vaadin 25; mirroring the messages would duplicate the platform model for a case that is not a goal (see NG4). `OtpFieldI18n` is server-side only |

### 8.3 Events

| Event | When |
|---|---|
| `value-changed` | Value property change (Flow syncs this) |
| `otp-complete` | Value became complete; `detail.value` carries the code |
| `change` | Native semantics: on commit (blur or completion) |
| `validated` | Client-side constraint validation ran; `detail.valid` |

## 9. Java API

Package `com.flowingcode.vaadin.addons.otpfield`.

```java
@Tag("fc-otp-field")
@JsModule("./fc-otp-field/otp-field.ts")
public class OtpField extends AbstractSinglePropertyField<OtpField, String>
    implements InputField<AbstractField.ComponentValueChangeEvent<OtpField, String>, String>,
               HasAllowedCharPattern,
               HasValidationProperties,
               HasValidator<String>,
               HasValueChangeMode,
               HasAriaLabel,
               HasPrefix, HasSuffix,
               Focusable<OtpField> {
```

`AbstractSinglePropertyField` provides `Binder` integration, empty-value semantics and `value`
property synchronization. `InputField` (Vaadin 25, `com.vaadin.flow.component.shared`) pulls in
`HasEnabled`, `HasHelper`, `HasLabel`, `HasSize`, `HasStyle`, `HasTooltip` and `HasValue` in a single
declaration.

This replaces the starter placeholder currently in `OtpField.java` (a `Div` wrapping
`@polymer/paper-input`), whose `@Tag`, `@JsModule` and `@NpmPackage` annotations and Polymer
dependency all go away.

### 9.1 Constructors

```java
new OtpField();
new OtpField(int length);
new OtpField(String label);
new OtpField(String label, int length);
new OtpField(String label, ValueChangeListener<ComponentValueChangeEvent<OtpField, String>> listener);
```

### 9.2 Methods

| Method | Description |
|---|---|
| `setLength(int)` / `getLength()` | Number of slots, `1..24`, default 6 (FR-1, FR-2) |
| `setAllowedCharPattern(String)` / `getAllowedCharPattern()` | Single-character regex; from `HasAllowedCharPattern`, overridden to assign the default `inputmode` and refresh `autocapitalize` (D6) |
| `setInputMode(String)` / `getInputMode()` | `numeric`, `text` or empty; call after `setAllowedCharPattern` (D6) |
| `setMasked(boolean)` / `isMasked()` | FR-4 |
| `setMaskGlyph(String)` | Single character, default `•` |
| `setCaseConversion(OtpCaseConversion)` / `getCaseConversion()` | FR-5 |
| `setPlaceholder(String)` / `getPlaceholder()` | FR-6 |
| `setValue(String)` / `getValue()` | FR-21, FR-22 |
| `addCompleteListener(ComponentEventListener<OtpCompleteEvent>)` | FR-24 |
| `setValueChangeMode(ValueChangeMode)` | Default `EAGER` (FR-23) |
| `setRequiredIndicatorVisible(boolean)` | Drives the required constraint (FR-26) |
| `setI18n(OtpFieldI18n)` / `getI18n()` | FR-28 |
| `validate()` (**protected**, as on `TextField`), `setManualValidation(boolean)`, `getDefaultValidator()` | FR-27 |
| `setInvalid(boolean)`, `setErrorMessage(String)` | From `HasValidationProperties` |
| `setAutofocus(boolean)`, `focus()`, `blur()` | FR-32 |
| `setReadOnly(boolean)`, `setEnabled(boolean)` | FR-30, FR-31 |
| `setAriaLabel(String)` / `setAriaLabelledBy(String)` | §11 |
| `getMaskGlyph()` | Added for symmetry with `setMaskGlyph` |
| `setPrefixComponent(Component)` / `setSuffixComponent(Component)` | From `HasPrefix`/`HasSuffix` |
| `MIN_LENGTH`, `MAX_LENGTH`, `DEFAULT_LENGTH`, `DEFAULT_MASK_GLYPH`, `NUMERIC`, `ALPHANUMERIC` | Public constants, so callers need not repeat the bounds or the two common character patterns |

### 9.3 Supporting types

- `enum OtpCaseConversion { NONE, UPPERCASE, LOWERCASE }`
- `class OtpFieldI18n` — `requiredErrorMessage`, `incompleteErrorMessage`, `invalidErrorMessage`
  (FR-22/FR-26), with fluent setters, mirroring `TextField.TextFieldI18n` in Vaadin 25. Unlike the
  platform's, all three carry an English default, so an unconfigured field never shows an empty
  error
- `class OtpCompleteEvent extends ComponentEvent<OtpField>` — `getValue()`

Lombok is already available in the build and is expected for new add-ons; use it for the i18n and
event boilerplate where it reads well.

*As built.* Lombok is used for `OtpCompleteEvent`'s getter only. `OtpFieldI18n` is written out by
hand: javadoc runs without annotation processing, so Lombok-generated accessors would be missing
from the Directory javadoc jar, which is not acceptable for documented public API.

### 9.4 Usage

```java
OtpField otp = new OtpField("Verification code", 6);
otp.addCompleteListener(e -> verifyCode(e.getValue()));
```

```java
OtpField token = new OtpField("Security token", 8);
token.setAllowedCharPattern(OtpField.ALPHANUMERIC);
token.setCaseConversion(OtpCaseConversion.UPPERCASE);
token.setMasked(true);
token.setPlaceholder("_");

binder.forField(token)
    .asRequired("Please enter the code")
    .bind(User::getOtpCode, User::setOtpCode);
```

## 10. Validation and Binder integration

The field follows the Vaadin 25 constraint-validation model used by the built-in input fields:

1. Client-side constraint state is computed in the element and mirrored to the server.
2. `getDefaultValidator()` returns a validator covering *required* and *incomplete* (FR-26), built
   with the public `com.vaadin.flow.component.shared.ValidationUtil` helpers, so `Binder` reports
   those errors before any application-level validator runs.
3. `setManualValidation(true)` suppresses the field's own invalid-state management for applications
   that drive validation themselves.
4. `setInvalid`/`setErrorMessage` (from `HasValidationProperties`) stay available for server-driven
   errors — the typical "code is wrong" case after a back-end check:

```java
otp.addCompleteListener(e -> {
    if (!service.verify(e.getValue())) {
        otp.setErrorMessage("Incorrect code");
        otp.setInvalid(true);
        otp.clear();
        otp.focus();
    }
});
```

Only *incomplete* and *required* are built in. Checking a code against a back end is the
application's job (NG1).

## 11. Accessibility — withdrawn

Accessibility is no longer a requirement of this specification. Nothing was removed from the
component: it is built on `field-base`, so the label, helper and error wiring, the single focusable
input and the `aria-hidden` slot container come with the mixins and would have to be actively
dismantled to lose. What is gone is the *obligation* — no screen-reader pass gates the release, and
no target is set for contrast.

The observable parts that other requirements still cover are FR-14 (the component is one tab stop),
FR-19 (`autocomplete="one-time-code"`) and FR-20 (`inputmode`, `autocapitalize`, `autocorrect`,
`spellcheck`). `OtpFieldAccessibilityIT` is kept: it guards those three and the DOM shape that D3
depends on.

## 12. Deviations from the API sketched in the issue

| Issue sketch | Specified instead | Why |
|---|---|---|
| `setOnlyNumeric(true)` | `setAllowedCharPattern(OtpField.NUMERIC)`, and any other single-character regex for custom sets | Numeric is already the default, so the boolean would rarely be called, and it does not extend to the alphanumeric/hex cases the same issue asks for. `allowedCharPattern` is the established Vaadin mixin for character filtering. |
| `implements HasValue<OtpField, String>` | `extends AbstractSinglePropertyField<OtpField, String> implements InputField<...>` | `HasValue<E extends ValueChangeEvent<V>, V>` is parameterized by the *event* type, so `HasValue<OtpField, String>` does not compile. Extending `AbstractSinglePropertyField` also brings value/property sync, empty-value handling and `Binder` support for free. |
| "stylable via Lumo CSS variables" | Base styles + shared `--vaadin-input-field-*` + our own `--fc-otp-field-*` properties | In Vaadin 25 components render with *base styles* by default, Aura was added and Material removed. Hardcoding `--lumo-*` would leave base-style and Aura users with an unstyled field. Lumo stays fully supported through the shared properties. |
| `setPlaceholder("_")` | Same call, semantics pinned in FR-6 | "One char repeated per slot" versus "one char per position" is ambiguous and both are useful, so the length of the string selects the behaviour. |
| `ValueChangeEvent` "fired when the full code is completed or changed" | Value change on change (FR-23) plus a separate `OtpCompleteEvent` (FR-24) | Keeps `HasValue` semantics standard and removes the `if (value.length() == n)` check from application code. |

## 13. Theming and styling API

- The element consumes the shared `--vaadin-input-field-*` properties, so it picks up base, Aura and
  Lumo styling with no theme-specific code (D4).
- Own style properties, all prefixed `--fc-otp-field-`: `slot-width`, `slot-height`, `slot-gap`,
  `slot-background`, `slot-border-width`, `slot-border-color`, `slot-border-radius`,
  `slot-font-size`, `slot-color`, `caret-color`, plus the `-active` and `-invalid` variants of the
  border and background properties.
- Shadow parts: `label`, `field`, `slots`, `slot`, `input`, `helper-text`, `error-message`. Slot state
  attributes available for styling: `filled`, `active`, `invalid`, `masked`.
- The README documents every style property with a worked example (one compact variant and one
  large "banking" variant), and the demo ships a themed instance.

## 14. Security and privacy

- Masking renders mask glyphs rather than using `type="password"`, because a password input
  suppresses `one-time-code` autofill in several browsers. Masking is therefore a shoulder-surfing
  measure only, and the README says so.
- The component never logs or echoes the value; error messages never include it.
- Codes travel over the standard Flow request channel. Applications that prefer not to transmit
  partial codes can set `ValueChangeMode.ON_CHANGE`, which leaves the complete event intact — this
  trade-off is documented.
- The component never writes to the clipboard.
- Verification, attempt limits, expiry and lockout are explicitly the application's responsibility
  (NG1), stated in the README so nobody mistakes the field for a security control.

## 15. Repository, build and testing

### 15.1 Current state

This repository was created from **AddonStarter24** and adapted by `/adapt-addon-starter`
(pom coordinates, README, issue templates, license headers, Java package rename). Work is on the
`initial-implementation` branch. The component itself is still the starter placeholder.

```
pom.xml            com.flowingcode.vaadin.addons:otp-field-addon:1.0.0-SNAPSHOT
                   vaadin.version 24.9.1 / Java 17, with an opt-in `v25` profile (25.0.3 / Java 21)
                   commons-demo 5.4.0, Lombok 1.18.40, jetty-maven-plugin 11.0.20
.github/workflows/ maven.yml (builds Vaadin 24 *and* 25), commits.yml
src/main/java/com/flowingcode/vaadin/addons/otpfield/OtpField.java   placeholder (paper-input Div)
src/main/resources/META-INF/frontend/styles/static_addon_styles
src/main/resources/META-INF/VAADIN/package.properties
src/test/java/com/flowingcode/vaadin/addons/otpfield/       DemoView, OtpFieldDemo, OtpFieldDemoView
src/test/java/com/flowingcode/vaadin/addons/otpfield/it/    AbstractViewTest, ViewIT
src/test/java/com/flowingcode/vaadin/addons/otpfield/test/  SerializationTest
```

### 15.2 Repository adaptation required by this spec

D2 (Vaadin 25 only) conflicts with the starter's Vaadin 24 defaults. Before or alongside M1:

1. **pom** — set `vaadin.version` to 25.2.6 and `maven.compiler.source`/`target` to 21 in
   the main `<properties>`; fold what the `v25` profile adds (`vaadin-dev`,
   `jakarta.servlet-api 6.1.0`) into the main build and drop the profile.
2. **CI** — reduce `maven.yml` to the single Vaadin 25 job; drop `build-vaadin24`. Keep `commits.yml`.
3. **Demo container** — `jetty.version` is 11.0.20 and the `v25` profile does not override it.
   Confirm `mvn jetty:run` actually serves the demo on Vaadin 25 and move to the Jetty 12 EE11 plugin
   if it does not.
4. **Component** — replace the `paper-input` placeholder in `OtpField.java` per §9 and remove the
   Polymer `@NpmPackage`.
5. **Frontend** — add `META-INF/frontend/fc-otp-field/otp-field.ts` next to the existing
   `styles/static_addon_styles` placeholder. No extra toolchain config is expected (Vaadin's frontend
   build compiles add-on `.ts` sources, as in `XTermConsoleAddon`) — verify on the first build.
6. **README** — the online-demo link points at `addonsv24.flowingcode.com/otpfield`; repoint it at the
   Vaadin 25 demo host and fill in the placeholder Features and Getting-started sections.

Items 1–3 are a decision about the repo's shape, not just chores: if the team would rather keep the
starter's 24 + 25 dual build, that reopens D2.

### 15.3 Spec lifecycle

Per `AddonsInternal/spec-branch-workflow.md`, this document may either evolve on a dedicated
specification branch with its own review PRs, or — the "organic alternative" — travel in the feature
branch and merge together with the implementation. Given a single add-on with one implementer, the
organic path is the lighter fit; the choice is §18.8.

### 15.4 Demo

One demo view per theme of this spec, so reviewers can exercise each requirement. These extend the
existing `OtpFieldDemoView`/`DemoView` scaffolding on `commons-demo`, and each carries `@DemoSource`
so its code shows beside it.

*As built*, eight tabs: **Basic** (6 digits, live value and complete event); **Configuration**
(length, allowed characters, case conversion, masking, placeholder, read-only, enabled); **Custom
characters** (`allowedCharPattern`, the both-cases caveat, custom mask glyph); **Prefix and suffix**
(prefix/suffix components, tooltip, `setAriaLabel`, `focus()`); **Value change modes** (EAGER vs
ON_CHANGE vs LAZY, each counting the events the server receives); **Binder** (required, incomplete
and invalid errors, plus loading a stored code that no longer fits); **Verification** (server-side
"incorrect code" handling); **Theming**.

### 15.5 Tests

- **Unit tests** (plain JUnit 4, *not* `UIUnitTest`): length change truncation (FR-1), `setLength`
  bounds (FR-2), `setValue` acceptance and the invalid constraint (FR-22), value change modes (FR-23),
  complete event semantics (FR-24),
  `clear()` (FR-25), required/incomplete constraints and `Binder` round-trip (FR-26 to FR-28),
  read-only and disabled (FR-30, FR-31), i18n messages. `UIUnitTest` was not used: the browserless
  framework (`vaadin-testbench-unit-junit5` 10.1.x) requires Vaadin 25.1.2 or newer and is absent
  from the 25.0 BOM, so it cannot span the CI matrix. Nothing in the list above needs a UI.
  `PublicApiTest` additionally pins the API-shape decisions that are source incompatible to reverse.
- **Integration tests** (TestBench, extending the existing `AbstractViewTest`) with a page object
  `OtpFieldElement` exposing `type`, `paste`, `getSlotTexts`, `getActiveSlotIndex`, `isSlotFilled`:
  typing and advance (FR-8, FR-9), backspace and delete (FR-10, FR-11), caret navigation (FR-12,
  FR-13), tab behaviour (FR-14), paste distribution and filtering (FR-16 to FR-18), masking (FR-4),
  case conversion (FR-5), placeholder (FR-6), invalid styling (FR-29), prefix and suffix layout,
  the DOM contract behind FR-14/FR-19/FR-20 (`OtpFieldAccessibilityIT`), and an **axe-core scan** of
  the demo (`OtpFieldAxeIT`). The axe scan is run for the signal it gives, not as a release gate:
  §11 is withdrawn, so a finding is information rather than a failed obligation.
- **`SerializationTest`** — already scaffolded; extend to the real component.
- **Not verified.** FR-19 (autofill on iOS Safari and Android Chrome) and FR-20 (on-screen keyboard
  type) cannot be driven from a desktop Chrome harness, and no manual matrix gates the release. They
  rest on the client-side implementation alone.
- CI runs unit and integration tests on Chrome against the single supported platform version,
  taken from the pom rather than overridden per job.

## 16. Risks

| # | Risk | Mitigation |
|---|---|---|
| **R1** | `@vaadin/field-base` (D4) is published and used by Vaadin's own components, but is not a documented public API, so mixin internals may shift between platform minors. It also has to be present in the application's bundle. | Depend only on the mixin surface the built-in fields use; declare the npm dependency explicitly and keep it aligned with the supported platform version — the `@NpmPackage` versions track what `vaadin-core-versions.json` pins, which for 25.2.6 is npm 25.2.7, not the platform number. Fallback: implement the small subset we need (label/helper/error controllers, `allowedCharPattern`) directly on top of the documented `--vaadin-input-field-*` properties. |
| **R2** | Layering a transparent input over painted slots (D3) is sensitive to font metrics, zoom and letter-spacing; misalignment shows up as a caret in the wrong slot. | Derive slot geometry and caret position from measured character advance rather than assumed widths; cover zoom levels and a proportional-font override in the visual tests. |
| **R3** | `one-time-code` autofill behaviour differs across browsers and cannot be tested in CI. | Keep the single-input design, which is the best-supported shape, and cover it in the manual matrix (§15.5) every release. |
| **R4** | The starter's Vaadin 24 defaults (§15.2) silently diverge from D2, so a `mvn package` without `-Pv25` would build against the wrong platform. | Flip the defaults early, in the same PR as the first real implementation commit. |

## 17. Acceptance criteria

Status as of spec version 0.2. Unticked items are the release work that remains.

- [x] All requirements in §7 implemented, each covered by at least one automated test (§15.5),
      except FR-19 and FR-20, which a desktop harness cannot reach.
- [x] Repository adaptation items in §15.2 completed. Jetty 11.0.26 was confirmed to serve the demo
      on Vaadin 25, so the move to the Jetty 12 EE11 plugin was not needed.
- [x] Public API as specified in §9, javadoc'd, with `@since` tags (class level, all types being new
      in 1.0.0).
- [x] `Binder` integration verified for required, incomplete and application-level validators, and
      for a stored value that no longer fits the field.
- [ ] Field renders correctly under base styles, Aura and Lumo, in light and dark. **Verified under
      Lumo light only**, by screenshot; base styles, Aura and dark are unverified.
- [x] Documented style properties and parts (§13), exercised by the demo.
- [x] README covering usage, full API, styling and the security notes from §14, with the placeholder
      Features and Getting-started sections replaced.
- [x] CI green on both matrix legs; Apache 2.0 headers present; the `directory` profile produces a
      valid Directory bundle (jar, sources, javadoc with `failOnWarnings`).

## 18. Open questions

Every recommendation below was **implemented as recommended**. Implementing one is not the same as
signing off on it, so they stay open until reviewed; the "shown by" column says where a reviewer can
see the consequence for themselves.

| # | Question | Built as | Shown by |
|---|---|---|---|
| 1 | **D3** — single input behind slots, or one per slot? | Single input | Not shown directly. The consequences are visible — one tab stop, <kbd>Ctrl</kbd>+<kbd>A</kbd> selecting the whole code, a paste distributing over the slots — but no tab states the design. **Still the decision most in need of sign-off** |
| 2 | **D2** vs the starter's dual build | Flipped to Vaadin 25 only | Not a demo question: `pom.xml` and the CI matrix |
| 3 | **D5** — partial value observable through `getValue()`? | Yes | **Basic**, which prints `Value: "12"` as you type, and **Value change modes**, which prints the value carried by each event |
| 4 | **FR-26** — incomplete invalid on its own, or only when required? | On its own | **Basic**, which is not required: blur a partial code and it reports "The code is incomplete". Covered by `OtpFieldValidationIT` |
| 5 | **Default `length`** — 6 or 4? | 6 | Six tabs construct the field without a length and render six slots |
| 6 | **Grouping and separators** — v1 or future? | Future; not implemented | **Custom characters** shows the related half: pasting `12-34` strips the separator rather than rendering it |
| 7 | Element tag `<fc-otp-field>` | Confirmed | **Theming**, whose CSS selects `fc-otp-field.otp-compact` and `fc-otp-field::part(slot)` |
| 8 | **Spec lifecycle (§15.3)** | Organic: this document travels with the implementation and is updated alongside it | This section |

## 19. Future work (explicitly out of v1)

- Grouped slots with separators.
- Theme variants (small/large, centred) via `HasThemeVariant`.
- A resend/countdown companion component, or a composed "verify code" form block.
- Hilla/React binding for the element.
- Paste-from-clipboard button and an "SMS received" hint using the WebOTP API.
- A Vaadin 24 back-port, if customer demand appears.

## 20. Milestones

| Milestone | Content |
|---|---|
| **M1** | Repository adaptation (§15.2); element (D3, D4) plus Flow wrapper and a basic demo; typing, backspace, paste and masking working. |
| **M2** | Validation, i18n, events and `Binder` integration; UI unit tests. |
| **M3** | Accessibility pass, integration tests, README and demo completion. |

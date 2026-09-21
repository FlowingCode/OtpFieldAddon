[![Published on Vaadin Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/otp-field-add-on)
[![Stars on vaadin.com/directory](https://img.shields.io/vaadin-directory/star/otp-field-add-on.svg)](https://vaadin.com/directory/component/otp-field-add-on)
[![Build Status](https://jenkins.flowingcode.com/job/OtpField-addon/badge/icon)](https://jenkins.flowingcode.com/job/OtpField-addon)
[![Maven Central](https://img.shields.io/maven-central/v/com.flowingcode.vaadin.addons/otp-field-addon)](https://mvnrepository.com/artifact/com.flowingcode.vaadin.addons/otp-field-addon)
[![Javadoc](https://img.shields.io/badge/javadoc-00b4f0)](https://javadoc.flowingcode.com/artifact/com.flowingcode.vaadin.addons/otp-field-addon)

# OTP Field Add-On

Field for entering one-time passwords and verification codes

## Features

* One visual slot per character, with the active slot following the caret.
* Complete keyboard, paste, drop and `autocomplete="one-time-code"` autofill behaviour, with no
  application-side glue code.
* Configurable length (1&ndash;24, default 6), accepted characters through `allowedCharPattern`
  (numeric by default), case conversion, per-slot placeholder and masking.
* `Binder` integration with built-in *required* and *incomplete* constraints, and an
  `OtpCompleteEvent` that fires when the code reaches its full length.
* Accessible: one tab stop, announced as a single field, and usable with a screen reader.
* Themeable through documented CSS custom properties and shadow parts, under base styles, Aura and
  Lumo.

## Online demo

[Online demo here](http://addonsv25.flowingcode.com/otpfield)

## Download release

[Available in Vaadin Directory](https://vaadin.com/directory/component/otp-field-add-on)

### Maven install

The add-on requires **Vaadin 25** and **Java 21**.

Add the following dependencies in your pom.xml file:

```xml
<dependency>
   <groupId>com.flowingcode.vaadin.addons</groupId>
   <artifactId>otp-field-addon</artifactId>
   <version>X.Y.Z</version>
</dependency>
```
<!-- the above dependency should be updated with latest released version information -->

Release versions are available from Maven Central repository. For SNAPSHOT versions see [here](https://maven.flowingcode.com/snapshots/).

## Building and running demo

- git clone repository
- mvn clean install jetty:run

To see the demo, navigate to http://localhost:8080/

## Release notes

See [here](https://github.com/FlowingCode/OtpFieldAddon/releases)

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome. There are two primary ways you can contribute: by reporting issues or by submitting code changes through pull requests. To ensure a smooth and effective process for everyone, please follow the guidelines below for the type of contribution you are making.

#### 1. Reporting Bugs and Requesting Features

Creating an issue is a highly valuable contribution. If you've found a bug or have an idea for a new feature, this is the place to start.

* Before creating an issue, please check the existing issues to see if your topic is already being discussed.
* If not, create a new issue, choosing the right option: "Bug Report" or "Feature Request". Try to keep the scope minimal but as detailed as possible.

> **A Note on Bug Reports**
> 
> Please complete all the requested fields to the best of your ability. Each piece of information, like the environment versions and a clear description, helps us understand the context of the issue.
> 
> While all details are important, the **[minimal, reproducible example](https://stackoverflow.com/help/minimal-reproducible-example)** is the most critical part of your report. It's essential because it removes ambiguity and allows our team to observe the problem firsthand, exactly as you are experiencing it.

#### 2. Contributing Code via Pull Requests

As a first step, please refer to our [Development Conventions](https://github.com/FlowingCode/DevelopmentConventions) page to find information about Conventional Commits & Code Style requirements.

Then, follow these steps for creating a contribution:
 
- Fork this project.
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- For commit message, use [Conventional Commits](https://github.com/FlowingCode/DevelopmentConventions/blob/main/conventional-commits.md) to describe your change.
- Send a pull request for the original project.
- Comment on the original issue that you have implemented a fix for it.

## License & Author

This add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

OTP Field Add-On is written by Flowing Code S.A.

# Developer Guide

## Getting started

`OtpField` is a Vaadin Flow input field for a fixed-length verification code. It renders one slot
per character and behaves like any other input field, so it works with `Binder` out of the box.

```java
OtpField otp = new OtpField("Verification code", 6);
otp.addCompleteListener(e -> verifyCode(e.getValue()));
add(otp);
```

The value is a `String` that may be **shorter** than the length: what the user typed is always the
value, and "not finished yet" is a constraint violation rather than a hidden value. Applications
that only care about the finished code use `addCompleteListener` instead of checking the length on
every value change.

The complete event fires again whenever the user edits a complete code into a different one, whether
by replacing one of its characters, by pasting another code over it, or by emptying it and typing a
new one, so a code corrected after a failed verification is verified again.

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

### Length and accepted characters

`setLength(int)` takes 1&ndash;24 and defaults to 6, the dominant convention for TOTP and SMS
codes. Shortening the length truncates the value, which fires a value change event.

The accepted characters are the standard Vaadin `setAllowedCharPattern(String)`, a
single-character regular expression. `OtpField.NUMERIC` (`[0-9]`, the default) and
`OtpField.ALPHANUMERIC` (`[0-9A-Za-z]`) are provided as constants; anything else is written out:

```java
otp.setAllowedCharPattern("[0-9A-Fa-f]");
```

The pattern also assigns the `inputmode`, which decides the on-screen keyboard mobile browsers
show: `NUMERIC` implies `numeric`, and every other pattern leaves it empty. `setInputMode(String)`
states a different one, and because the pattern assigns the default, call it *after*
`setAllowedCharPattern`.

Note that the pattern also filters what the user may type, *before* case conversion is applied. If
you combine a custom pattern with `UPPERCASE` or `LOWERCASE`, let the pattern accept both cases
(`[0-9A-Fa-f]` rather than `[0-9A-F]`), so that typing a lower-case letter is converted instead of
rejected.

Disallowed characters are dropped silently. On paste and drop, whitespace and the separators
commonly used to group a printed code (hyphen, en dash, `.`, `_`, `/`, non-breaking space) are
dropped too, so pasting `123-456` fills the code with `123456`.

### Placeholder and masking

`setPlaceholder(String)` accepts a one-character placeholder, which is repeated in every empty slot,
or a placeholder that is exactly `getLength()` characters long, which provides one character per
slot:

```java
otp.setPlaceholder("_");        // _ _ _ _ _ _
otp.setPlaceholder("YYMMDD");   // Y Y M M D D
```

Any other length throws `IllegalArgumentException`. Set the placeholder after the length, because a
multi-character placeholder that no longer matches the length is not rendered.

`setMasked(true)` renders a mask glyph (a bullet by default, see `setMaskGlyph(String)`) instead of
the characters. The value is unaffected.

### Value change mode

The value change mode defaults to `ValueChangeMode.EAGER`, so the server sees every keystroke.
Applications that would rather not transmit partial codes can switch to
`ValueChangeMode.ON_CHANGE`, which sends the value when the code is committed &mdash; on blur, or
when it becomes complete. The complete event is fired in either case.

```java
otp.setValueChangeMode(ValueChangeMode.ON_CHANGE);
```

### Validation

The field has three built-in constraints:

* **required** &mdash; the value is empty while `setRequiredIndicatorVisible(true)`
* **incomplete** &mdash; the value is non-empty and shorter than `getLength()`
* **invalid** &mdash; the value is longer than `getLength()`, or contains characters the field does
  not accept

`getDefaultValidator()` reports all three, so a `Binder` picks them up automatically. On its own,
the field does not flag an incomplete code while the user is still typing it: it validates when the
code is committed, and afterwards on every keystroke until it is valid again.

A user cannot type or paste a value that violates the third constraint, so it reports a value that
was set programmatically &mdash; typically a stored code that no longer fits the length or the
allowed character pattern the field is configured with. `setValue` accepts such a value and lets the validator
report it, rather than throwing, so that loading a bean with `Binder` never fails outright.

Error messages come from `OtpFieldI18n` and are overridable per instance:

```java
otp.setI18n(new OtpFieldI18n()
    .setRequiredErrorMessage("Please enter the code")
    .setIncompleteErrorMessage("The code has 6 digits")
    .setInvalidErrorMessage("This code cannot be entered here"));
```

Verifying a code against a back end is the application's job. Report the outcome with
`setErrorMessage`/`setInvalid`:

```java
otp.addCompleteListener(e -> {
    if (!service.verify(e.getValue())) {
        // Empty the field first: clear() revalidates the constraints, which would otherwise
        // reset the error message set below.
        otp.clear();
        otp.focus();
        otp.setErrorMessage("Incorrect code");
        otp.setInvalid(true);
    }
});
```

For fully custom validation logic, `setManualValidation(true)` hands the invalid state over to the
application, exactly as on the built-in Vaadin fields.

### Keyboard, paste and autofill

Everything below is handled client side, without a server round trip:

| Interaction | Behaviour |
|---|---|
| Typing | Fills the active slot and advances; typing past the last slot has no effect |
| <kbd>Backspace</kbd> | Clears the character before the caret and moves back |
| <kbd>Delete</kbd> | Clears the character at the caret |
| <kbd>&larr;</kbd> <kbd>&rarr;</kbd> <kbd>Home</kbd> <kbd>End</kbd> | Move the active slot |
| <kbd>Ctrl/Cmd</kbd>+<kbd>A</kbd>, double click | Select the whole code |
| Click or tap | Places the caret at that slot, never past the last character |
| <kbd>Tab</kbd> | Moves out of the whole component; the field is one tab stop |
| <kbd>Enter</kbd> | Not swallowed, so a surrounding form or shortcut still works |
| Paste, drop | Distributed over the slots from the caret, filtered and case-converted |

The field carries `autocomplete="one-time-code"`, so browsers and password managers fill SMS and
authenticator codes into it. Filling that way produces exactly one value change and, when the code
is complete, one complete event.

### Styling

The field is built on the same shared field styling as the built-in Vaadin input fields, so it picks
up base styles, Aura and Lumo with no theme-specific CSS. On top of the shared
`--vaadin-input-field-*` properties, it exposes its own:

| Custom property | Default | Description |
|---|---|---|
| `--fc-otp-field-slot-width` | `2.25em` | Width of a slot |
| `--fc-otp-field-slot-height` | `2.5em` | Height of a slot |
| `--fc-otp-field-slot-gap` | `--vaadin-gap-s` | Space between slots |
| `--fc-otp-field-slot-background` | `--vaadin-input-field-background` | Slot background |
| `--fc-otp-field-slot-border-width` | `--vaadin-input-field-border-width` | Slot border width |
| `--fc-otp-field-slot-border-color` | `--vaadin-border-color` | Slot border colour |
| `--fc-otp-field-slot-border-radius` | `--vaadin-input-field-border-radius` | Slot corner radius |
| `--fc-otp-field-slot-font-size` | `--vaadin-input-field-value-font-size` | Slot font size |
| `--fc-otp-field-slot-color` | `--vaadin-input-field-value-color` | Character colour |
| `--fc-otp-field-caret-color` | slot colour | Caret in the active slot |
| `--fc-otp-field-slot-background-active` | slot background | Background of the active slot |
| `--fc-otp-field-slot-border-color-active` | `--vaadin-focus-ring-color` | Border of the active slot |
| `--fc-otp-field-slot-border-width-active` | `--vaadin-focus-ring-width` | Border width of the active slot |
| `--fc-otp-field-slot-background-invalid` | slot background | Background while invalid |
| `--fc-otp-field-slot-border-color-invalid` | `--vaadin-input-field-error-color` | Border while invalid |
| `--fc-otp-field-slot-border-width-invalid` | slot border width | Border width while invalid |

Shadow parts: `label`, `field`, `slots`, `slot`, `helper-text`, `error-message`,
`required-indicator`. A slot carries the state attributes `filled`, `active`, `selected`, `masked`
and `invalid`.

A compact variant:

```css
fc-otp-field.otp-compact {
  --fc-otp-field-slot-width: 1.75em;
  --fc-otp-field-slot-height: 2em;
  --fc-otp-field-slot-gap: 0.15em;
  --fc-otp-field-slot-border-radius: 0.15em;
  --fc-otp-field-slot-font-size: 0.9em;
}
```

A large "banking token" variant:

```css
fc-otp-field.otp-banking {
  --fc-otp-field-slot-width: 2.75rem;
  --fc-otp-field-slot-height: 3.5rem;
  --fc-otp-field-slot-gap: 0.5rem;
  --fc-otp-field-slot-border-radius: 0.5rem;
  --fc-otp-field-slot-border-width: 2px;
  --fc-otp-field-slot-font-size: 1.5rem;
  --fc-otp-field-slot-background: #0b1b2b;
  --fc-otp-field-slot-border-color: #244056;
  --fc-otp-field-slot-color: #e8f1f8;
  --fc-otp-field-slot-border-color-active: #4ea8ff;
  --fc-otp-field-caret-color: #4ea8ff;
}

fc-otp-field.otp-banking::part(slot) {
  font-family: ui-monospace, "SFMono-Regular", "Consolas", monospace;
}
```

### Accessibility

The field is a single accessible node: one real `<input>` is the only focusable element and the slot
container is `aria-hidden`, so a screen reader announces the label once rather than "edit, 1 of 6"
six times over. Label association, helper-text `aria-describedby` and the error-message live region
come from the shared field controllers, so they behave exactly as on the built-in fields. The whole
component is one tab stop, the active slot shows a focus affordance, and its caret respects
`prefers-reduced-motion`.

### Security notes

**The field is an input control, not a security control.** It does not generate, send, verify,
expire or rate-limit codes: TOTP/HOTP, SMS delivery, retry limits and lockout are entirely the
application's responsibility.

* **Masking is a shoulder-surfing measure only.** It renders mask glyphs rather than using
  `type="password"`, because a password input suppresses `one-time-code` autofill in several
  browsers. The value is not hidden from assistive technology any more than a password field's is.
* **Codes travel over the standard Flow request channel.** With the default
  `ValueChangeMode.EAGER`, every keystroke is transmitted. Applications that prefer not to send
  partial codes can use `ValueChangeMode.ON_CHANGE`, which leaves the complete event intact.
* The component never logs or echoes the value, error messages never include it, and it never writes
  to the clipboard.

## Special configuration when using Spring

By default, Vaadin Flow only includes `com/vaadin/flow/component` to be always scanned for UI components and views. For this reason, the add-on might need to be allowed in order to display correctly. 

To do so, just add `com.flowingcode` to the `vaadin.allowed-packages` property in `src/main/resources/application.properties`, like:

```
vaadin.allowed-packages = com.vaadin,org.vaadin,dev.hilla,com.flowingcode
```
 
More information on Spring scanning configuration [here](https://vaadin.com/docs/latest/integrations/spring/configuration/#configure-the-scanning-of-packages).

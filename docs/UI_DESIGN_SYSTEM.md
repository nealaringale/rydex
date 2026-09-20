# RYDEX UI Design System

This document records the UI rules used by the RYDEX Android client so later screens stay visually consistent.

## 1. Design basis

RYDEX follows Android's adaptive-app guidance rather than hard-coding separate phone and tablet screens.

Primary references:
- Android adaptive layouts: https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes
- Android window size classes: https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes
- Android adaptive Compose guide: https://developer.android.com/develop/ui/compose/build-adaptive-apps
- Material accessibility guidance: https://m1.material.io/usability/accessibility.html
- Google Maps Android controls: https://developers.google.com/maps/documentation/android-sdk/controls
- Google Maps Android configuration and map padding: https://developers.google.com/maps/documentation/android-sdk/configure-map

The important layout breakpoints used by RYDEX mirror the documented width classes:
- Compact: < 600dp
- Medium: 600dp to < 840dp
- Expanded: >= 840dp

Height is also considered for short landscape windows. In particular, a landscape phone often has a medium width but compact height, so RYDEX keeps the navigation cockpit intentionally dense instead of trying to display a full two-pane planning page.

## 2. Layout behavior

### Compact / portrait
Use one vertical content flow, 18dp horizontal page padding, 14dp section spacing, and vertically scrollable content.

### Medium / landscape
Use a two-pane layout when the task benefits from simultaneous context:
- Planning: form on the left, live ride status on the right.
- Preferences: controls on the left, route profile summary on the right.
- Trip preview: summary/map on the left, stops/weather on the right.
- Live ride: map occupies the majority of the width and the navigation context sits in a side panel.

### Expanded
Keep the same information architecture as medium, but constrain content width so large displays do not create excessively long reading lines.

### Live ride
The map is always the visual background. In portrait, navigation context is a bottom panel. In landscape, it becomes a persistent right-side panel so the rider can see the map and next instruction simultaneously.

Google Maps controls are deliberately reduced to the interactions RYDEX needs. A custom location button is provided for the rider, while zoom controls, compass and map toolbar are hidden.

## 3. Color system

RYDEX uses a restrained dark palette with one high-energy cyan brand accent.

Semantic roles:
- Background: near-black blue-gray
- Surface: dark neutral card
- Surface high: slightly lighter elevated card
- Primary: bright cyan for primary actions and active status
- Secondary: soft indigo for secondary emphasis
- Success: mint/green for food or positive route status
- Warning: warm yellow for fuel and caution
- Info: blue for weather/information
- Error: red only for actual errors

Text is not encoded entirely through accent colors. Primary information uses the on-surface color, while secondary copy uses a stronger neutral gray chosen to remain readable on the dark surfaces.

## 4. Contrast and visibility

For normal body text, target a minimum 4.5:1 contrast ratio against its background. Large text may use 3:1, but RYDEX still prefers stronger contrast where practical.

Important states use multiple cues, not color alone:
- icon + label + color for fuel/food/weather
- selected state uses label text such as “Included”
- GPS state uses icon + status text

Do not introduce low-opacity gray text for important information.

## 5. Typography

Use Material 3 typography and reserve the large display style for the page hero. Prefer:
- display/headline for the screen's main idea
- title styles for card and section headings
- label styles for metadata/eyebrows
- body styles for explanations

Keep labels concise and avoid large blocks of all-caps text. The only all-caps treatment used in RYDEX is small eyebrow metadata with letter spacing.

## 6. Spacing and shape

Use an 8dp-based rhythm:
- 8dp: tight grouping
- 12dp: component spacing
- 14dp: section/card spacing
- 18dp: compact page padding
- 20–24dp: wide-screen page padding
- 18dp corner radius for standard cards
- 14–15dp corner radius for primary buttons and controls

Avoid excessive shadows. Separation is primarily created through surface tone, outline, spacing and hierarchy.

## 7. Touch targets

Interactive controls should provide at least a 48dp touch target. The visual icon can remain smaller than 48dp while the surrounding interaction area remains comfortably tappable.

## 8. Riding-specific rules

The cockpit must prioritize glanceability:
1. Next instruction
2. Current speed
3. Route/destination
4. Relevant stop or weather context
5. Secondary controls

The map remains the largest visual area. Do not cover the map with multiple unrelated floating cards.

Do not add animation or decorative effects that compete with navigation information.

## 9. Future screens

New RYDEX screens should:
- consume MaterialTheme color roles instead of hard-coded text colors
- use the same responsive breakpoints
- preserve state through orientation/window-size changes
- keep primary actions at least 48dp high
- provide content descriptions for meaningful icons
- use text, iconography and shape in addition to color for important states

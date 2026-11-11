---
name: Precision Lens & Document Studio
colors:
  surface: '#fff8f7'
  surface-dim: '#f6d2cd'
  surface-bright: '#fff8f7'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff0ee'
  surface-container: '#ffe9e6'
  surface-container-high: '#ffe2de'
  surface-container-highest: '#ffdad5'
  on-surface: '#2a1614'
  on-surface-variant: '#5e3f3a'
  inverse-surface: '#412b28'
  inverse-on-surface: '#ffedea'
  outline: '#936e69'
  outline-variant: '#e8bcb6'
  surface-tint: '#c00003'
  primary: '#bc0003'
  on-primary: '#ffffff'
  primary-container: '#e71610'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb4a8'
  secondary: '#af2f22'
  on-secondary: '#ffffff'
  secondary-container: '#fe6855'
  on-secondary-container: '#690001'
  tertiary: '#0058bc'
  on-tertiary: '#ffffff'
  tertiary-container: '#0d70e9'
  on-tertiary-container: '#fefcff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad5'
  primary-fixed-dim: '#ffb4a8'
  on-primary-fixed: '#410000'
  on-primary-fixed-variant: '#930002'
  secondary-fixed: '#ffdad5'
  secondary-fixed-dim: '#ffb4a8'
  on-secondary-fixed: '#410000'
  on-secondary-fixed-variant: '#8d160d'
  tertiary-fixed: '#d8e2ff'
  tertiary-fixed-dim: '#adc6ff'
  on-tertiary-fixed: '#001a41'
  on-tertiary-fixed-variant: '#004494'
  background: '#fff8f7'
  on-background: '#2a1614'
  surface-variant: '#ffdad5'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-md-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-compact: 0.75rem
  margin: 1rem
  margin-expanded: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system expresses modern Android Material Design 3 (M3) architecture tailored for high-speed document capture, optical processing, and file management. The system balances utility and utility-led delight: crisp, crystalline canvas surfaces establish an environment of clinical accuracy, while soft red-tinted tonal accents bring freshness, clarity, and tactile responsiveness to daily document handling. 

Contrasting this gentle baseline is a purposeful promotional and conversion layer characterized by deep midnight canvases, razor-sharp crimson geometric accents, and high-energy electric blue primary action triggers. The aesthetic evokes the feel of a professional handheld scanner fused with modern native Android polish: fluid, light, structural, and decisively elevated.

## Colors

The palette leverages Material 3 tonal color mapping, introducing specialized role extensions for scanner utility states and promotional anchors.

- **Primary Tonal System (`#EB1B13`)**: A calibrated vibrant red anchor. Provides the basis for M3 Primary, Primary Container, and on-container relationships. Active chips, navigation bar pill indicators, and soft capture toggles use low-chroma light red tints with high-contrast text.
- **Surface & Canvas Architecture**: Neutral-variant light surfaces establishing pristine document contrast without blinding glare.
- **Conversion & Hero Card Dark Canvas**: Reserved specifically for the premium scanning features and promotional tier, charged with high-energy accents and structural dividers.
- **Accent CTA (`#006DE6`)**: Vibrant electric blue dedicated exclusively to high-conversion triggers, subscription conversion buttons, and high-priority batch export states on dark cards.
- **Scanner States**: Capture target reticles and boundary detection utilize high-luminance cyan `#00E5FF` and validation green `#22C55E` strictly as state indicators.

## Typography

The type system is built on Inter for its structural clarity and numerical legibility across dense data displays (file sizes, page indices, DPI indicators).

- **Weight Hierarchy**: Titles, section headers, and callouts utilize Semibold (`600`) and Bold (`700`) weights to provide fast visual anchor points while browsing thumbnail grids. Body text relies on Regular (`400`) to maintain neutral, comfortable reading in file meta lists and document detail panels.
- **Letter Spacing**: Micro-typography (metadata, chips, badges, and scanner status counters) enforces a positive tracking of `0.4px` to `0.5px` to preserve crisp scanning metrics on high-density AMOLED and OLED screens.
- **Display Restraint**: The large display tier is reserved solely for document page counts within hero preview panes and full-screen onboarding moments.

## Layout & Spacing

Layouts follow Material 3 responsive mobile conventions based on an 8dp (0.5rem) spatial cadence with 4dp (0.25rem) sub-steps for micro-alignments.

- **Handheld Grid Architecture**: Mobile phone screens default to a 4-column layout with 16dp (`1rem`) outer margins and 16dp (`1rem`) gutters between document grid tiles. 
- **Compact & Split Adaptations**: In two-page comparison view or split-sheet processing drawers, gutters reduce to 12dp (`0.75rem`) to ensure preview thumbnails maintain aspect ratio integrity.
- **Floating Action Zones**: Document creation actions (Floating Action Button and Shutter) maintain a 24dp (`1.5rem`) buffer from the screen bottom or are docked centrally above the M3 Navigation Bar with a 16dp (`1rem`) clearance.
- **Vertical Flow**: Section groupings (Recent Scans, Cloud Storage Folders, Batch Tools) enforce `space-lg` (24dp) separation, while items inside collections retain `space-sm` (8dp) to maintain visual association.

## Elevation & Depth

Visual hierarchy uses Material 3 surface tinting and ambient dual-layer shadows to convey depth without heavy skeuomorphic effects.

- **M3 Tonal Surface Layering**: Elevation is primarily articulated via surface tone rather than raw shadow strength. Higher elevation tiers shift the surface container toward a faint red-tinted neutral.
- **Ambient Soft Drop Shadows**: Reserved strictly for actionable floating elements (FAB, elevated bottom bar, preview overlays). Shadows use a low-opacity primary-tinted blur combined with a diffuse ambient shadow, creating soft separation from background grids.
- **Promotional Contrast Box**: The dark promotional card uses zero elevation shadow, relying instead on its deep dark fill and an inner glowing hairline border to establish immediate foreground priority over surrounding light cards.

## Shapes

The design system implements native Material 3 corner geometry, balancing structural cards with circular interactive touch points.

- **Corners & Radii Hierarchy**:
  - `rounded-2xl` (16dp / 1rem): Standard document cards, scan item previews, dark promotional banners, bottom sheets, and dialog surfaces.
  - `rounded-lg` (12dp / 0.75rem): Text fields, segmented controls, contextual menus, and preview actions.
  - `rounded-full` (Pill / 9999px): All chips (filter, sort, tag), bottom navigation active indicator capsules, and main FAB controls.
- **Touch Targets**: All rounded interactive elements strictly preserve minimum touch bounds of 48dp x 48dp, even when visual badge shapes are rendered as compact 32dp pill forms.

## Components

### Buttons
- **Primary Action (Standard)**: Pill-shaped (`rounded-full`), primary red background with crisp white text. Min height 48dp.
- **Promotional CTA (Blue)**: Embedded inside dark promotional surfaces. Pill-shaped (`rounded-full`), `#006DE6` background with white text, bold typography (`label-lg`). Hover/pressed state drops to a deeper blue shade.
- **Tonal / Secondary Action**: Light red container with red text. Zero shadow, subtle tactile ripple.

### Chips (Filter, Sort, Status)
- **Container**: `rounded-full` geometry, 32dp height, padding horizontal 16dp.
- **Inactive State**: Outlined with low-contrast neutral border, text `#49454E`.
- **Active State**: Light red fill, no outline, deep red text, accompanied by a leading checkmark or indicator dot.

### Material 3 Navigation Bar
- **Bar Surface**: Surface Container Low, 80dp height, docked bottom.
- **Active Indicator**: Horizontal pill shape (64dp width, 32dp height) in light red accent.
- **Icons & Labels**: Active icon rendered in primary red, inactive icons in `#79747E`. Labels styled in `label-sm`.

### Dark Promotional Card
- **Surface**: `rounded-2xl` (16dp), deep canvas, internal padding 20dp.
- **Accent Details**: Diagonal top-right vector badge with neon blue (`#006DE6`) geometric glow line. Subtitles in muted silver (`#C4C7C5`).
- **Integrated Action**: Includes right-aligned or full-width vibrant blue pill button (`#006DE6`).

### Document & File Cards
- **Structure**: Surface Container Low, `rounded-2xl` (16dp), subtle elevation (Level 1).
- **Layout**: Top thumbnail view with 4:3 or 16:9 aspect ratio clipped with 12dp inner radius; bottom metadata row with document title (`title-sm`), page count, and overflow action icon.

### Text Inputs & Search Fields
- **Search Header**: Docked or floating M3 Search Bar, `rounded-full`, Surface Container High, leading search lens icon, trailing profile or voice action.
- **Form Inputs**: Filled text field variant with rounded top corners (8dp) and active indicator line tinted to the primary red color.

### Checkboxes & Multi-Select
- **Toggle Shape**: Rounded square (4dp corner radius), checked fill in primary red with white checkmark. Selection ring animations adopt a 200ms spring curve matching native Android 14 transitions.
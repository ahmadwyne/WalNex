# Walnex UI Design System — “E‑Wallet Concept / Bento Style” (Android, Material)

**Applies to:** Walnex Android app (Java)  
**Style goal:** Dark-mode-first **E‑Wallet Concept, Bento style** as in designs **[image1–image4]**  
**Source screens provided:**  
- **[image4]** Home/Dashboard (balance card + quick actions + recent transactions + bottom nav)  
- **[image2]** Statistics screen (bento tiles for spendings/incomes)  
- **[image3]** Onboarding/Intro screen (hero cards + “Get started” gradient pill button)  
- **[image1]** Style reference collage (“E‑Wallet Concept Bento style”)

This doc tells the coding agent exactly how the UI should look and behave: colors, backgrounds, buttons, spacing, typography, component rules, and UX/HCI guidelines.

---

## 1) Visual Identity Summary (what the UI should feel like)
Walnex should feel:
- **Premium, minimal, high-contrast** (dark surfaces + soft elevation)
- **Bento-grid** content layout (rounded “tiles” grouped in clean rows)
- **Friendly gradients** as highlights (purple→blue, subtle)
- **Large numeric hierarchy** (balance and money amounts are the focal point)
- **Soft shadows / depth** (cards appear floating above a dark background)

---

## 2) Global Theme: Dark-first “Soft Black” Surfaces
### 2.1 Background
From the screens, the app uses a very dark charcoal/near-black background with subtle vertical gradient.

**Recommended tokens**
- `bg/base`: `#0B0D10` (near-black)
- `bg/elevated`: `#12151B` (slightly lighter)
- `bg/surface`: `#161A21` (card base)
- `bg/surface2`: `#1B2029` (tile base)
- `divider`: `#242A36` (very subtle)

**Gradient background (optional)**
- Top: `#0B0D10`
- Bottom: `#090A0D`

**Rule:** The background must never become pure black `#000000` everywhere; use “soft black” to reduce eye strain.

---

## 3) Color System (based on provided designs)
### 3.1 Brand accent gradient (primary)
The designs consistently use a **purple → blue** gradient for primary highlights and the main CTA button.

**Primary gradient**
- Start (purple): `#9B7CFF`
- End (blue): `#5BB7FF`

Use this gradient for:
- Primary CTA buttons (“Get started”, “Confirm”)
- The circular center action in bottom navigation (if implemented)
- Subtle highlights on key cards

### 3.2 Text colors
- `text/primary`: `#FFFFFF`
- `text/secondary`: `#B9C0CC` (subtitles, helper text)
- `text/muted`: `#7B8494` (timestamps, minor labels)
- Link/action text (like “View chart”, “See all”): `#59AFFF` (blue accent)

### 3.3 Semantic money colors
From [image2] & [image4], money is color-coded:
- **Income / positive**: green
  - `success`: `#22C55E` (or slightly deeper `#1ED760`)
- **Expense / negative**: red
  - `danger`: `#FF4D4D` (or `#FF3B30`-ish)
- Neutral amounts: `text/primary`

**Rules**
- Red = outgoing / spending
- Green = incoming / income
- Do not rely on color alone—also show context labels (“Salary”, “Food”) and +/- sign conventions.

---

## 4) Bento Layout Rules (core of the style)
Bento style means content is displayed in **rounded tiles** in a grid-like arrangement with consistent spacing.

### 4.1 Tile shape & depth
- Corner radius:
  - Large cards: **24dp**
  - Standard tiles: **18–20dp**
  - Buttons (pill): **28–32dp** (fully rounded)
- Elevation:
  - Keep subtle (dark UI). Prefer **soft shadow** + slightly lighter surface over heavy drop shadow.
- Stroke (optional):
  - 1dp border in `divider` color can replace shadow for cleaner look.

### 4.2 Spacing system
Use consistent spacing to match the clean layouts:
- Screen padding: **20–24dp**
- Tile gap: **12–16dp**
- Section vertical spacing: **20–24dp**
- Inside tile padding: **16–20dp**

### 4.3 Grid patterns (examples from designs)
- Stats screen [image2]:
  - Row of **2 tiles** (Top spendings)
  - Then a **wide tile** (Salary)
  - Then **2 tiles** (Loan, Savings)
- Dashboard [image4]:
  - 1 wide balance card
  - Row of 4 quick action icons (circular buttons + labels)
  - Recent transaction list

---

## 5) Typography (hierarchy like the designs)
### 5.1 Font
- Use Android default (Roboto) or a clean geometric sans (if later).
- Bold weight for numeric emphasis.

### 5.2 Sizes (recommended)
- Screen title (e.g., “Statistics”): **22–24sp**, semibold
- Section title (“Top spendings”, “Recent transactions”): **18–20sp**, semibold
- Balance number: **34–40sp**, bold
- Tile money amount: **22–26sp**, bold
- Labels (Food/Loan/Salary): **14–16sp**
- Meta (date, helper text): **12–13sp**

**Rules**
- Numbers must be the strongest visual element.
- Use monospaced numerals if possible (improves readability of balances).

---

## 6) Components Spec (what to build)

### 6.1 Primary button (CTA) — Gradient pill (from [image3])
**Visual**
- Full-width pill button with purple→blue gradient
- Height: **52–56dp**
- Radius: **28dp+**
- Text: white, 16sp semibold
- Shadow: subtle or none; rely on gradient

**States**
- Default: full gradient
- Pressed: darken gradient by ~8–12%
- Disabled: grayscale surface with 40–60% opacity text

**Usage**
- “Get started”
- “Confirm transfer”
- “Continue”

### 6.2 Bento tile cards (from [image2])
**Visual**
- Dark rounded rectangle tile
- Label top-left (white/secondary)
- Amount big and colored red/green
- Optional right-side mini action (“View chart”)

**Interaction**
- Whole tile tappable if it navigates to details
- Touch target padding >= 16dp

### 6.3 Balance card (from [image4])
**Visual**
- Large rounded card with **soft gradient** background (purple/blue with a glow)
- Contains:
  - “Your balance” label + eye icon (toggle hide/show)
  - Large balance value

**Recommended background**
- Gradient overlay: purple/blue
- Add a subtle blurred “orb” or radial gradient effect (optional) to mimic the design.

**Eye toggle behavior**
- Default: show
- Hidden: replace digits with `•••••` but keep currency symbol / formatting consistent.

### 6.4 Quick actions row (from [image4])
Four circular icon buttons:
- `Top up`
- `Transfer`
- `Request` (optional in PRD; can keep placeholder)
- `More`

**Visual**
- Circular button background: `bg/surface2` (slightly lighter than base)
- Icon: white (or accent)
- Label under icon: muted text

**Touch target**
- Minimum 48dp hit area per button
- Provide ripple feedback

### 6.5 Transaction list item (from [image4])
Each row:
- Left: colored circular category icon
- Middle: title (Food/Loan/Salary) + date
- Right: amount colored red/green

**Rules**
- Amount alignment: right aligned
- Use consistent decimal formatting
- Use minus sign for spending if appropriate (e.g., `-24.00`)

### 6.6 Bottom navigation (from [image4])
- 5 icons with a standout center action (optional)
- Selected icon: accent or white
- Unselected: muted
- Center FAB-like action: gradient circle (purple→blue)

**HCI**
- Ensure labels or tooltips if icons are ambiguous.
- Keep bottom nav height standard; avoid tiny tap targets.

### 6.7 Onboarding hero (from [image3])
- Dark background
- Floating “cards” illustration
- Main title big: “Manage your wallet”
- Short supportive subtitle
- Page indicator: 3 dots with active purple segment
- Bottom CTA: gradient pill “Get started”

---

## 7) Iconography & Illustration Rules
- Use simple line/filled icons with consistent stroke weight.
- Category icons in transactions should be:
  - High-contrast (white icon on colored circle)
  - Colors can be pastel variants that still pop on dark background.

---

## 8) Motion & Micro-interactions (should feel premium)
- Screen transitions: subtle (Material shared axis or fade-through).
- Button press: scale down slightly (0.98) + ripple.
- Loading:
  - Use skeleton shimmer on bento tiles (subtle).
- Transfer processing:
  - Full-screen or modal progress state preventing double submit.

---

## 9) Accessibility & HCI (Human–Computer Interaction) Rules
These are mandatory for a wallet-like UX.

### 9.1 Visibility of system status
- Always show clear feedback on:
  - OTP sending / verifying
  - transfer “processing / completed / failed”
  - network offline states (banner/snackbar)

### 9.2 Error prevention (wallet critical)
- Confirm step before money moves:
  - recipient phone (masked)
  - amount
  - fee (if any; MVP likely none)
- Disable confirm during processing.
- Idempotency is backend enforced; UI must still guard double taps.

### 9.3 Recognition over recall
- Use clear labels (“Transfer”, “Top up”).
- Keep consistent placement for balance, actions, notifications bell.

### 9.4 Consistency & standards
- Use consistent tile spacing, radii, icon style.
- Use standard Material components where possible, themed to match.

### 9.5 Minimalist design
- Avoid heavy outlines and loud colors.
- Use accent colors only for:
  - primary actions
  - important numbers
  - status (green/red)

### 9.6 Accessibility (WCAG-ish)
- Contrast: text must be readable on dark surfaces.
- Support dynamic type (font scaling).
- Touch targets: minimum 48dp.
- Provide content descriptions for icons (TalkBack).
- Don’t rely only on color to communicate debit vs credit.

### 9.7 Privacy UX
- Balance hide/show toggle (eye icon).
- Mask phone numbers in receipts and lists where appropriate.

---

## 10) Screen-by-screen mapping to Walnex features

### Dashboard / Home ([image4])
- Balance card (gradient)
- Quick actions row
- Recent transactions list
- Bottom nav + notification bell

### Statistics ([image2]) (optional MVP screen)
- Bento tiles for spendings/incomes
- “View chart” action (link style)
> If not in MVP, reuse this layout later for analytics.

### Onboarding ([image3])
- Intro carousel or single screen
- CTA “Get started”

---

## 11) Implementation guidance (Android Material)
- Use Material 3 (recommended) but style it to match dark bento look:
  - Custom `ColorScheme` with dark surfaces
  - Custom shapes (rounded corners)
- Use:
  - `MaterialCardView` / `ShapeableImageView` for tiles
  - Gradient drawables for CTA buttons and balance cards
  - `BottomNavigationView` with custom center action (optional)

**Important:** Keep the UI spec consistent with backend constraints:
- Transfers must show “Processing” and not assume immediate success until function returns.

---

## 12) Design Do’s / Don’ts (quick checklist)
### Do
- Use dark soft surfaces + consistent rounded tiles
- Use purple→blue gradient for primary actions
- Use green/red for income/expense
- Keep numbers large and readable
- Provide clear confirmations for transfers

### Don’t
- Don’t let clients “fake” updated balances before server confirmation
- Don’t use tiny tap targets or low-contrast gray text
- Don’t overload screens with too many colors or borders
- Don’t reveal full phone numbers everywhere (mask in lists/receipts)

---
**End of UI Design System**
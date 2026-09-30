# GIIC Asset Management UI/UX Design Guide

> **Version:** 1.0  
> **Last Updated:** February 3, 2026  
> **Application:** GIIC Asset Management System

This document captures the complete design system used across the GIIC Asset Management application, extracted from the current CSS implementation.

---

## 1. Color Palette

### Primary Colors (Blues)
Used for primary actions, links, and brand identity.

| Color Name | Hex Code | Usage | CSS Variable |
|------------|----------|-------|--------------|
| Primary Blue | `#3B82F6` | Primary buttons, links, focus states | `--button-blue`, `--gradient-blue` |
| Dark Blue | `#1e40af` | Logo text, active nav items | - |
| Hover Blue | `#2563eb` | Button hover states | - |
| Light Blue BG | `#eff6ff` | Active navigation background | - |
| Sky Blue | `#0EA5E9` | Gradient accents | `--gradient-cyan` |

### Secondary Colors
Used for gradients and decorative elements.

| Color Name | Hex Code | Usage | CSS Variable |
|------------|----------|-------|--------------|
| Purple | `#6B5B95` | Gradient backgrounds | `--gradient-purple` |
| Violet | `#8B5CF6` | Gradient accents | `--gradient-violet` |
| Deep Purple | `#667eea` | OCR section gradient start | - |
| Purple End | `#764ba2` | OCR section gradient end | - |

### Status Colors

#### Success (Green)
| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Success Text | `#10b981` | Available status text |
| Success BG | `#ecfdf5` | Available status background |

#### Warning (Yellow/Orange)
| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Warning Text | `#f59e0b` | Maintenance status text |
| Warning BG | `#fffbeb` | Maintenance status background |

#### Error (Red)
| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Error Primary | `#ef4444` | Error text, lost status |
| Error Dark | `#dc2626` | Hover states, danger actions |
| Error BG Light | `#fef2f2` | Error message background |
| Error BG | `#fee2e2` | Danger button background |
| Error Border | `#fecaca` | Error borders, danger button borders |

### Neutral Colors (Grays)

| Color Name | Hex Code | Usage | CSS Variable |
|------------|----------|-------|--------------|
| Text Primary | `#1e293b` | Main text, headings | `--text-primary` |
| Text Body | `#334155` | Table data text | - |
| Text Secondary | `#64748b` | Labels, secondary text | `--text-secondary` |
| Gray 700 | `#374151` | Nav links default | - |
| Gray 600 | `#475569` | Legend items | - |
| Gray 500 | `#6b7280` | Subtitles, disposed status, icons | - |
| Gray 400 | `#94a3b8` | Placeholders, notes | - |
| Gray 300 | `#cbd5e1` | Hover borders, spinner | - |
| Gray 200 | `#e5e7eb` | Button borders | - |
| Border Gray | `#e2e8f0` | Default borders | - |
| Light Border | `#f1f5f9` | Table row borders, dividers | - |

### Background Colors

| Color Name | Hex Code | Usage | CSS Variable |
|------------|----------|-------|--------------|
| Page BG | `#f0f4f8` | Auth page background | `--bg-light` |
| Dashboard BG | `#f5f7fa` | Dashboard background | - |
| White | `#ffffff` | Cards, forms, tables | `--bg-white` |
| Table Header BG | `#f8fafc` | Table headers, input backgrounds | - |
| Hover BG | `#f3f4f6` | Nav hover, table row hover, disposed status BG | - |
| Auto-filled BG | `#f0f4ff` | OCR auto-filled fields | - |

---

## 2. Typography

### Font Families

**Primary Font Stack:**
```css
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
```

This system font stack provides optimal readability across all platforms.

### Font Sizes

| Size | Usage | Context |
|------|-------|---------|
| `28px` | Page headings (H2) | Auth page title, stat values |
| `24px` | Page titles | Asset page headers |
| `20px` | Section titles | OCR section titles |
| `18px` | Widget titles, Logo titles | Dashboard widgets, Form section titles |
| `16px` | Buttons, Large inputs | Primary actions, Auth inputs, OCR buttons |
| `15px` | Organization name | Auth header |
| `14px` | Body text, Labels | Form labels, table data, nav links, inputs |
| `13px` | Small labels, Filter labels | Table headers, auth notes, secondary actions |
| `12px` | Badges, Small text | Status badges, error messages, table headers (uppercase) |
| `11px` | Subtitles, Tiny badges | Logo subtitles, auto-filled badges, mobile status badges |
| `10px` | Dropdown arrows | Navigation dropdowns |

### Font Weights

| Weight | Value | Usage |
|--------|-------|-------|
| Regular | `400` | Default text (inherited) |
| Medium | `500` | Stat labels, auth footer links |
| Semibold | `600` | Auth org name, headings, labels, buttons, nav active, badges |
| Bold | `700` | Page titles, headings, logo text, widget titles |

### Line Heights

| Value | Usage |
|-------|-------|
| `1.2` | Stat values (tight) |
| Default | Body text (browser default ~1.5) |

### Letter Spacing

| Value | Usage |
|-------|-------|
| `-0.5px` | Auth page title (tight) |
| `0.05em` | Table headers (uppercase) |
| `0.5px` | Auth org name (slightly expanded) |

---

## 3. Spacing System

### Padding Values

| Value | Common Usage |
|-------|--------------|
| `3px` | Badge vertical padding (mobile) |
| `4px` | Small badge padding, stat value margin |
| `6px` | Badge padding, filter group gap, legend color size |
| `8px` | Input/button padding, gaps, dropdown menu padding, checkbox margin, label margin, detail border radius |
| `10px` | Button padding, input padding, dropdown item padding, auto-filled badge padding |
| `12px` | Nav link padding, error message padding, checkbox inputs, dropdown legend gap, mobile inputs |
| `14px` | Button/input padding |
| `15px` | Header padding, main content padding (mobile), form container (mobile), OCR section (mobile) |
| `16px` | Input padding, table cell padding, OCR gaps, OCR result badge, detail row padding |
| `20px` | Search filter container, form sections, dropdown items, widget header, OCR upload container, form notes, image upload |
| `24px` | Stat card padding, widget header, chart container, OCR section, page margins |
| `25px` | Auth footer margin, form options margin |
| `30px` | Header padding, main content padding, form container, detail container, detail card |
| `35px` | Auth header margin, auth notes margin |
| `50px` | Auth card padding |
| `60px` | OCR placeholder padding |

### Margin Values

| Value | Common Usage |
|-------|--------------|
| `0` | Reset margins |
| `4px` | Required asterisk margin, dropdown arrow margin, error margin, stat value margin |
| `5px` | Auth footer link margin |
| `8px` | Auth org name margin, checkbox margin, filter label margin, legend gap, component gaps |
| `10px` | Image preview margin, auth notes padding |
| `12px` | Auth logo margin, action button gaps, error padding, legend gaps, dropdown legend |
| `15px` | Auth notes margin, form group margin (mobile), button padding (mobile) |
| `16px` | OCR preview gaps, result badge margin |
| `20px` | Form groups, filter gaps, form section margin, form grid gaps, widget header padding, auth notes padding |
| `24px` | Page header margin, search filter margin, pagination margin, stats grid gap, widgets gap, detail gap |
| `25px` | Auth footer margin, form options margin |
| `30px` | Form sections, form actions, detail gap |
| `32px` | OCR section margin |
| `35px` | Auth header margin, auth notes margin |

### Gap Values

| Value | Usage |
|-------|-------|
| `2px` | Mobile nav gap |
| `5px` | Header nav gap |
| `8px` | Pagination gap, button icon gap, checkbox gap, legend gap, nav link gap |
| `10px` | Image placeholder gap, asset page header gap (mobile) |
| `12px` | Header logo gap, action buttons gap, legend gaps, form actions gap, stat card gap |
| `16px` | Search filter gap, OCR preview gap |
| `20px` | Form groups, form grid, stat card gap, chart container gap (mobile) |
| `24px` | Stats grid gap, dashboard widgets gap |

---

## 4. Border & Radius

### Border Widths

| Width | Usage |
|-------|-------|
| `1px` | Default borders (inputs, buttons, cards, tables) |
| `2px` | Dashed borders, focus rings, auto-filled fields, OCR upload, spinner |
| `6px` | Auth card top gradient stripe |

### Border Radius Values

| Value | Usage | Context |
|-------|-------|---------|
| `4px` | Small elements | Asset thumbnails, legend colors |
| `6px` | Small containers | OCR result badge |
| `8px` | Standard radius | Inputs, buttons, nav links, dropdowns, error messages, form elements, images, pagination, logout button |
| `12px` | Medium cards | Search filter, table container, form container, detail cards, stat icon wrapper, donut chart legend, OCR section, auto-filled badge |
| `16px` | Large cards | Auth card, stat cards, widget cards |
| `50%` | Circles | Donut chart |
| `9999px` | Pills | Status badges |

### Border Colors

| Color | Hex Code | Usage |
|-------|----------|-------|
| Default Border | `#e2e8f0` | Inputs, buttons, forms, tables |
| Light Border | `#f1f5f9` | Table rows, dividers, auth notes |
| Gray Border | `#e5e7eb` | Logout button |
| Hover Border | `#cbd5e1` | Input/button hover |
| Error Border | `#fee2e2` | Error messages |
| Focus Border | `#3B82F6` | Input focus states |
| OCR Border | `rgba(255, 255, 255, 0.4)` | OCR upload container |
| Top Border | `rgba(255, 255, 255, 0.2)` | OCR section title |

---

## 5. Shadows

### Box Shadow Styles

#### Card Shadows
```css
/* Small card shadow (default) */
box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);

/* Medium card shadow (hover) */
box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -2px rgba(0, 0, 0, 0.025);

/* Large shadow (auth card) */
box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
```

#### Dropdown Shadows
```css
/* Dropdown menu */
box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
```

#### Header Shadow
```css
/* Main header */
box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
```

#### Button Shadows
```css
/* Primary button default */
box-shadow: 0 4px 6px rgba(59, 130, 246, 0.2);

/* Primary button hover */
box-shadow: 0 6px 10px rgba(59, 130, 246, 0.3);

/* OCR button */
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

/* OCR button hover */
box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
```

#### OCR Section Shadow
```css
/* OCR section card */
box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);

/* OCR image preview */
box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
```

#### Focus Shadows
```css
/* Input focus ring */
box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);

/* Form input focus */
box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);

/* Auto-filled field */
box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
```

---

## 6. Components

### 6.1 Buttons

#### Primary Button
```css
.btn-primary {
    background: linear-gradient(135deg, #3B82F6, #8B5CF6);
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 8px;
    font-weight: 600;
    font-size: 14px;
    cursor: pointer;
    transition: opacity 0.2s;
    display: inline-flex;
    align-items: center;
    gap: 8px;
}

.btn-primary:hover {
    opacity: 0.9;
}
```

**Usage:** Main call-to-action buttons, asset creation, form submissions

#### Secondary Button
```css
.btn-secondary {
    background-color: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
    padding: 10px 20px;
    border-radius: 8px;
    font-weight: 600;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    display: inline-flex;
    align-items: center;
    gap: 8px;
}

.btn-secondary:hover {
    background-color: #f8fafc;
    color: #1e293b;
    border-color: #cbd5e1;
}
```

**Usage:** Cancel buttons, secondary actions

#### Danger Button
```css
.btn-danger {
    background-color: #fee2e2;
    color: #ef4444;
    border: 1px solid #fecaca;
    padding: 10px 20px;
    border-radius: 8px;
    font-weight: 600;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    display: inline-flex;
    align-items: center;
    gap: 8px;
}

.btn-danger:hover {
    background-color: #fecaca;
    color: #dc2626;
}
```

**Usage:** Delete actions, destructive operations

#### Auth Button
```css
.auth-button {
    width: 100%;
    padding: 14px;
    background-color: #3B82F6;
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 4px 6px rgba(59, 130, 246, 0.2);
}

.auth-button:hover {
    background-color: #2563eb;
    transform: translateY(-2px);
    box-shadow: 0 6px 10px rgba(59, 130, 246, 0.3);
}
```

**Usage:** Login, signup submissions

#### OCR Button
```css
.btn-ocr {
    background: white;
    color: #667eea;
    border: none;
    padding: 14px 32px;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.btn-ocr:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
}
```

**Usage:** OCR image analysis, special feature buttons

#### Logout Button
```css
.logout-button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    background: none;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    cursor: pointer;
    font-size: 18px;
    color: #6b7280;
    transition: all 0.2s ease;
}

.logout-button:hover {
    background: #fee2e2;
    border-color: #fecaca;
    color: #dc2626;
}
```

**Usage:** Header logout action

### 6.2 Inputs

#### Text Input
```css
.form-input, .form-select {
    padding: 10px 14px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    font-size: 14px;
    color: #1e293b;
    outline: none;
    transition: border-color 0.2s;
    min-width: 200px;
}

.form-input:focus, .form-select:focus {
    border-color: #3B82F6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}
```

**Usage:** Form fields, search inputs, filters

#### Auth Input
```css
.form-group input {
    width: 100%;
    padding: 14px 16px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    font-size: 16px;
    color: #1e293b;
    background-color: #f8fafc;
    transition: all 0.2s ease;
}

.form-group input:focus {
    border-color: #3B82F6;
    background-color: #fff;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
```

**Usage:** Login, signup forms

#### Auto-filled Input (OCR)
```css
.form-input.auto-filled {
    border: 2px solid #667eea;
    background-color: #f0f4ff;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
```

**Usage:** Fields auto-populated by OCR

### 6.3 Cards

#### Stat Card
```css
.stat-card {
    background: white;
    padding: 24px;
    border-radius: 16px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 
                0 2px 4px -1px rgba(0, 0, 0, 0.03);
    display: flex;
    align-items: center;
    gap: 20px;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 
                0 4px 6px -2px rgba(0, 0, 0, 0.025);
}
```

**Structure:**
- Icon wrapper: 60x60px, border-radius 12px
- Stat value: 28px, bold (700)
- Stat label: 14px, medium (500), gray color

#### Widget Card
```css
.widget-card {
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 
                0 2px 4px -1px rgba(0, 0, 0, 0.03);
    overflow: hidden;
    height: 100%;
    display: flex;
    flex-direction: column;
}

.widget-header {
    padding: 20px 24px;
    border-bottom: 1px solid #f1f5f9;
}

.widget-title {
    font-size: 18px;
    font-weight: 700;
    color: #1e293b;
}
```

**Usage:** Dashboard charts, recent assets lists

#### Auth Card
```css
.auth-card {
    background-color: white;
    padding: 50px;
    border-radius: 16px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
    width: 100%;
    max-width: 440px;
    position: relative;
    overflow: hidden;
}

/* Gradient top stripe */
.auth-card::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 6px;
    background: linear-gradient(to right, #6B5B95, #3B82F6, #0EA5E9);
}
```

**Features:** Gradient top stripe, centered layout, decorative background

#### Detail Card
```css
.detail-card {
    background-color: white;
    padding: 30px;
    border-radius: 12px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
    height: fit-content;
}
```

**Usage:** Asset detail pages, information display

### 6.4 Tables

#### Asset Table
```css
.asset-table {
    width: 100%;
    border-collapse: collapse;
}

.asset-table th {
    background-color: #f8fafc;
    text-align: left;
    padding: 16px 20px;
    font-size: 13px;
    color: #64748b;
    font-weight: 600;
    border-bottom: 1px solid #e2e8f0;
    white-space: nowrap;
}

.asset-table td {
    padding: 16px 20px;
    font-size: 14px;
    color: #1e293b;
    border-bottom: 1px solid #f1f5f9;
}

.asset-table tr {
    cursor: pointer;
    transition: background-color 0.2s;
}

.asset-table tr:hover {
    background-color: #f8fafc;
}
```

**Features:** Hover states, alternating row colors, responsive overflow

#### Recent Assets Table
```css
.recent-assets-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;
}

.recent-assets-table th {
    text-align: left;
    padding: 12px 24px;
    background: #f8fafc;
    color: #64748b;
    font-weight: 600;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.recent-assets-table td {
    padding: 16px 24px;
    border-bottom: 1px solid #f1f5f9;
    color: #334155;
}
```

**Features:** Uppercase headers, smaller text, compact display

### 6.5 Navigation

#### Header Navigation
```css
.main-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: white;
    padding: 15px 30px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
    position: sticky;
    top: 0;
    z-index: 1000;
}

.nav-link {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 18px;
    background: none;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    color: #374151;
    transition: all 0.2s ease;
}

.nav-link:hover {
    background: #f3f4f6;
    color: #1e40af;
}

.nav-link.active {
    background: #eff6ff;
    color: #1e40af;
    font-weight: 600;
}
```

**Features:** Sticky header, active states, dropdown support

#### Dropdown Menu
```css
.dropdown-menu {
    position: absolute;
    top: 100%;
    left: 0;
    background: white;
    min-width: 160px;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    padding: 8px 0;
    margin-top: 4px;
    animation: fadeIn 0.15s ease;
    z-index: 1001;
}

.dropdown-item {
    display: block;
    padding: 10px 20px;
    color: #374151;
    font-size: 14px;
    transition: all 0.15s ease;
}

.dropdown-item:hover {
    background: #f3f4f6;
    color: #1e40af;
}
```

**Features:** Fade-in animation, hover states, active item highlighting

### 6.6 Badges

#### Status Badges
```css
.status-badge {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
}
```

**Status Variants:**

| Status | Background | Text Color |
|--------|-----------|-----------|
| Available | `#ecfdf5` | `#10b981` |
| Maintenance | `#fffbeb` | `#f59e0b` |
| Disposed | `#f3f4f6` | `#6b7280` |
| Lost | `#fef2f2` | `#ef4444` |

#### Auto-filled Badge
```css
.auto-filled-badge {
    margin-left: 8px;
    background: #667eea;
    color: white;
    padding: 3px 10px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
}
```

**Usage:** Indicates OCR auto-populated fields

### 6.7 Special Components

#### OCR Section
```css
.ocr-section {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 32px;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.ocr-upload-container {
    background: rgba(255, 255, 255, 0.1);
    border: 2px dashed rgba(255, 255, 255, 0.4);
    border-radius: 8px;
    padding: 20px;
}
```

**Features:** Gradient background, white text, dashed upload border, glass-morphism effects

#### Loading Spinner
```css
.spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(102, 126, 234, 0.3);
    border-top-color: #667eea;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
}
```

---

## 7. Layout

### 7.1 Header

**Height:** ~80px (flexible based on content)
**Padding:** `15px 30px`
**Background:** White
**Position:** Sticky, top: 0, z-index: 1000

**Logo:**
- Image: 48x48px
- Title: 18px, bold
- Subtitle: 11px, gray

### 7.2 Main Content

**Max-width:** 1400px
**Padding:** 30px
**Margin:** 0 auto (centered)
**Background:** `#f5f7fa` (Dashboard) or `#f0f4f8` (Auth)

### 7.3 Grid Systems

#### Stats Grid (Dashboard)
```css
.stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 24px;
}
```

**Breakpoints:**
- Desktop (>1024px): 4 columns
- Tablet (768-1024px): 2 columns
- Mobile (<768px): 1 column

#### Dashboard Widgets
```css
.dashboard-widgets {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 24px;
}
```

**Breakpoints:**
- Desktop (>1024px): 2:1 ratio
- Tablet/Mobile (<1024px): 1 column

#### Form Grid
```css
.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
}
```

**Features:** Responsive auto-fit, minimum 250px column width

### 7.4 Detail Container
```css
.detail-container {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 30px;
}
```

**Breakpoints:**
- Desktop (>1024px): 2:1 ratio
- Tablet/Mobile (<1024px): 1 column

---

## 8. Responsive Breakpoints

### 8.1 Desktop
**Range:** > 1024px

**Characteristics:**
- Full navigation with text labels
- 4-column stats grid
- 2:1 widget layout
- Multi-column forms
- Side-by-side detail views

### 8.2 Tablet
**Range:** 768px - 1024px

**Characteristics:**
- Wrapped header navigation
- 2-column stats grid
- Stacked widget layout
- Icon-only navigation (text hidden)
- Form columns adapt

**Header Changes:**
```css
.header-nav {
    order: 3;
    width: 100%;
    margin-top: 15px;
    flex-wrap: wrap;
    justify-content: center;
}

.nav-text {
    display: none;
}

.nav-icon {
    font-size: 20px;
}
```

### 8.3 Mobile
**Range:** < 768px

**Characteristics:**
- Single column layouts
- Smaller text sizes
- Reduced padding
- Full-width buttons
- Simplified header
- Centered dropdowns
- Stacked form elements

**Key Changes:**
```css
/* Logo */
.logo-image {
    width: 36px;
    height: 36px;
}

.logo-title {
    font-size: 14px;
}

.logo-subtitle {
    display: none;
}

/* Content */
.main-content {
    padding: 20px 15px;
}

/* Stats Grid */
.stats-grid {
    grid-template-columns: 1fr;
}

/* Dropdowns */
.dropdown-menu {
    position: fixed;
    left: 50%;
    transform: translateX(-50%);
    min-width: 200px;
}
```

### 8.4 Small Mobile
**Range:** < 480px

**Characteristics:**
- Further reduced text sizes
- Minimal padding
- Compact table cells
- Smaller logos

**Key Changes:**
```css
.page-title {
    font-size: 18px;
}

.asset-table th,
.asset-table td {
    font-size: 11px;
    padding: 6px;
}

.auth-card {
    padding: 20px 15px;
}

.auth-logo {
    width: 80px;
}
```

---

## 9. Animation & Transitions

### 9.1 Transition Durations

| Duration | Usage |
|----------|-------|
| `0.15s` | Fast transitions (dropdown items, quick interactions) |
| `0.2s` | Standard transitions (hover states, focus, most UI elements) |
| `0.3s` | Slow transitions (OCR button, complex animations) |
| `0.8s` | Very slow (spinner rotation) |
| `1.5s` | Infinite pulse animation |

### 9.2 Easing Functions

| Function | Usage |
|----------|-------|
| `ease` | Default easing for most transitions |
| `linear` | Spinner rotation, constant speed animations |

### 9.3 Common Transitions

#### Standard Hover
```css
transition: all 0.2s ease;
```
**Applied to:** Buttons, links, cards, nav items

#### Input Focus
```css
transition: border-color 0.2s;
/* or */
transition: all 0.2s ease;
```

#### Transform Hover
```css
transition: transform 0.2s ease, box-shadow 0.2s ease;
```
**Applied to:** Stat cards, OCR buttons

### 9.4 Keyframe Animations

#### Fade In (Dropdowns)
```css
@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(-8px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* Usage */
animation: fadeIn 0.15s ease;
```

#### Pulse (Loading States)
```css
@keyframes pulse {
    0%, 100% {
        opacity: 0.5;
    }
    50% {
        opacity: 1;
    }
}

/* Usage */
animation: pulse 1.5s infinite;
```

#### Spin (Spinner)
```css
@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

/* Usage */
animation: spin 0.8s linear infinite;
```

### 9.5 Transform Effects

#### Hover Lift
```css
/* Card hover */
transform: translateY(-2px);

/* Button hover */
transform: translateY(-2px);
box-shadow: 0 6px 10px rgba(59, 130, 246, 0.3);
```

#### Dropdown Arrow Rotation
```css
.dropdown-arrow {
    transition: transform 0.2s ease;
}

.has-dropdown.open .dropdown-arrow {
    transform: rotate(180deg);
}
```

---

## 10. Korean UI Text Guidelines

While the CSS files primarily define visual styles, the application includes Korean text for labels, buttons, and interface elements. Here are common patterns observed:

### Common Korean Labels

Based on the application context (Asset Management System), typical Korean UI text includes:

**Navigation & Headers:**
- 자산 관리 (Asset Management)
- 대시보드 (Dashboard)
- 자산 목록 (Asset List)
- 자산 등록 (Asset Registration)

**Asset Fields:**
- 자산명 (Asset Name)
- 자산 번호 (Asset Number)
- 구매일 (Purchase Date)
- 상태 (Status)
- 위치 (Location)
- 담당자 (Manager)

**Status Values:**
- 사용 가능 (Available)
- 유지보수 중 (Under Maintenance)
- 폐기 (Disposed)
- 분실 (Lost)

**Actions:**
- 등록 (Register)
- 수정 (Edit)
- 삭제 (Delete)
- 검색 (Search)
- 취소 (Cancel)
- 저장 (Save)
- 로그인 (Login)
- 로그아웃 (Logout)

### Typography Considerations for Korean

1. **Font Stack:** The system font stack supports Korean characters well:
   ```css
   font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, 
                "Helvetica Neue", Arial, sans-serif;
   ```

2. **Minimum Font Size:** Korean text should not go below 12px for readability

3. **Line Height:** Default line-height works well for Korean text; may need adjustment for dense tables

4. **Letter Spacing:** Korean doesn't need letter-spacing adjustments like English uppercase

---

## Design Principles Summary

### Visual Hierarchy
1. **Primary Blue (#3B82F6)** dominates as the brand color
2. **Gradients** add visual interest to special sections (OCR, auth card)
3. **White cards** on light gray backgrounds create clear content separation
4. **Generous spacing** (24-30px gaps) ensures breathing room
5. **Consistent shadows** provide depth without overwhelming

### User Experience
1. **Hover states** provide clear feedback on interactive elements
2. **Focus states** ensure accessibility with visible rings
3. **Transitions** (0.2s) make interactions feel smooth without lag
4. **Status colors** (green/yellow/red) follow convention
5. **Responsive design** maintains usability across all devices

### Consistency
1. **Border radius** follows 4px/8px/12px/16px scale
2. **Spacing** uses multiples of 4 (4, 8, 12, 16, 20, 24, 30px)
3. **Font sizes** increment logically (11-13-14-16-18-20-24-28px)
4. **Shadows** maintain consistent blur and spread patterns
5. **Colors** from defined palette ensure brand coherence

---

## Usage Guidelines

### When to Use Each Color

**Primary Blue (#3B82F6):**
- Primary call-to-action buttons
- Links and interactive text
- Focus states
- Active navigation items

**Success Green (#10b981):**
- Success messages
- Available status
- Positive confirmations

**Warning Yellow/Orange (#f59e0b):**
- Warning messages
- Maintenance status
- Caution indicators

**Error Red (#ef4444):**
- Error messages
- Destructive actions
- Lost/critical status
- Validation errors

**Neutral Grays:**
- Body text (#334155 - #1e293b)
- Secondary text (#64748b)
- Borders (#e2e8f0)
- Backgrounds (#f5f7fa - #f8fafc)

### Card Component Selection

- **Stat Cards (16px radius):** Dashboard metrics, KPIs
- **Widget Cards (16px radius):** Charts, tables, complex content
- **Detail Cards (12px radius):** Information display, forms
- **Auth Card (16px radius + stripe):** Login, signup, special workflows

### Button Selection

- **Primary:** Main actions (Create, Submit, Confirm)
- **Secondary:** Cancel, Back, Alternative actions
- **Danger:** Delete, Remove, Destructive operations
- **OCR:** Special feature buttons with unique styling

---

## File References

This design guide was extracted from the following CSS files:

- [`Auth.css`](../src/main/giic_frontend/src/styles/Auth.css) - Login/Signup pages (295 lines)
- [`Dashboard.css`](../src/main/giic_frontend/src/styles/Dashboard.css) - Dashboard and navigation (482 lines)
- [`Asset.css`](../src/main/giic_frontend/src/styles/Asset.css) - Asset management pages (603 lines)

**Total CSS analyzed:** 1,380 lines

---

## Maintenance Notes

### When Adding New Components

1. **Check existing patterns** before creating new styles
2. **Use CSS variables** from `:root` where defined
3. **Follow spacing scale** (multiples of 4px)
4. **Apply consistent border radius** (4/8/12/16px)
5. **Use existing color palette** - don't introduce new colors
6. **Add hover states** for interactive elements
7. **Include focus states** for accessibility
8. **Test responsive breakpoints** (768px, 1024px)

### When Updating Colors

1. Update CSS variables in `:root` (Auth.css)
2. Search and replace specific hex values
3. Test contrast ratios for accessibility (WCAG AA minimum)
4. Verify status colors remain intuitive
5. Check gradient combinations

### Future Enhancements

Consider defining CSS variables for:
- Spacing scale
- Border radius values
- Shadow variations
- Transition timings
- Font sizes

This would centralize the design system and make future updates easier.

---

*End of UI/UX Design Guide*

# Stitch MCP — Generating UI Screens for NovaWallet

This document teaches any agent how to generate/edit UI designs for NovaWallet using the **Stitch MCP server**. Read this BEFORE touching Stitch tools so you don't create duplicate projects or lose the design-system context.

---

## 1. Critical: The project ALREADY EXISTS

**Do NOT call `stitch_create_project`.** A NovaWallet design project already exists:

- **Project title:** `NovaWallet`
- **Project ID (resource name):** `projects/11542754846512650236`
- **For tool calls (most tools want the bare ID):** `11542754846512650236`
- **Design system asset referenced as:** `assets/3eff015a7c124ead8b3372f51bc4572c`

There are already ~30 screens on the canvas (home, send, cards, profile, receive, transaction flows, etc.). Generate NEW screens and use the existing project + design system; never recreate.

## Step 0 — Always fetch the project first

Before any generation/edit, call `stitch_get_project` with `{"name": "projects/11542754846512650236"}`.

Why:
1. Confirms the project still exists and its current screen list.
2. Returns the **`designSystem`** field — **pass this exact value** to every generation call so output stays on-brand.

## Step 1 — Generate a new screen

Use `stitch_generate_screen_from_text`:

```json
{
  "projectId": "11542754846512650236",
  "deviceType": "MOBILE",
  "designSystem": "assets/3eff015a7c124ead8b3372f51bc4572c",
  "prompt": "<detailed screen description>"
}
```

**Rules:**
- **Always pass BOTH `projectId` and `designSystem`.** Omitting them → off-brand or orphaned output.
- `deviceType`: `MOBILE` for phone screens (app uses 390px mockups), `DESKTOP` for the web dashboard flows.
- The prompt must describe the layout top-to-bottom, in brand terms (see Step 3). More specific = better.

**Timing:** Generation takes **1–5 minutes**. Don't retry aggressively.

### Timeouts & retries
- If the call fails with a *connection* error: DON'T retry immediately — the generation may still succeed server-side. Call `stitch_get_project` again to see if a new screen appeared.
- If the call fails with a `timeout` (tool time limit): **DO NOT retry the same generation.** Instead call `stitch_get_screen` (or `get_project`) to poll for the result, up to ~10 times every 30s.
- For `stitch_generate_screen_from_text` and `stitch_edit_screens`: **do not spam**. Design generation is async.

## Step 2 — Review the output

After a successful generation, the response contains:

- `outputComponents[0].design.screens[]` — each has:
  - `id` — the new screen resource e.g. `projects/11542754846512650236/screens/a8f44...`
  - `screenshot.downloadUrl` — PNG of the screen
  - `htmlCode.downloadUrl` — the HTML code Genium can use to port to the app
  - `title`, `amountType`, `deviceType`, `status`
- `outputComponents[].text` — the design agent's written breakdown (read it for the design decisions)
- `outputComponents[].suggestion` — Stitch often suggests 2–3 next screens to build (e.g. "Add a Success confirmation screen").
  - If the user says "yes / do it / make it" to a suggestion, call `stitch_generate_screen_from_text` again with that suggestion as the prompt.

## Step 3 — Iterate with edits & variants (instead of regenerating)

- **Edit existing screens:** `stitch_edit_screens` (projectId, `screens` = screen IDs, prompt describing the change). Async like generation.
- **Create riff variants:** `stitch_generate_screen_from_text` again from a fresh prompt, or `stitch_generate_variants` for 1–5 near-duplicates to explore directions.
  - For variants, pass `variantOptions: {variantCount, creativeRange}` with `creativeRange`: `REFINE` (subtle), `EXPLORE` (balanced, default), `REIMAGINE` (radical).

## Step 4 — Design system (only if branding changes)

The NovaWallet design system is already defined (see `get_project` → `designTheme` + `designMd`). Normal generation **does not need** design-system changes.

Only touch:

- `stitch_update_design_system` — change theme/brand tokens
- `stitch_upload_design_md` + `stitch_create_design_system`... — to swap in a new DESIGN.md
- `stitch_apply_design_system` — retrofit an existing system onto a screen

---

## NovaWallet brand rules (embed in every prompt)

Copy these into the prompt text of EVERY generation so Stitch stays on-brand:

1. **Palette:** Light background `#F8FAFC`; white cards with soft indigo-tinted shadows (no heavy borders); Primary indigo `#4F46E5`; success emerald `#10B981`; error rose `#F43F5E`; text navy `#0B1C30`.
2. **Typography:** Inter everywhere. Numeric amounts = 28px bold, tight tracking. Labels 12px caps. Body 16px.
3. **Shapes:** Pill/stadium shapes for buttons, chips, inputs (full roundness). Cards `rounded-xl` (1.25/24px). No dark mode. No illustrations. Subtle indigo elevation instead of borders.
4. **Spacing:** 4px baseline grid, container margin 20px, stacks in 8px steps.
5. **Context:** Zambian fintech market — "Modern Trust" aesthetic: friendly but authoritative, mobile-first, heavy whitespace.

---

## Troubleshooting & gotchas

- **"Project already exists" / extra projects:** there are other Stitch projects (e.g. MediArray) — do not confuse them with NovaWallet. Create nothing; always use the existing id above.
- **Need the download-url content:** `screenshot.downloadUrl`/`htmlCode.downloadUrl` are fetchable later (JWT-signed).
- **Design system regressions:** If a generated screen looks off-brand, the `designSystem` param was probably missing — check it before regenerating.
- **If Stitch isn't connected:** verify the `stitch` MCP server is listed in the availability list before proceeding; if absent it's not mis-wired, report back instead of trying APIs.

## Quick reference — all Stitch tools used here

| Tool | Purpose |
|---|---|
| `stitch_list_projects` / `stitch_get_project` | Find/create projects, read live design-system + screens |
| `stitch_generate_screen_from_text` | Create a NEW screen from prompt (primary workflow) |
| `stitch_edit_screens` | Modify existing screens |
| `stitch_generate_variants` | Explore variants of a screen |
| `stitch_apply_design_system` | Retrofit design system onto screens |
| `stitch_update_design_system` | Change brand tokens |
| `stitch_upload_design_md` / `stitch_create_design_system_from_design_md` | Change DESIGN.md-based theming |
---
name: project-architect
description: Use this skill to design technical blueprints, Room database schemas, and navigation graphs before any code is written.
---

# Goal
Transform high-level requirements from `screens.md` and `architecture.md` into a concrete, actionable `PLAN.md`.

# Instructions
1. **Analyze Requirements:** Review the current task in `PROGRESS.md`.
2. **Design Schema:** Define Data Models (Entities), DAOs, and Repositories.
3. **Draft the Blueprint:** Create a `PLAN.md` file in the project root.
4. **Constraint:** Do not write implementation code (Kotlin/Compose). Focus exclusively on types, structure, and logic flow.

# Example
Input: "Plan Task 1.1: User Identity"
Action: Generate `PLAN.md` detailing the `UserEntity` table, `UserDao` methods, and a 72-hour lockout logic.
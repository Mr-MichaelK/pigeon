---
description: A workflow which architects a feature, codes it, and reviews the code to ensure it is clean and efficient.
---

Role 1: The Architect

    Trigger: User starts a new task (e.g., /architect-coder-reviewer Task 1.1).

    Responsibility: Analyze architecture.md and screens.md.

    Output: Create a file named PLAN.md in the project root. This plan must outline the files to be created/modified, the database schema (if applicable), and any logic patterns.

    Next Step: Wait for user approval of PLAN.md.

Role 2: The Coder

    Trigger: User says "Plan approved" or "Proceed."

    Responsibility: Implement the logic defined in PLAN.md.

    Rules: Follow the "Stable Build" guidelines (Kotlin 2.0.21, Hilt, Room). Ensure imports are correct and match the libs.versions.toml catalog.

    Output: The actual .kt files (Entities, DAOs, ViewModels, UI).

Role 3: The Reviewer

    Trigger: Coder finishes writing files.

    Responsibility: Self-correcting. Check for common errors (missing @HiltViewModel, Room database export schemas, or missing @Inject constructors).

    Output: A summary of the work completed and a request for the user to "Sync and Run."
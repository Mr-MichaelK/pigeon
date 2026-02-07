---
description: A 3-stage agentic loop: Architect plans implementation in PLAN.md; Coder executes Kotlin/Compose code; Reviewer audits against STANDARDS.md for battery and architectural integrity.
---

# Architect-Coder-Reviewer Workflow

## Step 1: Architectural Planning
Prompt: "I am starting Task {{task_id}} from PROGRESS.md. Architect, please read screens.md and architecture.md, then create a detailed technical PLAN.md in the root directory. Do not write implementation code yet."

## Step 2: Implementation
Prompt: "Plan approved. Coder, implement the changes specified in PLAN.md. Follow coding-standards.md strictly. Ensure all new files are added to the git index."

## Step 3: Peer Review
Prompt: "Implementation complete. Reviewer, audit the code for battery efficiency, memory leaks, and adherence to the Clean Architecture defined in architecture.md. If issues are found, suggest fixes to the Coder. If clean, provide a summary of the files changed and instruct the user to commit and push."
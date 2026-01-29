# Project Constitution: Retirement Cash Flow Simulator

**Last Updated**: January 29, 2026  
**Status**: Active

This document defines the core principles, standards, and non-negotiable requirements for developing the Retirement Cash Flow Simulator. All contributors, including AI agents, must adhere to these principles.

---

## Core Development Principles

### 1. Test-First Development (NON-NEGOTIABLE)

**Principle**: Tests must be written BEFORE implementation code.

**Requirements**:
- Unit tests must exist before the code they test
- Integration tests must be planned before feature implementation
- Test coverage must meet minimum thresholds before code review
- Red-Green-Refactor cycle must be followed

**Rationale**: Prevents untested code from entering production, ensures design quality, and provides living documentation.

**Enforcement**: Features without adequate test coverage will be rejected during code review.

---

### 2. Git Commit and Push Protocol (CRITICAL)

**Principle**: Code must NEVER be committed or pushed without EXPLICIT user permission.

#### AI Agent Requirements

**CRITICAL RULE**: AI agents must **NEVER** commit or push code automatically.

#### Required Workflow

1. ✅ **Code Changes**: AI can freely make code changes, edits, and builds
2. ✅ **Testing**: AI can run tests and verify functionality  
3. ✅ **Build Verification**: AI can compile and verify builds succeed
4. ⛔ **STOP HERE** - DO NOT PROCEED TO COMMIT/PUSH

#### Before Commit/Push - AI MUST:

✅ **ASK the user explicitly**:
   - "The changes are ready and tested. Would you like me to commit and push them?"
   - "Build successful. Ready to commit when you say so."
   - "All tests passing. Let me know if you want to commit these changes."

✅ **Wait for user response** - One of:
   - "Yes, commit and push"
   - "Commit and push"
   - "Go ahead"
   - "commit"
   - Other explicit approval

❌ **NEVER assume** commit/push is desired  
❌ **NEVER auto-commit** after builds or tests  
❌ **NEVER auto-push** without user confirmation

#### When User Requests Work (Without Explicit Commit Instruction)

User says: "Make a change" → Make the change, test it, **STOP** (do NOT commit)  
User says: "Add a feature" → Add the feature, test it, **STOP** (do NOT commit)  
User says: "Fix a bug" → Fix the bug, test it, **STOP** (do NOT commit)  
User says: "Update the code" → Update the code, test it, **STOP** (do NOT commit)

#### Only Commit When User EXPLICITLY Says

- "commit" or "commit this" or "commit the changes"
- "commit and push"
- "save these changes"
- "git commit"
- "increment version, commit and push"

#### Correct Workflow Example

```
User: "Add validation to the expense form"

Agent: [Makes change, adds tests, builds successfully]

Agent: "✅ Added expense validation with tests. All builds successful. 
       Ready to commit when you're ready."

[WAIT FOR USER]

User: "commit and push"

Agent: [Now proceeds with version increment, commit, and push]
```

#### Incorrect Workflow Example (DO NOT DO THIS)

```
❌ User: "Add validation to the expense form"
❌ Agent: [Makes change, tests, commits, and pushes WITHOUT ASKING]
```

#### Why This Matters

- **User Review**: User may want to review changes before committing
- **Local Testing**: User may want to test locally in their environment first
- **Commit Message Control**: User may want to customize the commit message
- **Staging Strategy**: User may want to stage changes differently
- **Branch Strategy**: User may be working on a different branching strategy
- **Change Scope**: User may not want all changes committed together
- **Git History**: Commits are permanent records that should be intentional

#### Pre-Commit Checklist (When Permission Granted)

Only proceed with these steps AFTER user explicitly grants permission:

1. ✅ Increment version number (if applicable)
2. ✅ Run build (`./gradlew build`)
3. ✅ Restart services to verify changes work
4. ✅ Stage changes (`git add`)
5. ✅ Commit with clear, descriptive message
6. ✅ Push to remote repository

---

### 3. N-Tier Architecture

**Principle**: Maintain clear separation between presentation, business logic, and data layers.

**Current Structure**:
- **Frontend**: React + Vite + TypeScript (Presentation)
- **Backend Gateway**: Deno + Oak (API Gateway/BFF)
- **API Server**: Kotlin + Ktor (Business Logic)
- **Database**: SQLite (Data Layer)

**Requirements**:
- Frontend calls only the Backend Gateway
- Backend Gateway proxies to API Server
- API Server is the sole owner of database access
- No layer should bypass the tier above or below it

**Rationale**: Ensures maintainability, testability, and proper separation of concerns.

---

### 4. Type Safety and Static Analysis

**Principle**: Leverage strong typing to catch errors at compile time.

**Requirements**:
- TypeScript strict mode enabled in frontend
- Kotlin type system fully utilized
- No `any` types in TypeScript (use `unknown` with guards)
- Serialization types defined for all API contracts

**Rationale**: Prevents runtime errors and improves code quality.

---

### 5. API Server Restart Protocol

**Principle**: API server must be restarted after ANY code changes.

**Critical Note**: The Kotlin API server does NOT support hot-reload.

**When to Restart**:
- Any `.kt` source file changes
- `build.gradle.kts` modifications
- Data model changes
- Business logic updates
- Resource or configuration file changes

**Restart Commands** (Execute in order):
```bash
# 1. Stop
lsof -ti:8090 | xargs kill -9 2>/dev/null

# 2. Start
cd api-server && nohup ./gradlew run > apiserver.log 2>&1 &

# 3. Verify
sleep 10 && lsof -i:8090
```

**Rationale**: Without restart, old code continues running and changes won't take effect.

---

### 6. Code Quality Standards

**Requirements**:
- Code must be formatted according to linting rules
- No compiler warnings in production code
- Meaningful variable and function names
- Comments for complex business logic
- Documentation for public APIs

**Tools**:
- ESLint for TypeScript
- Kotlin compiler warnings enabled
- Prettier for code formatting

---

### 7. Version Control

**Requirements**:
- Semantic versioning (Major.Minor.Build)
- Build number auto-incremented on each build
- Version metadata included in API responses
- Clear commit messages describing changes

**Format**: `v1.1.24` where:
- Major: Breaking changes
- Minor: New features (backward compatible)
- Build: Bug fixes and minor updates

---

### 8. Documentation Standards

**Requirements**:
- README updated for major changes
- API contracts documented in code
- Architecture decisions recorded
- Feature specifications maintained in `/specs`

**Spec-Driven Development**:
- Features must have `spec.md` before implementation
- Implementation must follow `plan.md`
- Tasks tracked in `tasks.md`

---

## Enforcement

These principles are **NON-NEGOTIABLE**. Violations will result in:

1. **Code Review Rejection**: Code that violates these principles will not be merged
2. **Rollback**: Merged code that violates principles will be reverted
3. **Documentation Update**: If a principle needs to change, this constitution must be updated first

---

## Amendment Process

To modify this constitution:

1. Propose changes in a spec document
2. Discuss with team/stakeholders
3. Update this document
4. Communicate changes to all contributors
5. Update dependent templates and documentation

---

## Quick Reference for AI Agents

### ✅ AI Can Do Automatically
- Make code changes
- Run tests
- Build projects
- Restart services
- Read files
- Search codebase
- Generate documentation drafts

### ⛔ AI Must Ask Permission For
- **Committing code**
- **Pushing to repository**
- Creating new branches
- Merging branches
- Deleting files
- Modifying constitution
- Changing version numbers (except during approved commit workflow)

### 🎯 Default Response Pattern

When user requests work:
1. Understand requirement
2. Make changes
3. Test thoroughly
4. Report completion
5. **WAIT for explicit commit instruction**

---

**Remember**: This constitution exists to maintain code quality, ensure proper review processes, and prevent accidental commits. Following these principles protects the project and respects the user's control over their codebase.

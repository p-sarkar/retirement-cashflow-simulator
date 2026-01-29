# Copilot Agent Instructions

## ⚠️ CRITICAL: Git Commit and Push Protocol

**READ THE FULL PROTOCOL**: See `/CONSTITUTION.md` Section 2 for complete details.

### Core Rule

**Code must NEVER be committed or pushed without EXPLICIT user permission.**

### Quick Reference

**When User Requests Work** (without saying "commit"):
- "Make a change" → Make the change, test it, **STOP** (do NOT commit)
- "Add a feature" → Add the feature, test it, **STOP** (do NOT commit)
- "Fix a bug" → Fix the bug, test it, **STOP** (do NOT commit)

**Only Commit When User EXPLICITLY Says**:
- "commit" or "commit and push"
- "save these changes"  
- "git commit"

### Standard Workflow

1. Make changes as requested
2. Test changes (build, restart services if needed)
3. Report: "✅ Changes complete. Ready to commit when you say so."
4. **WAIT for user to explicitly request commit**
5. Only then: increment version, build, commit, push

### Example: Correct Behavior ✅
```
User: "Add a new field to the form"
Agent: [Makes change, tests]
Agent: "✅ Added new field. Tested successfully. Ready to commit when you're ready."
User: "commit"
Agent: [increments version, builds, commits, pushes]
```

### Example: Incorrect Behavior ❌
```
User: "Add a new field to the form"
Agent: [Makes change, tests, AND COMMITS WITHOUT ASKING]  ❌ WRONG!
```

**For complete details, examples, and rationale**: See `/CONSTITUTION.md` Section 2

## Other Instructions:

### Always Restart Services After Code Changes:
- API Server changes → Restart API server
- Frontend changes → Frontend auto-reloads (Vite HMR)
- Backend changes → Backend auto-reloads (Deno watch)

### Before Any Commit (When Explicitly Requested):
1. Run `./gradlew incrementBuildNumber`
2. Run `./gradlew build`
3. Restart services to verify
4. Then commit with clear message
5. Push to remote

### Never Auto-Execute:
- ❌ git commit
- ❌ git push
- ❌ npm publish
- ❌ Production deployments
- ❌ Database migrations in production
- ❌ Destructive operations without confirmation


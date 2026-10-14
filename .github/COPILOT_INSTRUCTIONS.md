# Copilot Agent Instructions

## ⚠️ CRITICAL: NEVER AUTO-COMMIT

**Commits must ALWAYS be a human decision. AI agents must NEVER commit code automatically.**

### When User Asks You To:
- "Make a change" → Make the change, test it, **STOP** (do NOT commit)
- "Add a feature" → Add the feature, test it, **STOP** (do NOT commit)
- "Fix a bug" → Fix the bug, test it, **STOP** (do NOT commit)
- "Update the code" → Update the code, test it, **STOP** (do NOT commit)

### Only Commit When User EXPLICITLY Says:
- "commit" or "commit this" or "commit the changes"
- "commit and push"
- "save these changes"
- "git commit"

### Workflow:
1. **Make changes** as requested
2. **Test changes** (build, restart services if needed)
3. **Report completion** to user
4. **WAIT for user to explicitly request commit**
5. Only then: increment version, build, commit, push

### Example Good Behavior:
```
User: "Add a new field to the form"
Agent: [Makes change, tests]
Agent: "✅ Added new field. Tested successfully. Ready to commit when you're ready."
User: "commit"
Agent: [increments version, builds, commits, pushes]
```

### Example BAD Behavior (DO NOT DO THIS):
```
User: "Add a new field to the form"
Agent: [Makes change, tests, AND COMMITS WITHOUT ASKING]  ❌ WRONG!
```

## Why This Matters:
- Commits are permanent records
- User needs to review changes first
- Commit messages should be written by humans
- Version increments should be intentional
- Git history should be clean and meaningful

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


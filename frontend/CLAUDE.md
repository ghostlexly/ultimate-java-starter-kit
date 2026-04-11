@AGENTS.md

## Conventions

- Never use optimistic updates in React Query mutations. Always wait for the server response before updating the cache.
- Never use nested ternaries. Extract them into early returns, if/else blocks, or dedicated functions.
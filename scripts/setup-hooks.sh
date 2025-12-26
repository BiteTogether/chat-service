#!/bin/bash
echo "Installing Git-hooks..."

HOOKS_DIR=".git/hooks"
HOOK_SCRIPTS_DIR="hooks"

for hook in pre-commit pre-push; do
  if [ -f "$HOOK_SCRIPTS_DIR/$hook.sh" ]; then
    ln -sf "../../$HOOK_SCRIPTS_DIR/$hook.sh" "$HOOKS_DIR/$hook"
    chmod +x "$HOOKS_DIR/$hook"
    echo "  --> Installed $hook hook"
  fi
done

echo "Git hooks-installed."
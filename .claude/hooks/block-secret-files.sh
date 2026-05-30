#!/usr/bin/env bash
# PreToolUse(Edit|Write) guard: refuse to modify .env secret files.
#
# genesis-backend keeps live secrets in .env / .env.* (JWT_SECRET, Cloudinary
# keys, DB creds). These are gitignored; an accidental Claude edit could clobber
# a developer's local credentials. env.example (the placeholder template) is NOT
# blocked — only real dotfiles.
#
# Contract: read the hook payload on stdin, exit 2 to block (stderr is shown to
# the model), exit 0 to allow.

input=$(cat)
fp=$(printf '%s' "$input" \
  | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' \
  | head -1 \
  | sed 's/.*"file_path"[[:space:]]*:[[:space:]]*"//; s/"$//')

[ -n "$fp" ] || exit 0
base=$(basename "$fp")

case "$base" in
  .env | .env.*)
    echo "Refusing to edit '$base' — it holds live secrets (JWT_SECRET, Cloudinary keys, DB creds) and is gitignored. Edit 'env.example' for template changes, or change '$base' yourself outside Claude." >&2
    exit 2
    ;;
esac

exit 0

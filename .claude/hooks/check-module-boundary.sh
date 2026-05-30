#!/usr/bin/env bash
# PostToolUse(Edit|Write) guard: warn at edit-time when a feature module's main
# source imports another module's repository/entity package.
#
# This mirrors ModuleBoundaryTest (genesis-api, ArchUnit) but fires the instant
# the import is written, instead of waiting for `mvn test`. genesis-api (the
# composition root) and genesis-common (shared kernel) are exempt, as is any
# import of the file's own module. Cross-module needs go through an event or an
# outbound port — see the boundary-fix skill.
#
# Note: directory name (genesis-import-export) differs from the package segment
# (com.genesis.importexport), so hyphens are stripped before the self-compare.
#
# Contract: exit 2 surfaces stderr to the model as a strong nudge (the edit has
# already applied — this is a "fix it now" signal, not a block). exit 0 = clean.

input=$(cat)
fp=$(printf '%s' "$input" \
  | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' \
  | head -1 \
  | sed 's/.*"file_path"[[:space:]]*:[[:space:]]*"//; s/"$//')

# Only Java main sources inside a genesis-<module>.
case "$fp" in
  *genesis-*/src/main/*.java) ;;
  *) exit 0 ;;
esac

module=$(printf '%s' "$fp" | sed -n 's#.*/genesis-\([a-z-]*\)/src/main/.*#\1#p')
case "$module" in
  api | common | "") exit 0 ;;  # composition root / shared kernel / unparsable
esac
[ -f "$fp" ] || exit 0

pkg=$(printf '%s' "$module" | tr -d '-')   # import-export -> importexport

hits=$(grep -nE '^import +com\.genesis\.[a-z]+\.' "$fp" \
  | grep -E '\.(repository|entity)\.' \
  | grep -vE "^[0-9]+:import +com\.genesis\.(common|${pkg})\." || true)

if [ -n "$hits" ]; then
  {
    echo "Module-boundary warning: genesis-${module}/src/main reaches into another module's data access:"
    echo "$hits"
    echo
    echo "Feature modules must not depend on another module's repository/entity. Use a thin ApplicationEvent (fire-and-forget write) or an outbound port with an adapter in genesis-api (sync read). See the boundary-fix skill. ModuleBoundaryTest will fail the build if this is a NEW reach."
  } >&2
  exit 2
fi

exit 0

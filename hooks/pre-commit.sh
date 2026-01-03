#!/bin/bash
set -euo pipefail

echo "Checking format for staged Java files..."
### Spotless (with Google Java Format) only applies formatting rules to Java code
### Running it on non-Java files either does nothing
# Get staged Java files (full relative paths)
staged_files=$(git diff --cached --name-only --diff-filter=ACM | grep '\.java$' || true)
if [[ -z "$staged_files" ]]; then
  echo "No staged for commit. Skipping formatting check."
  exit 0
fi

########
# Get absolute paths and convert to regex pattern for spotless
# https://github.com/diffplug/spotless/blob/892fd96e95d5f48daecc0b30f42c30bd0991bd9d/plugin-maven/README.md?plain=1#L1959
patterns=""
for f in $staged_files; do
  abs_path="$(pwd)/$f"
  # Escape slashes for regex and build pattern list
  regex_pattern=$(echo "$abs_path" | sed 's/\//\\\//g')
  if [ -z "$patterns" ]; then
    patterns="$regex_pattern"
  else
    patterns="$patterns,$regex_pattern"
  fi
done

# Execute spotless and re-stage
file_list=$(echo "$staged_files" | paste -sd ',' -)
echo "Formatting files: $file_list"
# echo "Running Spotless on files matching: $patterns"
#
mvn spotless:apply -DspotlessFiles="$patterns"
for f in $staged_files; do
  git add "$f"
done

# Prevent wildcard import
for file in $staged_files; do
  # Check if file contains wildcard imports
  if git show ":$file" | grep -qE 'import\s+.*\.\*;'; then
    echo "Wildcard import found in $file"
    echo "Commit blocked. Please replace with explicit imports."
    exit 1
  fi
done

echo "All staged Java files formatted and re-staged."
exit 0
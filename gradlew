#!/bin/sh
# Wrapper script for Gradle builds on GPanel / CI runners

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "Error: Gradle is not installed on system PATH." >&2
  exit 1
fi

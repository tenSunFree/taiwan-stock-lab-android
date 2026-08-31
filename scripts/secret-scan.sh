#!/usr/bin/env bash
# scripts/secret-scan.sh
#
# Full repository secret scan, in two passes:
#   1. Git history       — gitleaks' default mode, walks `git log -p`
#   2. Current working tree — gitleaks --no-git, scans files directly rather than commit history
#
# These are genuinely different checks: a plain `gitleaks detect --source .` only walks commit
# history by default and does NOT inspect uncommitted working-tree content, so without the
# second pass this script would silently fail to scan anything not yet committed.
#
# Recommended usage:
#   - Before opening a PR
#   - Before a release
#   - After major changes
#   - When you suspect you've previously committed a secret
#
# Not recommended to run on every commit (too slow, and walks the full history every time) —
# that's what pre-commit's staged-only scan is for.
#
# Usage: bash scripts/secret-scan.sh

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

if ! command -v gitleaks >/dev/null 2>&1; then
    echo ""
    echo "gitleaks not found — unable to perform the full secret scan."
    echo ""
    echo "Installation:"
    echo "  macOS         : brew install gitleaks"
    echo "  Windows Scoop : scoop install gitleaks"
    echo "  Windows winget: winget install gitleaks"
    echo "  Other         : https://github.com/gitleaks/gitleaks#installing"
    exit 1
fi

echo ""
echo "[secret-scan] Running full repository scan..."

echo ""
echo "==> Git history"
if gitleaks detect --source . --verbose --redact; then
    echo "Passed: git history"
else
    status=$?
    echo ""
    echo "Potential confidential information detected in git history (exit code: $status)."
    exit "$status"
fi

echo ""
echo "==> Current working tree"
if gitleaks detect --source . --no-git --verbose --redact; then
    echo "Passed: working tree"
else
    status=$?
    echo ""
    echo "Potential confidential information detected in the working tree (exit code: $status)."
    exit "$status"
fi

echo ""
echo "[secret-scan] All checks passed."

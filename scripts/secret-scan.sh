#!/usr/bin/env bash
# scripts/secret-scan.sh
#
# Performs a complete confidential-information scan of the entire working directory and git
# history. Slower than pre-commit's staged-only scan, but more comprehensive — pre-commit can
# only catch what's newly staged, not something committed weeks ago before this hook existed.
#
# Recommended usage:
#   - Before opening a PR
#   - Before a release
#   - After major changes
#   - When you suspect you've previously committed a secret
#
# Not recommended to run on every commit (too slow) — that's what pre-commit's staged scan is for.
#
# Usage: bash scripts/secret-scan.sh

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

if ! command -v gitleaks >/dev/null 2>&1; then
    echo ""
    echo "gitleaks not found, unable to perform a full scan."
    echo "Installation:"
    echo "  macOS         : brew install gitleaks"
    echo "  Windows Scoop : scoop install gitleaks"
    echo "  Windows winget: winget install gitleaks"
    echo "  Other         : https://github.com/gitleaks/gitleaks#installing"
    exit 1
fi

echo ""
echo "==> gitleaks detect (working directory + git history)"

if gitleaks detect --source . --verbose; then
    echo ""
    echo "Passed: no confidential information detected"
else
    STATUS=$?
    echo ""
    echo "gitleaks detected potentially confidential information (exit code: $STATUS)"
    echo "If it is confirmed to be a false positive, create a .gitleaks.toml file to set an allowlist:"
    echo "https://github.com/gitleaks/gitleaks#configuration"
    exit 1
fi

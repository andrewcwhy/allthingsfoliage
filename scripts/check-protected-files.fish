#!/usr/bin/env fish

set protected_files .sc_active_version
set staged (git diff --cached --name-only)

for file in $protected_files
    if contains -- "$file" $staged
        echo "‼️ Attempted to modify a protected file: $file"
        echo "‼️ .sc_active_version is managed by Stonecutter."
        echo "‼️ The Stonecutter active version should always be reset to match the VSC version!"
        echo "‼️ If this change is intentional, use: git commit --no-verify"
        exit 1
    end
end

exit 0

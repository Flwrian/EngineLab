#!/bin/bash
# Script to create a fresh git repo without heavy history

echo "Creating fresh git repository..."

# Backup current remote
REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")

# Remove old git history
rm -rf .git

# Initialize new repo
git init
git add .
git commit -m "Initial commit - clean history"

# Add remote if it existed
if [ -n "$REMOTE_URL" ]; then
    git remote add origin "$REMOTE_URL"
    echo ""
    echo "Remote added: $REMOTE_URL"
    echo "To push: git push -u origin master --force"
fi

echo ""
echo "✅ Fresh repository created!"
du -sh .git

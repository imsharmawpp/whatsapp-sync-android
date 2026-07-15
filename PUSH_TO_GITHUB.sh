#!/bin/bash

# WhatsApp Sync Android - Push to GitHub Script
# This script will push your project to GitHub

echo "WhatsApp Sync Android - GitHub Setup"
echo "======================================"
echo ""

# Check if GitHub username is set
if [ -z "$1" ]; then
    echo "Usage: ./PUSH_TO_GITHUB.sh imsharmawpp"
    echo ""
    echo "Steps:"
    echo "1. Go to https://github.com/new"
    echo "2. Create a new repository named 'whatsapp-sync-android'"
    echo "3. Run this script with your GitHub username"
    exit 1
fi

GITHUB_USERNAME=$1
REPO_URL="https://github.com/$GITHUB_USERNAME/whatsapp-sync-android.git"

echo "Pushing to: $REPO_URL"
echo ""

# Configure git
git config user.name "imsharmawpp"
git config user.email "imsharmawpp@gmail.com"

# Add all files
git add .

# Commit if there are changes
if ! git diff-index --quiet HEAD --; then
    git commit -m "WhatsApp Sync Android - Complete project with OAuth2 and Google Sheets integration"
else
    echo "No changes to commit"
fi

# Add remote origin
git remote remove origin 2>/dev/null
git remote add origin "$REPO_URL"

# Push to GitHub
echo ""
echo "Pushing to GitHub (you may be asked for credentials)..."
echo ""
git push -u origin master --force

echo ""
echo "✓ Project pushed to GitHub!"
echo "View at: https://github.com/$GITHUB_USERNAME/whatsapp-sync-android"

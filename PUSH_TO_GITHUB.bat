@echo off
REM WhatsApp Sync Android - Push to GitHub Script (Windows)
REM This script will push your project to GitHub

echo WhatsApp Sync Android - GitHub Setup
echo ======================================
echo.

REM Check if GitHub username is provided
if "%1"=="" (
    echo Usage: PUSH_TO_GITHUB.bat imsharmawpp
    echo.
    echo Steps:
    echo 1. Go to https://github.com/new
    echo 2. Create a new repository named 'whatsapp-sync-android'
    echo 3. Run this script with your GitHub username
    pause
    exit /b 1
)

set GITHUB_USERNAME=%1
set REPO_URL=https://github.com/%GITHUB_USERNAME%/whatsapp-sync-android.git

echo Pushing to: %REPO_URL%
echo.

REM Configure git
git config user.name "imsharmawpp"
git config user.email "imsharmawpp@gmail.com"

REM Add all files
git add .

REM Commit
echo Committing changes...
git commit -m "WhatsApp Sync Android - Complete project with OAuth2 and Google Sheets integration"

REM Remove old remote if exists
git remote remove origin 2>nul

REM Add new remote
git remote add origin %REPO_URL%

REM Push to GitHub
echo.
echo Pushing to GitHub (you may be asked for credentials)...
echo.
git push -u origin master --force

echo.
echo Success! Project pushed to GitHub
echo View at: https://github.com/%GITHUB_USERNAME%/whatsapp-sync-android
pause

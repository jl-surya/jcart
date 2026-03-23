#!/bin/bash

# JCart Deployment Script
# Compiles Java source files and deploys to Tomcat webapps directory
# Steps: Shutdown Tomcat -> Clean old deployment -> Compile -> Copy files -> Start Tomcat

set -e

APP_SRC="/home/surya-pt8233/Documents/Tasks/task1/JCart"
TOMCAT_HOME="/opt/tomcat"
WEBAPPS_DIR="$TOMCAT_HOME/webapps"
SRC_DIR="$APP_SRC/src"
CLASSES_DIR="$APP_SRC/WEB-INF/classes"
DEPLOY_DIR="$WEBAPPS_DIR/JCart"

echo "[1/5] Shutting down Tomcat..."
$TOMCAT_HOME/bin/shutdown.sh 2>/dev/null || true
sleep 5
pkill -f "catalina.startup.Bootstrap" 2>/dev/null || true

echo "[2/5] Removing old deployment..."
rm -rf "$DEPLOY_DIR"

echo "[3/5] Compiling Java files..."
mkdir -p "$CLASSES_DIR"
javac -cp "/opt/tomcat/lib/*" \
      -d "$CLASSES_DIR" \
      -sourcepath "$SRC_DIR" \
      $(find "$SRC_DIR" -name "*.java")

echo "[4/5] Copying files to webapps..."
mkdir -p "$DEPLOY_DIR"
cp -r "$APP_SRC/WEB-INF"   "$DEPLOY_DIR/"

echo "[5/5] Starting Tomcat..."
$TOMCAT_HOME/bin/startup.sh

echo "Deployment complete! http://localhost:8080/JCart/"
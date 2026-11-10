#!/usr/bin/env sh
# NG Browser: CI installs Gradle 8.9 via gradle/actions/setup-gradle.
# This shim forwards to that Gradle on PATH.
exec gradle "$@"

#!/bin/sh
# One-off account management for the running API container.
#
# Reuses the container's environment and Spring profile (the same ones the
# entrypoint used) so it talks to the same database, but the application
# disables its web server when it sees --create-admin/--create-super-admin.
exec java -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-render}" \
  -jar /app/app.jar "$@"

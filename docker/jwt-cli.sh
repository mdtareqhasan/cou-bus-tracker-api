#!/bin/sh
# One-off JWT minting for the running API container.
#
# Reuses the container's environment and Spring profile (the same ones the
# entrypoint used), so the token is signed with the same JWT_SECRET the API
# uses, but the application disables its web server when it sees --gen-jwt.
exec java -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-render}" \
  -jar /app/app.jar "$@"

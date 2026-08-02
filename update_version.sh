#!/bin/bash
#
# Updates plugin to a the specified version of elasticsearch
# To bump the patch version run ./update_version.sh +1
# To release a specific version run ./update_version.sh 1.2.3
# To build the latest snapshot from the current branch run ./update_version.sh
# Add --docker (in any position) to run the build in a Docker container instead of the
# host machine, e.g. ./update_version.sh --docker +1
# The docker image used can be overridden with the DOCKER_IMAGE env var.

set -e

DOCKER_IMAGE="${DOCKER_IMAGE:-eclipse-temurin:21-jdk}"

use_docker=false
args=()
for arg in "$@"; do
  if [[ "$arg" == "--docker" ]]; then
    use_docker=true
  else
    args+=("$arg")
  fi
done
set -- "${args[@]}"

# Detect OS for sed compatibility
if [[ "$OSTYPE" == "darwin"* ]]; then
  SED_INPLACE=("sed" "-i" "")
else
  SED_INPLACE=("sed" "-i")
fi

git_root=$(git rev-parse --show-toplevel)
git_branch=$(git branch --show-current)
revision=${REVISION:-0}
if [[ "$#" == 0 ]]; then
  branch="$git_branch"
  version=$(curl -s https://raw.githubusercontent.com/elastic/elasticsearch/$branch/build-tools-internal/version.properties | grep "^elasticsearch\s*=\s*" | cut -d'=' -f2)
  if [[ ! $version ]]; then
    echo "Cannot determine version for branch $branch"
    exit  1
  fi
  version="${version// /}-SNAPSHOT"
  release=false
  echo "Updating to the latest version ${version} in the branch $branch"
else
  release=true
  if [[ "$1" == "+1" ]]; then
    current_version=$(cat "$git_root/gradle.properties" | grep "^pluginApiVersion="  | cut -d'=' -f2)
    if [[ $current_version == *"-SNAPSHOT" ]]; then
        suffix="-SNAPSHOT"
        current_version=${current_version%"-SNAPSHOT"}
    fi
    IFS='.' read -r major minor patch <<< "$current_version"
    if [[ ! "$suffix" ]]; then
      patch=$((patch + 1))
    fi
    current_version=$"$major.$minor.$patch"
  else
    current_version="$1"
  fi
  version="$current_version"
  branch="v$version"
  echo "Updating to version $version"
fi

gradle_version=$(curl -s https://raw.githubusercontent.com/elastic/elasticsearch/$branch/gradle/wrapper/gradle-wrapper.properties | grep "^distributionUrl=" | cut -d'-' -f2)
if [[ ! $gradle_version ]]; then
  echo "Cannot determine gradle version for branch $branch and version $version."
  exit  1
fi
echo "Using gradle $gradle_version"

# Detect gradle version
"${SED_INPLACE[@]}" -e "s/^release=.*/release=$release/" "$git_root/gradle.properties"
"${SED_INPLACE[@]}" -e "s/^revision=.*/revision=$revision/" "$git_root/gradle.properties"
"${SED_INPLACE[@]}" -e "s/^elasticsearchBranch=.*/elasticsearchBranch=$branch/" "$git_root/gradle.properties"
"${SED_INPLACE[@]}" -e "s/^pluginApiVersion=.*/pluginApiVersion=$version/" "$git_root/gradle.properties"
"${SED_INPLACE[@]}" -e "s/^distributionUrl=.*/distributionUrl=https\\\\:\/\/services.gradle.org\/distributions\/gradle-$gradle_version-all.zip/" "$git_root/gradle/wrapper/gradle-wrapper.properties"

build_failed=false
if [ "$use_docker" = "true" ]; then
  echo "Building in Docker container ($DOCKER_IMAGE)..."
  docker_home="$git_root/.gradle/docker-home"
  mkdir -p "$docker_home"
  if ! docker run --rm --init \
      -v "$git_root":/workspace \
      -w /workspace \
      -u "$(id -u):$(id -g)" \
      -e HOME=/workspace/.gradle/docker-home \
      -e GRADLE_USER_HOME=/workspace/.gradle/docker-home/.gradle \
      "$DOCKER_IMAGE" \
      bash -c "./gradlew clean build"; then
    build_failed=true
  fi
else
  ./gradlew clean
  ./gradlew build
fi


if [ "$release" = "true" ]; then
  if [ "$build_failed" = "true" ]; then
    echo "Build failed; skipping release."
    exit 1
  fi
  release_version="v$version+$revision"
  echo "Releasing... $release_version"
  git add .
  git commit -m "Release $release_version"
  git tag -a "$release_version" -m "$release_version"
  git push origin --tags "$git_branch"
fi

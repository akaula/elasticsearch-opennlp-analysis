#!/bin/bash
#
# Updates plugin to a the specified version of elasticsearch
# To bump the patch version run ./update_version.sh +1
# To release a specific version run ./update_version.sh 1.2.3
# To build the latest snapshot from the current branch run ./update_version.sh

set -e
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
sed -i '' "s/^release=.*/release=$release/" "$git_root/gradle.properties"
sed -i '' "s/^revision=.*/revision=$revision/" "$git_root/gradle.properties"
sed -i '' "s/^elasticsearchBranch=.*/elasticsearchBranch=$branch/" "$git_root/gradle.properties"
sed -i '' "s/^pluginApiVersion=.*/pluginApiVersion=$version/" "$git_root/gradle.properties"
sed -i '' "s/^distributionUrl=.*/distributionUrl=https\\\\:\/\/services.gradle.org\/distributions\/gradle-$gradle_version-all.zip/" "$git_root/gradle/wrapper/gradle-wrapper.properties"

# We still need JDK 17
JAVA_HOME=`/usr/libexec/java_home -v17` ./gradlew clean build

if [ "$release" = "true" ]; then
    release_version="$version+$revision"
    echo "Releasing... $release_version"
    git add .
    git commit -m "Release $release_version"
    git tag -a "v$release_version" -m "v$release_version"
    git push origin --tags "$git_branch"
fi

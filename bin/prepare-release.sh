#!/bin/bash
set -eu

function usage() {
    cat <<- EOF
Prepares a version for release (patch or minor).

This script:
1. Runs a full 'mvn verify' on the new release
2. Creates a new tag for the release version
3. Updates the special tag 'latest-release' to point to the new release
4. Updates the pom.xml to the next development version

usage: prepare-release.sh [<flags>] [<type>]

args:
    <type>                        What type of release to prepare.
                                  Valid values: "patch" "minor"
                                  If not provided will do a patch version
flags:
    -h  --help                    Display this message

example:
    ./prepare-release.sh minor

EOF
}

function prop {
    grep -w "${1}" ./release.properties|cut -d'=' -f2
}

function checkOnMaster() {
    if [[ "$(git rev-parse --abbrev-ref HEAD)" != "master" ]]; then
        echo "Can only prepare a release from the master branch."
        echo "Aborting."
        exit 1
    fi
}

function checkUpToDate() {
    git remote update > /dev/null
    if git merge-base --is-ancestor origin/master master; then
        return
    else
        echo "Local master is behind origin. Refresh your local branch before preparing a release."
        exit 1
    fi
}

while [ $# -gt 0 ]; do
  case $1 in
    -h|--help )
      usage
      exit 2
      ;;
    * )
      releaseType="$1"
      shift
      ;;
  esac
done

checkOnMaster
checkUpToDate

profile="patch-release"
if [ -z "${releaseType:-}" ]; then
  echo "Preparing patch release..."
elif [[ "${releaseType}" == "patch" ]]; then
  echo "Preparing patch release..."
elif [[ "${releaseType}" == "minor" ]]; then
  echo "Preparing minor release..."
  profile="minor-release"
else
  echo "Unknown release type ${releaseType}"
  exit 1
fi

pushd "$(dirname ${BASH_SOURCE[0]})/.." > /dev/null

mvn build-helper:parse-version release:prepare -B -P${profile}
git tag -f latest-release "$(prop 'scm.tag')"
git push origin --tags -f
mvn release:clean -q

popd > /dev/null
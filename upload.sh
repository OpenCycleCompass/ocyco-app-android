#!/bin/sh

BASE_URL="https://dl.ocyco.de/"
FILE=$(find . | grep -P 'app/build/outputs/apk/ocyco-app-.+\.apk')
FILE_NAME=$(echo ${FILE} | rev | cut -d/ -f1 | rev)
TRAVIS_BRANCH_ESCAPED=$(echo "$TRAVIS_BRANCH" | sed -r 's/[/]+/-/g')

curl --verbose --upload-file ${FILE} --user ${OCYCO_UPLOAD_AUTH} ${BASE_URL}branches/${TRAVIS_BRANCH_ESCAPED}/${FILE_NAME}

if [ "$TRAVIS_BRANCH" = "develop" ]
then
    echo "Branch is 'develop': update $BASE_URL/ocyco-develop.apk (updating redirect)"
    curl --verbose -X PUT -H 'Content-Type: text/text' -d "branches/${TRAVIS_BRANCH_ESCAPED}/${FILE_NAME}" --user ${OCYCO_UPLOAD_AUTH} https://dl.ocyco.de/branches/current-develop
fi
if [ "$TRAVIS_BRANCH" = "master" ]
then
    echo "Branch is 'master': update $BASE_URL/ocyco-master.apk (updating redirect)"
    curl --verbose -X PUT -H 'Content-Type: text/text' -d "branches/${TRAVIS_BRANCH_ESCAPED}/${FILE_NAME}" --user ${OCYCO_UPLOAD_AUTH} https://dl.ocyco.de/branches/current-master
fi

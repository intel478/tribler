#!/bin/bash

set -e

export ADB="adb -s $DEVICE"

timestamp=$(date +%s)

$ADB logcat -c

echo Start nosetests
$ADB shell am start -n org.tribler.android/.NoseTestActivity

echo Waiting on tests to finish
python wait_for_process_death.py "org.tribler.android:service_NoseTestService"

echo Fetching results
python adb_pull.py "../output/nosetests.xml" "$DEVICE.$timestamp.nosetests.xml"
python adb_pull.py "../output/coverage.xml" "$DEVICE.$timestamp.coverage.xml"

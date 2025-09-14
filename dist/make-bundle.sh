#!/bin/bash
rm -rf Pin2Dmd-Editor.app
unzip app-skeleton.zip
chmod a+x Pin2Dmd-Editor.app/Contents/MacOS/launcher
cp ../target/*-with-dependencies.jar Pin2Dmd-Editor.app/Contents/Resources



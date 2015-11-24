#!/bin/bash
rm -rf build/
mkdir build
mvn -Pwin clean package assembly:single launch4j:launch4j
cp target/pin2dmd-editor.exe build/
mvn -Pwin_64 clean package assembly:single launch4j:launch4j
cp target/pin2dmd-editor64.exe build/
mvn -Plinux_x86_64 clean package assembly:single
cat dist/stub.sh target/go-dmd-clock-*-jar-with-dependencies.jar >pin2dmd-editor && chmod a+x pin2dmd-editor
mv pin2dmd-editor build/
mvn -Pmac_64 clean package assembly:single
cd dist
rm -rf Pin2Dmd-Editor.app/
ant
zip -r Pin2Dmd-Editor.zip Pin2Dmd-Editor.app
cd ..
mv dist/Pin2Dmd-Editor.zip build
echo "Successfull build"


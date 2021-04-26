#!/bin/bash
version=$(git describe --always --tags | awk -F- '{print $1}')
rm -rf build/
mkdir build
mvn -Pwin clean package assembly:single launch4j:launch4j
cp target/pin2dmd-editor.exe build/pin2dmd-editor-${version}.exe
mvn -Djavacpp.platform=windows-x86_64 -DskipTests -Pwin_64 clean package assembly:single launch4j:launch4j
cp target/pin2dmd-editor64.exe build/pin2dmd-editor64-${version}.exe
mvn -Plinux_x86_64 clean package assembly:single
cat dist/stub.sh target/go-dmd-clock-*-jar-with-dependencies.jar >pin2dmd-editor-${version} && chmod a+x pin2dmd-editor-${version}
mv pin2dmd-editor-${version} build/
mvn -Djavacpp.platform=macosx-x86_64 -DskipTests -Pmac_64 clean package assembly:single
cd dist
rm -rf Pin2Dmd-Editor.app/
ant
zip -r Pin2Dmd-Editor64-${version}.zip Pin2Dmd-Editor.app
cd ..
mv dist/Pin2Dmd-Editor64-${version}.zip build
mvn -Pmac clean package assembly:single
cd dist
rm -rf Pin2Dmd-Editor.app/
ant
zip -r Pin2Dmd-Editor-${version}.zip Pin2Dmd-Editor.app
cd ..
mv dist/Pin2Dmd-Editor-${version}.zip build
echo "Successfull build"

# -Djavacpp.platform=macosx-x86_64 -> for mac64 

#!/bin/bash

version=$(git describe --always --tags)
date=$(date)
echo "$version -- $date" > version

zip go-dmd-editor.jar version
zip go-dmd-editor_mac_x86.jar version
zip go-dmd-editor_mac_x86_64.jar version
zip go-dmd-editor_x86.jar version
zip go-dmd-editor_x86_64.jar version

(cd target/classes/ && zip -r ../../go-dmd-editor.jar com)
(cd target/classes/ && zip -r ../../go-dmd-editor_mac_x86.jar com)
(cd target/classes/ && zip -r ../../go-dmd-editor_mac_x86_64.jar com)
(cd target/classes/ && zip -r ../../go-dmd-editor_x86.jar com)
(cd target/classes/ && zip -r ../../go-dmd-editor_x86_64.jar com)

rm go-dmd-editor-${version}.zip
zip -r go-dmd-editor-${version}.zip transitions go-dmd-editor*.jar font0.dat start*

echo "go-dmd-editor-${version}.zip build"

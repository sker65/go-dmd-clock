version=$(git describe --always --tags)
date=$(date)
echo "$version -- $date" > version


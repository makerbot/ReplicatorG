#!/bin/sh
# I created this because I couldn't get the ant target dist to compile on OS X =ml=

if [ `uname` != 'Darwin' ]; then
	echo "OS X is required to build all distros."
	echo "You have been warned!"
fi

rm -rf dist-all
mkdir dist-all

echo "===> Compiling Linux distro..."
./dist-linux.sh &
wait
mv dist/* dist-all/.
echo "===> done"
echo "===> Compiling Windows distro..."
./dist-windows.sh &
wait
mv dist/* dist-all/.
echo "===> done"
echo "===> Compiling OS X distro..."
./dist-mac.sh &
wait
mv dist/* dist-all/.
rm -rf dist
echo "===> done"

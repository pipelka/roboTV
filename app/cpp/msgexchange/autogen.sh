#!/bin/sh

autoreconf -vif || ( echo "***ERROR*** autoreconf failed." ; exit 1 )

echo
echo "Please run ./configure now."

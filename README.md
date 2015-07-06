# roboTV
Android TV frontend for XVDR

## About

roboTV is a Android TV based frontend for XVDR.
Now you can use your Android TV Box (or your TV running Android) to stream LiveTV channels from your VDR backend (xvdr-plugin must be installed).

##  Building

1. Build the ndk part:
```bash
# cd roboTV/app
# ndk-build
```

2. Build the Java part:
```bash
# cd roboTV
# ./gradlew build
```
or use AndroidStudio

## Features

- [x] Watching Live TV
- [x] Channel Icons
- [x] EPG
- [ ] Schedule / Watch Recordings
- [ ] Movie Database integration


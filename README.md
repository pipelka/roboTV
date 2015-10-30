# roboTV

roboTV is a Android TV based frontend for XVDR.
Now you can use your Android TV Box (or your TV running Android) to stream LiveTV channels from your VDR backend (xvdr-plugin must be installed).

## Screenshots

<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg-genre.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/multiaudio.jpg" width="200" />

## System requirements

* Android TV 5.1 (API Level 22)

The Google Nexus Player is used for development.

## Building

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

# Current Features

- [x] Watching Live TV
- [x] Channel Icons
- [x] EPG
- [x] H264 Video support
- [x] decoding of AC3 streams
- [x] MPEG Audio support

# Planned Features

- [ ] Full timeshift support
- [ ] Support for EAC3 audio streams
- [ ] Schedule / Watch Recordings
- [ ] Movie Database integration


# roboTV

roboTV is a Android TV based frontend for XVDR.
Now you can use your Android TV Box (or your TV running Android) to stream LiveTV channels from your VDR backend (xvdr-plugin must be installed).

## Screenshots

<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg-genre.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/multiaudio.jpg" width="200" />

## System requirements

* [Android TV 5.1](https://www.android.com/tv/) (API Level 22)
* [VDR 2.2](http://www.vdr-wiki.de/)
* [vdr-plugin-xvdr](https://github.com/pipelka/vdr-plugin-xvdr) ([robotv](https://github.com/pipelka/vdr-plugin-xvdr/tree/robotv) branch)

The Google [Nexus Player](https://www.google.com/nexus/player/) and the NVIDIA [Shield Android TV](https://shield.nvidia.com/android-tv) are used for development.

## Building

1. Set the SDK and NDK location in Android Studio or "local.properties"
2. ./gradlew assembleDebug

or use AndroidStudio

# Current Features

- [x] Watching Live TV
- [x] Channel Icons
- [x] EPG
- [x] H264 Video support
- [x] MPEG2 Video support
- [x] decoding of AC3 streams
- [x] MPEG Audio support
- [X] Movie Database integration for EPG

# Planned Features

- [ ] Full timeshift support
- [ ] Schedule / Watch Recordings
- [ ] H265 (UHD) Support (partly done)
- [ ] AC3 passthrough (config option missing)

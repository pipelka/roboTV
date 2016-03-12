# roboTV

roboTV is a Android TV based frontend for VDR.
Now you can use your Android TV Box (or your TV running Android) to stream LiveTV channels from your VDR backend (robotv-plugin must be installed).

## Screenshots

<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv-shortcuts.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv-timeshift.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg-genre.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/multiaudio.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/homescreen.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-folders.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-all.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-details.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-playback.jpg" width="200" />
<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/setup.jpg" width="200" />

## System requirements

* [Android TV](https://www.android.com/tv/) (minimum API Level 22)
* [VDR 2.2](http://www.vdr-wiki.de/)
* [vdr-plugin-robotv](https://github.com/pipelka/vdr-plugin-robotv)

Note: for roboTV versions up to 0.3.0 the xvdr plugin (robotv branch) was needed:

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
- [X] AC3 passthrough

# Planned Features

- [ ] Full timeshift support
- [ ] Schedule / Watch Recordings
- [ ] H265 (UHD) Support (partly done)

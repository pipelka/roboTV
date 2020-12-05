[![Travis](https://img.shields.io/travis/pipelka/roboTV.svg)](https://travis-ci.org/pipelka/roboTV)
[![GitHub release](https://img.shields.io/github/release/pipelka/roboTV.svg)](https://github.com/pipelka/roboTV/releases)

# roboTV

roboTV is a Android TV based frontend for VDR.
Now you can use your Android TV Box (or your TV running Android) to stream LiveTV channels from your VDR backend (robotv-plugin must be installed).

## Screenshots
|   |   |   |
|---|---|---|
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv-shortcuts.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/livetv-timeshift.jpg" width="200" />
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/epg-genre.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/multiaudio.jpg" width="200" />
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/homescreen.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-folders.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-all.jpg" width="200" />
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-details.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-details2.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-playback.jpg" width="200" />
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-series.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-series-detail.jpg" width="200" />|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/movies-timers.jpg" width="200" />
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/setup.jpg" width="200" />| | |

## System requirements

* [Android TV](https://www.android.com/tv/) (minimum API Level 29 Android TV 9 Pie)
* [VDR 2.4](http://www.vdr-wiki.de/)
* [vdr-plugin-robotv](https://github.com/pipelka/vdr-plugin-robotv)
* [vdr-epgsearch](http://www.vdr-wiki.de/wiki/index.php/Epgsearch-plugin) (recommended)

The Xiaomi [Mi Box](https://www.mi.com/global/mibox) and the NVIDIA [Shield Android TV](https://shield.nvidia.com/android-tv) are used for development.
A Sony Bravia TV (KD-43FX7596) with AndroidTV also uses RoboTV in our home.

### Server Deployment

The roboTV server (VDR, plugins, configuration) can be deployed easily with the "robotv" docker image:
https://github.com/pipelka/vdr-plugin-robotv

## Building

1. Download [Android Studio](https://developer.android.com/studio/index.html)
2. Set the SDK in Android Studio (Project Structure) or "local.properties".
4. ./gradlew assembleDebug

# Current Features

- [x] Watching Live TV
- [x] Channel Icons
- [x] EPG
- [x] H264 Video support
- [x] MPEG2 Video support
- [x] decoding of AC3 streams
- [x] EAC3 support
- [x] MPEG Audio support
- [x] Movie Database integration for EPG
- [x] AC3 passthrough
- [x] Watch Recordings
- [x] Timeshift support
- [x] Schedule Recordings (create timers)
- [x] Edit timers
- [x] Delete recordings
- [x] Move recordings between folders
- [x] Cover Artwork browsing
- [x] Timeshift Reverse Play
- [x] Management of TV shows (Season / Episode)
- [x] Notifications about timers / recordings
- [ ] H265 (UHD) Support (VDR 2.3.x)

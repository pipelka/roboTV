[![Travis](https://img.shields.io/travis/pipelka/roboTV.svg)](https://travis-ci.org/pipelka/roboTV)
[![GitHub release](https://img.shields.io/github/release/pipelka/roboTV.svg)](https://github.com/pipelka/roboTV/releases)

# roboTV

roboTV is a Android TV based frontend for VDR.
Now you can use your Android TV Box (or your TV running Android) to stream LiveTV channels from your VDR backend (robotv-plugin must be installed).

## Screenshots
|   |   |
|---|---|
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/livetv-2020.jpg" width="400" /><br /><small>LiveTV</small>|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/livetv-epg-2020.jpg" width="400" /><br /><small>EPG</small>
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/livetv-timer-2020.jpg" width="400" /><br /><small>Schedule Recordings</small>|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/livetv-addtimer-2020.jpg" width="400" /><br /><small>Add Timer</small>
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/channel-recommendation-2020.jpg" width="400" /><br /><small>Recommendation Channel</small>|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/browsing-2020.jpg" width="400" /><br /><small>Browse Recordings</small>
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/browsing-details-2020.jpg" width="400" /><br /><small>Movie Details</small>|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/browsing-cover-2020.jpg" width="400" /><br /><small>Change Artwork</small>
|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/browsing-search-2020.jpg" width="400" /><br /><small>Search Recordings</small>|<img src="https://raw.githubusercontent.com/pipelka/roboTV/master/media/screenshots/2020/edit-recording.jpg" width="400" /><br /><small>Edit Recording (long press on recording)</small>

## System requirements

* [Android TV](https://www.android.com/tv/) (minimum API Level 29 Android TV 9 Pie)
* [VDR 2.4](http://www.vdr-wiki.de/)
* [vdr-plugin-robotv](https://github.com/pipelka/vdr-plugin-robotv)
* [vdr-epgsearch](http://www.vdr-wiki.de/wiki/index.php/Epgsearch-plugin) (recommended)

The Xiaomi [Mi Box](https://www.mi.com/global/mibox) and the NVIDIA [Shield Android TV](https://shield.nvidia.com/android-tv) are used for development.
A Sony Bravia TV (KD-43FX7596) with AndroidTV and a Chromecast with [Google TV](https://tv.google/) also use RoboTV in our home.

### Server Deployment

The roboTV server (VDR, plugins, configuration) can be deployed easily with the "robotv" docker image:
https://github.com/pipelka/vdr-plugin-robotv

## Building

1. Download [Android Studio](https://developer.android.com/studio/index.html)
2. Set the SDK in Android Studio (Project Structure) or "local.properties".
4. Build in Android Studio or do a `./gradlew assembleDebug` on the command line

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
- [x] H265 (UHD) Support

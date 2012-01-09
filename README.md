## Another implementation of MobileOrg client for Android ##

Originally based on [MobileOrg](https://github.com/matburt/mobileorg-android), the source code is 90% new.

<a href="http://market.android.com/details?id=com.matburt.mobileorg.ng" style="cursor: pointer;">
  <img src="http://www.android.com/images/brand/60_avail_market_logo2.png"
       alt="Available in Android Market" />
</a>

<link href="https://plus.google.com/101518077347551839865" rel="publisher" />
<a href="https://plus.google.com/101518077347551839865?prsrc=3" style="text-decoration:none;">Project's page on Google+ <img src="https://ssl.gstatic.com/images/icons/gplus-16.png" alt="" style="border:0;width:16px;height:16px; align: left;"/></a>
Supported platforms: Android 1.6 - 4.0

Features:

* New Dropbox library with new login screen (don't need to enter login/password if you have Dropbox app installed)
* New parser, data is saved to SQLite DB but not to files
* New outline viewer UI - you can collapse/expand outlines in same view and open outline in new window - long click or Space button
* Colored outline viewer, similar to default emacs colors
* Outline visibility cycling, like in emacs - collapse -> expand -> expand all
* Support of plain lists with collapse/expand
* Support of Android 3 and 4 with new Action Bar, application still remains backward compatible to Android 1.6
* Support of Tablets, in landscape mode, screen transforms to two-pane, where you can browse your agenda/outlines in left pane and load outlines into right pane (like two-pane file managers)
* Icons for all menu actions (special white icons for Android>=3.0)
* Icon and sync progress in status bar
* New capture/edit dialog
* Support of add/edit/remove text and plain list entries of captured/existing outlines
* Periodical sync, auto-send changes
* Date/time and links parsing
* New configurable and resizable widget to display set of outlines
* Checkboxes in plain lists
* PKI and symmetric encryption via APG (see org-crypt.el)
* Bookmarks to outlines
* Capture templates using bookmarks
* Search
* Integration with other apps (like Tasker) via Intents (sync for now)
* Keyboard support (C - capture, S - sync, E - edit, Z - add text)

Screenshots:

![Main window](mobileorg-android/raw/master/src-img/ss-small/main.png) 
![Capture window](mobileorg-android/raw/master/src-img/ss-small/capture.png) 
![Sync progress](mobileorg-android/raw/master/src-img/ss-small/sync.png) 
![Search](mobileorg-android/raw/master/src-img/ss-small/search.png) 
![Widget preferences](mobileorg-android/raw/master/src-img/ss-small/widget_pref.png) 
![Outline widget](mobileorg-android/raw/master/src-img/ss-small/widget.png) 
![Tablet](mobileorg-android/raw/master/src-img/ss-small/tablet.png)

Limitation!

Currently only Dropbox sync has been tested, if you want/need support of other sync types, you are welcome to help

License remains same, GPLv2
PhonePlede
==========

For Licensing and copying information, see the file COPYING.

PhonePledge is an application for fundraising events. The application
allows you to manage and accept donation pledges via SMS, which can
then be displayed live on, e.g., a big projector screen. PhonePledge
can also display a slide show with images at times when there are no
incoming pledges.

PhonePledge uses Google Voice as the back-end, and thus requires a
Google Voice account. The Google Voice connectivity is provided by the
unofficial Java API library found at:

http://code.google.com/p/google-voice-java/

Functional Overview
-------------------

The PhonePledge application consists of two windows: the 'control'
window and the pledge 'display' window. The control window allows a
person to manage and selectively accept donation pledges that arrive
via SMS to a Google Voice account, while the display window shows
"accepted" pledges. Accepted pledges are put in a queue for being
displayed one-at-a-time in the display window. When the queue is empty
(e.g., at times of low pledge activity), the display window will flip
into "slide" mode, looping through a set of images configured by the
user (for, e.g., promotion purposes). Thus, the display window
operates in two modes: "pledge" mode or "display" mode. These modes
are typically set automatically based on the state of the pledge
display queue, but can also be overridden via the control window. For
instance, setting pledge mode when the queue is empty (e.g., when the
app has automatically switched to slide mode) will repopulate the
pledge display queue with all previously accepted pledges and loop
through them again.

PhonePledge can send automatic replies (e.g., "thank you" notes) to
the originators of incoming pledges. The default reply message can be
changed using a configuration file as described below. The reply
functionality must be enabled after the program has started, as it
would otherwise send replies to all SMS originators every time it
downloads the list of SMSes from the Google Voice account on
startup. Use the reply functionality with caution---you do not want to
spam your donors with lots of duplicate thank you notes.

Building from Source Code
-------------------------

Compile with ```make``` or the Eclipse IDE. Requires a Java JDK
installed on the system.

To compile without Eclipse in a terminal window, type:

$ make

To run:

In most OSes it is sufficient to just double click the jar
file. Otherwise, run ```java -jar phonepledge.jar``` in a terminal
window.

Configuration
-------------

Settings related to the control window and display window are
configured separately using two configuration files:
```control.properties``` and ```display.properties```. These should be
put in the same directory as the phonepledge.jar file. The
configuration file format is based on key-value pairs in the form
```key=value``` (one per line).

The ```display.properties``` configuration file supports the following
key-value pairs:

```
NextSlideTimeout=NUMBER
	Number of seconds between each slide

TelephoneNumberFont=TEXT
	Font of telephone number shown (Name of some system font, e.g., Arial)

BackgroundColor=NUMBER,NUMBER,NUMBER
	The RGB color code to use for the background of the display
	window

PledgeFont=TEXT
	Sets the font of the Pledges shown in the center of the screen

PledgeFontSize=NUMBER
	Sets the font size of the Pledges shown in the center of the screen

PledgeFontColor=NUMBER,NUMBER,NUMBER 
        The RGB color code to use for the pledges shown in the center of the
	screen.

TickerFont=TEXT
	Sets the font of the pledges in the Ticker

TickerFontSize=NUMBER
	Sets the font size of pledges in the Ticker

TickerColor=NUMBER,NUMBER,NUMBER
	The RGB color code to use for the ticker background in the
	display window

TickerFontColor=NUMBER,NUMBER,NUMBER
	The RGB color code to use for the ticker font in the
	display window

TickerHz=TEXT
	How frequently the ticker should be updated (affects smoothness)

TicksPerSecond=NUMBER
	How "far" the ticker should move each second

NextPledgeTimeout=NUMBER
	Timeout before showing next pledge (seconds)

PledgeInstruction=TEXT
	The instruction on how to send pledges (text above phone number)

PhoneNumber=TEXT 
	The phone number to send pledges to (this will probably 
	be overridden once logged in to Google Voice)
	

NOTE: most of these settings can also be changed from the control
window.

The 'control.properties' configuration file supports the following
key-value pairs:

SmsReplyMsg=TEXT
	The text message to send as a reply to incoming pledges

GoogleVoiceUser=TEXT
	The username for the Google Voice account. If set, the login
	dialog box will not be shown at startup

GoogleVoicePassword=TEXT
	The password for the Google Voice account
```

Changing the Logos
------------------

There are two logos displayed in the application. One large logo in
the lower left corner (logo.png) and one small that is displayed in
the pledge ticker (ticker.png). To change these, put a logo.png and a
ticker.png in phonepledge.jar directory. The corner logo should
preferably have the same background as the pledge window's (white by
default) and the ticker logo should have the same color as the ticker
field (RGB values 0,0,80 by default). However, colors can be user
defined as described above.

Adding Slides
-------------

Slides that are to be displayed during times of low pledge activity
should be put in a slides subdirectory of the directory containing
phonepledge.jar. The slides should be image files (most common formats
work) and they will be shown in the order of their file names. So, in
order to force a specific order it is convenient to name the files,
e.g., 1.jpg, 2.jpg, etc.

Make sure the slide images are of a decent resolution and quality so
that they look good on a big screen. PhonePledge will rescale them
automatically to fit the display area, so they can be quite
large. However, do not make them unnecessarily large, as PhonePledge
loads them dynamically and may slow down in case they are really huge.

Keyboard Shortcuts
------------------

Control window:

* a		- accept selected pledge
* r 		- reject selected pledge
* up/down arrow	- Move up/down in pledge list

Display window:

* ctrl/cmd-f	- Toggle full-screen (software mode)
* ctrl/cmd-g	- Toggle full-screen (hardware mode)

NOTE on full-screen modes:

Hardware mode works well on single screen displays. However, on
multi-screen displays, the hardware mode (with full acceleration)
causes one display to be black. This is because hardware mode can only
accelerate one display at a time. Therefore, software mode is more
useful on multi-screen displays.

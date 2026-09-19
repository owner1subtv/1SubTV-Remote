# Remote v2 implementation

1SubTV Remote targets Android TV Remote Service v2.

Implemented in the app shell:
- DNS-SD discovery for `_androidtvremote2._tcp.`
- customer-facing Find & Connect flow
- microphone permission and Voice Search control
- branded remote surface and non-blocking promotion area

Transport work:
- TLS pairing on port 6467 with the PIN shown by the TV
- persist client certificate/private key in Android Keystore-backed storage
- mTLS remote channel on port 6466
- protobuf framing for D-pad, Home, Back, volume, mute and media keys
- reconnect using saved credentials
- text/IME injection
- voice session using PCM 16-bit mono 8 kHz microphone audio

Protocol references:
- Android TV Remote protocol v2 implementations such as tronikos/androidtvremote2
- ScreenCast's Kotlin Android TV protocol implementation

Voice must be tested on physical onn./Google TV hardware because support can vary with the installed Android TV Remote Service version.

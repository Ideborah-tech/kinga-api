# Demo video narration script

For `kinga-demo.mp4` (1 min 36 s). One person reads it in her own voice; the recording is then merged into the video.

## How to record (about 10 minutes)

1. Use a phone voice recorder in a quiet room (closets and bedrooms sound better than empty rooms). Hold the phone about 20 cm from your mouth.
2. Play `kinga-demo.mp4` on a laptop with the sound off, start recording, and read each line when its section appears on screen. The captions in the video show the same idea, so you always know where you are.
3. Speak calmly and a little slower than normal. Small pauses are fine; the timing can be adjusted afterwards.
4. Record it in one take if you can. If you make a mistake, pause, then repeat the whole line.
5. Send the audio file (any format: m4a, mp3, wav, ogg) to be merged into the video.

## Script

| Time | On screen | Say |
|---|---|---|
| 0:00 – 0:05 | Sign-in page | "KingaAPI. It's them, not you." |
| 0:05 – 0:14 | Overview, all green | "This developer depends on M-Pesa, MTN MoMo, Flutterwave and Paystack. Everything is working." |
| 0:14 – 0:20 | Demo control | "Now M-Pesa goes down, for everyone." |
| 0:20 – 0:30 | Purple "It's them" card | "Six of six M-Pesa monitors, from four developers, fail at the same time. So KingaAPI says: it's them, not you." |
| 0:30 – 0:44 | Failure report | "The failure report: we expected HTTP 200, and got 503. It shows the response, and what to do next: retry with backoff, and queue the payments." |
| 0:44 – 0:53 | Demo control | "A different story. This time, only my API key is wrong." |
| 0:53 – 1:06 | Red "Probably your setup" report | "Only my monitor fails. Four other MTN monitors are healthy, so KingaAPI says: probably your setup, and points to the credentials." |
| 1:06 – 1:14 | Demo control | "The scariest failure: Paystack still answers 200, but the fields changed." |
| 1:14 – 1:24 | Report with missing fields | "KingaAPI lists exactly which fields disappeared, before customers notice." |
| 1:24 – 1:32 | Shared setups | "Developers share ready-made setups for African providers. One click, and you are monitoring." |
| 1:32 – 1:36 | Overview | "KingaAPI. Kinga means protection." |

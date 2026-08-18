"""Capture raw IR timing from the sbcshop PiBeam IR transceiver.

Run this on the PiBeam (RP2040 + MicroPython) via Thonny. Point the remote
at the PiBeam's IR receiver (GP1) and press a button; the raw mark/space
durations (microseconds) are printed to the console, ready to be decoded and
ported into `LgAcProtocol.kt`.

No library files are required - only MicroPython built-ins.
"""

from machine import Pin
from utime import ticks_us, ticks_diff, sleep_ms

RX = Pin(1, Pin.IN)  # GP1 = IR receiver on PiBeam
edges = []


def cb(_pin):
    edges.append(ticks_us())


RX.irq(trigger=Pin.IRQ_FALLING | Pin.IRQ_RISING, handler=cb)

print("Point the Friedrich remote at the PiBeam and press a button...")

while True:
    # 100 ms of silence marks the end of a burst (data + repeat frames).
    if edges and ticks_diff(ticks_us(), edges[-1]) > 100000:
        burst = [ticks_diff(edges[i], edges[i - 1]) for i in range(1, len(edges))]
        print("-----")
        print(burst)
        print("-----")
        edges.clear()
    sleep_ms(10)

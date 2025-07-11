# scan_gforce.py
import asyncio
from bleak import BleakScanner

async def main():
    devices = await BleakScanner.discover(timeout=5)
    for d in devices:
        if "gForce" in (d.name or ""):           # filtra solo brazaletes
            # usa rssi si está; si no, muestra "?"
            rssi = getattr(d, "rssi", "?")
            print(f"{d.address}  {d.name}  RSSI:{rssi}")

asyncio.run(main())

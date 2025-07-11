#!/usr/bin/env python3
import os
import time
import asyncio
import struct
import io
import numpy as np
import pandas as pd
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes
from gforce import DataNotifFlags, GForceProfile, NotifDataType

# Clave AES-256 (hex en variable de entorno AES256_KEY)
AES_KEY = bytes.fromhex(os.environ["AES256_KEY"])

# — Paráetros de tu experimento —
ADDRESS   = "A3788DFC-71F1-8839-BEB3-A5F355EBFFE2"
SUBJECT   = "01"
GESTURE   = "COMER"
REPS      = 5

PHASES = [
    # Definir fases si es necesario...
]

async def wait_disconnected(client, timeout=5.0):
    """Espera hasta que client.is_connected sea False o expire el timeout."""
    start = time.time()
    while getattr(client, "is_connected", True) and (time.time() - start) < timeout:
        await asyncio.sleep(0.1)

async def capture_session():
    # 0) Silence BLE disconnect TypeError
    loop = asyncio.get_event_loop()
    def handler(loop, context):
        exc = context.get("exception")
        if isinstance(exc, TypeError) and "handle_disconnect" in str(exc):
            return
        loop.default_exception_handler(context)
    loop.set_exception_handler(handler)

    prof = GForceProfile()
    client = prof.client
    buf_emg = []
    buf_acc = []
    buf_gyr = []
    buf_mag = []
    buf_quat = []

    try:
        # 1) Conectar BLE
        await prof.connect(ADDRESS)
        await asyncio.sleep(1.0)

        # 2) Configurar notificaciones de datos
        for data_type in [NotifDataType.EMG, NotifDataType.ACC, NotifDataType.GYR, NotifDataType.MAG, NotifDataType.QUAT]:
            await prof.startDataNotification(data_type)

        # 3) Empezar a notificar
        await safe(client.start_notify, prof.notifyCharacteristic, prof.data_notification_handler)

        # 4) Capturar fases
        for rep in range(1, REPS + 1):
            dir_path = os.path.join(
                "output",
                SUBJECT,
                GESTURE,
                f"signo{GESTURE}",
                f"rep{rep:02d}"
            )
            os.makedirs(dir_path, exist_ok=True)

            # Guardar EMG
            df_emg = pd.DataFrame(buf_emg, columns=["ts"] + [f"emg_{i}" for i in range(8)])
            buf = io.BytesIO()
            df_emg.to_parquet(buf)
            data_bytes = buf.getvalue()
            nonce = get_random_bytes(12)
            cipher = AES.new(AES_KEY, AES.MODE_GCM, nonce=nonce)
            ciphertext, tag = cipher.encrypt_and_digest(data_bytes)
            with open(os.path.join(dir_path, "emg.parquet.enc"), "wb") as f:
                f.write(nonce)
                f.write(tag)
                f.write(ciphertext)
            buf.close()

            # Guardar ACC
            df_acc = pd.DataFrame(buf_acc, columns=["ts","ax","ay","az"])
            buf = io.BytesIO()
            df_acc.to_parquet(buf)
            data_bytes = buf.getvalue()
            nonce = get_random_bytes(12)
            cipher = AES.new(AES_KEY, AES.MODE_GCM, nonce=nonce)
            ciphertext, tag = cipher.encrypt_and_digest(data_bytes)
            with open(os.path.join(dir_path, "acc.parquet.enc"), "wb") as f:
                f.write(nonce)
                f.write(tag)
                f.write(ciphertext)
            buf.close()

            # Guardar GYR
            df_gyr = pd.DataFrame(buf_gyr, columns=["ts","gx","gy","gz"])
            buf = io.BytesIO()
            df_gyr.to_parquet(buf)
            data_bytes = buf.getvalue()
            nonce = get_random_bytes(12)
            cipher = AES.new(AES_KEY, AES.MODE_GCM, nonce=nonce)
            ciphertext, tag = cipher.encrypt_and_digest(data_bytes)
            with open(os.path.join(dir_path, "gyr.parquet.enc"), "wb") as f:
                f.write(nonce)
                f.write(tag)
                f.write(ciphertext)
            buf.close()

            # Guardar MAG
            df_mag = pd.DataFrame(buf_mag, columns=["ts","mx","my","mz"])
            buf = io.BytesIO()
            df_mag.to_parquet(buf)
            data_bytes = buf.getvalue()
            nonce = get_random_bytes(12)
            cipher = AES.new(AES_KEY, AES.MODE_GCM, nonce=nonce)
            ciphertext, tag = cipher.encrypt_and_digest(data_bytes)
            with open(os.path.join(dir_path, "mag.parquet.enc"), "wb") as f:
                f.write(nonce)
                f.write(tag)
                f.write(ciphertext)
            buf.close()

            # Guardar QUAT
            df_quat = pd.DataFrame(buf_quat, columns=["ts","qw","qx","qy","qz"])
            buf = io.BytesIO()
            df_quat.to_parquet(buf)
            data_bytes = buf.getvalue()
            nonce = get_random_bytes(12)
            cipher = AES.new(AES_KEY, AES.MODE_GCM, nonce=nonce)
            ciphertext, tag = cipher.encrypt_and_digest(data_bytes)
            with open(os.path.join(dir_path, "quat.parquet.enc"), "wb") as f:
                f.write(nonce)
                f.write(tag)
                f.write(ciphertext)
            buf.close()

            print(f"✔ Datos cifrados y guardados en {dir_path}/")

            # Limpiar buffers para la siguiente repetición
            buf_emg.clear(); buf_acc.clear()
            buf_gyr.clear(); buf_mag.clear(); buf_quat.clear()

    finally:
        # 5) Cleanup robusto
        async def safe(fn, *args):
            try:
                await asyncio.wait_for(fn(*args), timeout=3.0)
            except Exception:
                pass

        # Desuscribir y parar notificaciones
        await safe(client.stop_notify, prof.notifyCharacteristic)
        await safe(prof.stopDataNotification)

        # Desconectar BleakClient y esperar desconexión
        await safe(client.disconnect)
        await wait_disconnected(client, timeout=3.0)

        # Desconectar perfil
        await safe(prof.disconnect)

        print("\nDesconectado limpiamente ✅")

if __name__ == "__main__":
    asyncio.run(capture_session())

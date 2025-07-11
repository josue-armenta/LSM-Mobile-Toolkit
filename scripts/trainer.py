"""
trainer.py - Pipeline sin sliding-windows + CNN híbrida (**EfficientNet-lite**)  
▸ Target HW 64 GB RAM + RTX 40-series (12 GB VRAM).
▸ ENTRENAMIENTO float32; exporta solo TFLite INT8 (full-integer) para Android.
▸ .repeat() + steps_per_epoch → épocas estables.
▸ Lote 64 (~2.5 GB VRAM) - ajustable con flag --batch.
▸ Sustituye VGG16-lite por **EfficientNetB0-lite** (weights=None) + capa 1x1 que reduce canales 63→3.
▸ **Checklist tf.data aplicado** (cache→shuffle→batch→repeat→slack→prefetch).
"""

import os, random, warnings, math, argparse
import numpy as np
import pandas as pd
import tensorflow as tf
from scipy.signal import butter, filtfilt, iirnotch, detrend, hilbert
from scipy.interpolate import interp1d
from pyts.approximation import PiecewiseAggregateApproximation
from pyts.image import GramianAngularField
from tensorflow.keras import layers, models, callbacks
from tensorflow.keras.applications import EfficientNetB0

warnings.filterwarnings("ignore", category=FutureWarning)

# ——— Argumentos CLI ———
parser = argparse.ArgumentParser()
parser.add_argument("--batch", type=int, default=64, help="Tamaño de lote")
parser.add_argument("--epochs", type=int, default=25)
ARGS = parser.parse_args()

# ——— Constantes ———
DATA_DIR = "data/"
SENSOR_CHANNELS = {"acc":3, "gyr":3, "mag":3, "quat":4, "emg":8}
FS = {k:50 for k in SENSOR_CHANNELS}; FS["emg"] = 938
BAND = {k:(0.5,20) for k in SENSOR_CHANNELS}; BAND["emg"] = (20,450)
NOTCH_F, Q = 50, 30
IMG = 128
PAA = PiecewiseAggregateApproximation(output_size=IMG)
GAF = GramianAngularField(image_size=IMG, method="s")
BATCH, EPOCHS = ARGS.batch, ARGS.epochs
TEST_R, VAL_R = 0.1, 0.2
SHUF = 1024
EXPECT_CH = sum(SENSOR_CHANNELS.values()) * 3  # 63 canales RGB

# ——— GPU ———
GPUS = tf.config.list_physical_devices('GPU')
for g in GPUS:
    tf.config.experimental.set_memory_growth(g, True)
print(f"[INFO] GPU detectadas: {len(GPUS)} – entrenamiento float32")

# ——— Funciones de preprocesado ———

def _band(x, fs, lo, hi):
    b, a = butter(4, [lo/(fs/2), hi/(fs/2)], btype='band')
    return filtfilt(b, a, x)

def _notch(x, fs):
    b, a = iirnotch(NOTCH_F/(fs/2), Q)
    return filtfilt(b, a, x)

def preprocess(arr, sensor):
    fs = FS[sensor]
    arr = detrend(arr, axis=1)
    for i in range(arr.shape[0]):
        arr[i] = _band(arr[i], fs, *BAND[sensor])
    if sensor == 'emg':
        for i in range(arr.shape[0]):
            arr[i] = _notch(arr[i], fs)
        arr = np.abs(hilbert(arr, axis=1))
    mu = arr.mean(axis=1, keepdims=True)
    sd = arr.std(axis=1, keepdims=True) + 1e-6
    return (arr - mu) / sd

# ——— Carga de una repetición ———

def load_rep(pt: str, sign: str, rep: int, L_min=IMG):
    base = os.path.join(DATA_DIR, pt, sign, f"rep{rep:02d}")
    raws, Ls = {}, []
    for s in SENSOR_CHANNELS:
        df = pd.read_parquet(os.path.join(base, f"{s}.parquet"))
        arr = df.drop(columns=['rel_timestamp']).values.T
        raws[s] = arr; Ls.append(arr.shape[1])
    L = max(max(Ls), L_min)
    out = []
    for s, arr in raws.items():
        t_old = np.arange(arr.shape[1])
        t_new = np.linspace(0, arr.shape[1]-1, L)
        interp = np.vstack([
            interp1d(t_old, arr[i], kind='cubic', fill_value='extrapolate')(t_new)
            for i in range(arr.shape[0])
        ])
        out.append(preprocess(interp, s))
    return np.vstack(out)

# ——— Listado de datos ———
pts = [d for d in os.listdir(DATA_DIR) if os.path.isdir(os.path.join(DATA_DIR, d))]
signs = sorted({s for p in pts for s in os.listdir(os.path.join(DATA_DIR, p))
                if os.path.isdir(os.path.join(DATA_DIR, p, s))})
label_map = {s:i for i,s in enumerate(signs)}
ALL = [(p,s,r) for p in pts for s in signs for r in range(1,6)]
random.Random(42).shuffle(ALL)
nt = int(len(ALL)*TEST_R); nv = int(len(ALL)*VAL_R)
TEST, VAL, TRAIN = ALL[:nt], ALL[nt:nt+nv], ALL[nt+nv:]
print(f"Total repeticiones: {len(ALL)} → train {len(TRAIN)}, val {len(VAL)}, test {len(TEST)}")

# ——— tf.data pipeline ———

sig = (tf.TensorSpec((IMG, IMG, EXPECT_CH), tf.float32), tf.TensorSpec((), tf.int32))


def gen(examples):
    for pt, sign, rep in examples:
        try:
            mat = load_rep(pt, sign, rep)
            paa = PAA.transform(mat)
            channels = [np.stack([GAF.transform(row[np.newaxis,:])[0]]*3, axis=2) for row in paa]
            img = np.concatenate(channels, axis=2).astype(np.float32)
            if img.shape[2] == EXPECT_CH:
                yield img, np.int32(label_map[sign])
        except Exception as e:
            print("[WARN] skip", pt, sign, rep, "→", e)
            continue


def make_ds(examples, cache_path=None):
    ds = tf.data.Dataset.from_generator(lambda: gen(examples), output_signature=sig)
    ds = ds.cache(cache_path) if cache_path else ds.cache()
    ds = ds.shuffle(SHUF)
    ds = ds.batch(BATCH, drop_remainder=True)
    ds = ds.repeat()
    opts = tf.data.Options()
    opts.experimental_deterministic = False
    opts.experimental_slack = True
    ds = ds.with_options(opts)
    ds = ds.prefetch(tf.data.AUTOTUNE)
    return ds

train_ds = make_ds(TRAIN)
val_ds   = make_ds(VAL)
test_ds  = make_ds(TEST)
steps_per_epoch = math.ceil(len(TRAIN)/BATCH)
val_steps       = math.ceil(len(VAL)/BATCH)

# ——— Modelo (EfficientNet-lite) ———

def build_model(input_shape, n_cls):
    inp = layers.Input(shape=input_shape)
    # Reducir canales 63→3 para EfficientNet
    x = layers.Conv2D(3, 1, activation='relu', padding='same')(inp)
    base = EfficientNetB0(include_top=False, weights=None, input_tensor=x, pooling='avg')
    x = base.output
    x = layers.Dense(128, activation='relu')(x)
    x = layers.Dropout(0.5)(x)
    out = layers.Dense(n_cls, activation='softmax')(x)
    model = models.Model(inp, out)
    model.compile('adam', 'sparse_categorical_crossentropy', metrics=['sparse_categorical_accuracy'])
    return model

model = build_model((IMG, IMG, EXPECT_CH), len(signs))
model.summary()

cb = callbacks.EarlyStopping(monitor='val_sparse_categorical_accuracy', patience=15, restore_best_weights=True)
model.fit(train_ds,
          validation_data=val_ds,
          steps_per_epoch=steps_per_epoch,
          validation_steps=val_steps,
          epochs=EPOCHS,
          callbacks=[cb])

print('Evaluando…')
loss, acc = model.evaluate(test_ds, steps=math.ceil(len(TEST)/BATCH))
print(f'Test accuracy: {acc:.4f}')

# ——— Export SavedModel ———
model.export('saved_model')

# ——— Conversión INT8 ———

def rep_ds():
    for img, _ in train_ds.take(128):
        yield [tf.cast(img, tf.float32)]

converter = tf.lite.TFLiteConverter.from_saved_model('saved_model')
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = rep_ds
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.int8
converter.inference_output_type = tf.int8
open('gesture_int8.tflite', 'wb').write(converter.convert())
print('[TFLite] modelo INT8 listo ✔')

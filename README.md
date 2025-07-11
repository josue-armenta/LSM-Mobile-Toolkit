# Reconocimiento de gestos con brazalete gForce Pro+ 

&#x20;  &#x20;

## Tabla de contenidos

- [Descripción general](#descripción-general)
- [Características](#características)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Requisitos previos](#requisitos-previos)
- [Instalación](#instalación)
- [Uso](#uso)
  - [Aplicación Android](#aplicación-android)
  - [Recolección y cifrado de datos](#recolección-y-cifrado-de-datos)
  - [Inferencia de modelos](#inferencia-de-modelos)
- [Conjunto de datos](#conjunto-de-datos)
- [Modelos](#modelos)
- [Contribuciones](#contribuciones)
- [Licencia](#licencia)

## Descripción general

Este proyecto de tesis provee un sistema completo para la adquisición, preprocesamiento y clasificación de señales multimodales obtenidas vía el brazalete **Oymotion gForce Pro+**. Incluye:

- Aplicación Android para captura en tiempo real.
- Scripts en Python para conexión BLE y cifrado AES-256.
- Conjunto de datos estructurado de 13 participantes y 35 gestos.
- Modelos TFLite INT8 (GADF, GASF, MTF) optimizados para dispositivos móviles.

## Características

- Captura simultánea de:
  - Acelerómetro (3 canales @50 Hz)
  - Giroscopio (3 canales @50 Hz)
  - Magnetómetro (3 canales @40 Hz)
  - Cuaterniones (4 canales @50 Hz)
  - EMG de superficie (8 canales @1 kHz)
- Cifrado seguro de los datos con AES-256.
- Modelos cuantizados a INT8 para clasificación eficiente.

## Estructura del repositorio

```
├── android/         # Proyecto Android Studio
├── data/            # Datos crudos organizados por participante, gesto y repetición
├── models/          # Modelos TFLite cuantizados (INT8)
├── scripts/         # Scripts de captura y utilidades BLE
└── README.md        # Documentación del proyecto
```

### android/

Código fuente de la aplicación Android encargada de conectarse al brazalete, mostrar y enviar las señales.

### data/

Conjunto de datos en crudo:

```
data/participante{01}/signo{signo}/rep{01}/{sensor}.parquet
```

- **Participantes**: 13 usuarios
- **Gestos**: 35 signos diferentes
- **Repeticiones**: 10 por gesto
- **Sensores**:
  - `acc.parquet`: acelerómetro (3 canales, 50 Hz)
  - `gyr.parquet`: giroscopio (3 canales, 50 Hz)
  - `mag.parquet`: magnetómetro (3 canales, 40 Hz)
  - `quat.parquet`: cuaterniones (4 canales, 50 Hz)
  - `emg.parquet`: EMG (8 canales, 1 kHz)

### models/

Contiene los tres modelos TFLite cuantizados en INT8:

- **GADF** – Gramian Angular Difference Field
- **GASF** – Gramian Angular Summation Field
- **MTF**  – Markov Transition Field

El tensor de entrada debe tener forma `128×128×63` (21 canales de sensores × 3 canales RGB).

### scripts/

- `scan_gforce.py`: escanea y muestra la MAC del brazalete para conexión BLE.
- `capture.py`: captura señales en tiempo real y las cifra con AES-256, apoyado en `gforce.py`.

## Requisitos previos

- **Android Studio** 4.0+ con SDK de Android instalado.
- **Python** 3.8 o superior.
- Paquetes Python (instalables desde `requirements.txt`):
  - `bleak`
  - `pycryptodome`
  - `pandas`
  - `numpy`

## Instalación

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/josue-armenta/LSM-Mobile-Toolkit.git
   cd LSM-Mobile-Toolkit
   ```
2. Configurar entorno virtual Python:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```
3. Abrir el proyecto Android:
   - Importar la carpeta `android/` en Android Studio y compilar.

## Uso

### Aplicación Android

1. Conectar el brazalete gForce Pro+ por BLE.
2. Ejecutar la app desde Android Studio o instalar el APK.
3. Visualizar y almacenar las señales o transmitirlas al PC.

### Recolección y cifrado de datos

Obtener la dirección MAC:

```bash
python scripts/scan_gforce.py
```

Capturar y cifrar señales:

```bash
python scripts/capture.py
```

### Inferencia de modelos

```python
import tensorflow as tf

interpreter = tf.lite.Interpreter(model_path="models/GADF.tflite")
interpreter.allocate_tensors()
# Preparar el tensor de entrada de forma (128,128,63)
# Ejecutar inferencia y leer la etiqueta más probable
```

## Conjunto de datos

Ver [Sección ](#estructura-del-repositorio)[`data/`](#estructura-del-repositorio) para detalles de organización y formatos de archivo.

## Modelos

Descripción y parámetros de los modelos en la carpeta `models/`.

## Contribuciones

¡Bienvenidas! Para contribuir:

1. Hacer fork del repositorio.
2. Crear una rama.
3. Hacer commits con descripciones claras.
4. Enviar un Pull Request.

## Licencia

Este proyecto está bajo Licencia MIT. Consulte [LICENSE](LICENSE).
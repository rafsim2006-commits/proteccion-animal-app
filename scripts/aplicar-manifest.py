#!/usr/bin/env python3
import sys

ruta = sys.argv[1]
with open(ruta, "r", encoding="utf-8") as f:
    contenido = f.read()

permisos = """    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

"""

if "android.permission.CAMERA" in contenido:
    print("Los permisos ya estaban presentes.")
else:
    marcador = "<application"
    posicion = contenido.find(marcador)
    if posicion == -1:
        print("ERROR: no se encontro la etiqueta <application>")
        sys.exit(1)
    contenido = contenido[:posicion] + permisos + contenido[posicion:]
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(contenido)
    print("Permisos insertados correctamente.")

if "<queries>" not in contenido:
    queries = """
    <queries>
        <intent>
            <action android:name="android.media.action.IMAGE_CAPTURE" />
        </intent>
        <intent>
            <action android:name="android.intent.action.GET_CONTENT" />
            <data android:mimeType="image/*" />
        </intent>
    </queries>
</manifest>"""
    contenido = contenido.replace("</manifest>", queries)
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(contenido)
    print("Bloque <queries> agregado.")

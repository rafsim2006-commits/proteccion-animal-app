#!/usr/bin/env python3
import os
import shutil
import sys

raiz_app = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
logo = os.path.join(raiz_app, "www", "logo.png")
if not os.path.isfile(logo):
    print("ERROR: no existe www/logo.png")
    sys.exit(1)

res = os.path.join(raiz_app, "android", "app", "src", "main", "res")
densidades = [
    "mipmap-mdpi",
    "mipmap-hdpi",
    "mipmap-xhdpi",
    "mipmap-xxhdpi",
    "mipmap-xxxhdpi",
]

for carpeta in densidades:
    destino = os.path.join(res, carpeta)
    os.makedirs(destino, exist_ok=True)
    for nombre in ("ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png"):
        shutil.copyfile(logo, os.path.join(destino, nombre))

anydpi = os.path.join(res, "mipmap-anydpi-v26")
if os.path.isdir(anydpi):
    for nombre in ("ic_launcher.xml", "ic_launcher_round.xml"):
        ruta = os.path.join(anydpi, nombre)
        if os.path.isfile(ruta):
            os.remove(ruta)
            print("Eliminado icono adaptativo:", nombre)

file_paths_src = os.path.join(
    raiz_app, "android_override", "app", "src", "main", "res", "xml", "file_paths.xml"
)
file_paths_dst = os.path.join(res, "xml", "file_paths.xml")
if os.path.isfile(file_paths_src):
    os.makedirs(os.path.dirname(file_paths_dst), exist_ok=True)
    shutil.copyfile(file_paths_src, file_paths_dst)
    print("file_paths.xml copiado.")

print("Icono de la Alcaldia aplicado.")

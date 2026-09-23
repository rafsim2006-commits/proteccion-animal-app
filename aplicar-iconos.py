#!/usr/bin/env python3
"""Fuerza el logo de la Alcaldia como icono de la app (sin icono adaptativo de Capacitor)."""
import os
import re
import shutil
import subprocess
import sys
import urllib.request

raiz_app = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
www = os.path.join(raiz_app, "www")
logo = None
logo_url = "https://alcaldiadeplaza.com/proteccion_animal/assets/img/logo.png"
candidatos = (
    "proteccionanimal.jpg",
    "proteccionanimal.jpeg",
    "proteccionanimal.png",
    "logo.png",
    "logo.jpg",
)
tamaños = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def instalar_pillow():
    try:
        from PIL import Image  # noqa: F401
        return
    except ImportError:
        pass
    print("Instalando Pillow...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "--user", "pillow"])


def asegurar_logo():
    global logo
    os.makedirs(www, exist_ok=True)
    for nombre in candidatos:
        ruta = os.path.join(www, nombre)
        if os.path.isfile(ruta) and os.path.getsize(ruta) > 1000:
            logo = ruta
            print("Logo local:", logo, os.path.getsize(logo), "bytes")
            return
    logo = os.path.join(www, "proteccionanimal.jpg")
    print("Descargando logo...")
    urllib.request.urlretrieve(logo_url, logo)
    if not os.path.isfile(logo) or os.path.getsize(logo) < 1000:
        print("ERROR: no se encontro proteccionanimal.jpg ni logo.png en www")
        sys.exit(1)


def hacer_cuadrado(px):
    from PIL import Image

    img = Image.open(logo).convert("RGBA")
    canvas = Image.new("RGBA", (px, px), (255, 255, 255, 255))
    margen = int(px * 0.12)
    disponible = px - margen * 2
    copia = img.copy()
    copia.thumbnail((disponible, disponible), Image.LANCZOS)
    x = (px - copia.size[0]) // 2
    y = (px - copia.size[1]) // 2
    canvas.paste(copia, (x, y), copia)
    return canvas.convert("RGB")


def guardar(img, ruta):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    img.save(ruta, "PNG")
    print("Creado:", ruta, os.path.getsize(ruta), "bytes")


def parchear_manifest(ruta):
    if not os.path.isfile(ruta):
        print("ERROR: no existe AndroidManifest.xml")
        sys.exit(1)
    with open(ruta, "r", encoding="utf-8") as f:
        texto = f.read()

    if "xmlns:tools=" not in texto:
        texto = texto.replace(
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">",
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            "    xmlns:tools=\"http://schemas.android.com/tools\">",
        )

    texto = re.sub(
        r'android:icon="[^"]*"',
        'android:icon="@drawable/logo_alcaldia"',
        texto,
        count=1,
    )
    if 'android:roundIcon=' in texto:
        texto = re.sub(
            r'android:roundIcon="[^"]*"',
            'android:roundIcon="@drawable/logo_alcaldia"',
            texto,
            count=1,
        )
    else:
        texto = texto.replace(
            'android:icon="@drawable/logo_alcaldia"',
            'android:icon="@drawable/logo_alcaldia"\n        android:roundIcon="@drawable/logo_alcaldia"',
            1,
        )

    if "tools:replace=" not in texto:
        texto = texto.replace(
            "<application",
            '<application\n        tools:replace="android:icon,android:roundIcon"',
            1,
        )

    with open(ruta, "w", encoding="utf-8") as f:
        f.write(texto)
    print("AndroidManifest: icono forzado a @drawable/logo_alcaldia")


instalar_pillow()
asegurar_logo()

res = os.path.join(raiz_app, "android", "app", "src", "main", "res")
if not os.path.isdir(res):
    print("ERROR: no existe", res)
    sys.exit(1)

for nombre_dir in list(os.listdir(res)):
    carpeta = os.path.join(res, nombre_dir)
    if not os.path.isdir(carpeta):
        continue
    if "anydpi" in nombre_dir:
        shutil.rmtree(carpeta)
        print("Eliminada carpeta adaptativa:", nombre_dir)
        continue
    if nombre_dir.startswith("mipmap") or nombre_dir.startswith("drawable"):
        for archivo in list(os.listdir(carpeta)):
            if archivo.startswith("ic_launcher"):
                os.remove(os.path.join(carpeta, archivo))
                print("Eliminado:", nombre_dir + "/" + archivo)

icono_grande = hacer_cuadrado(512)
guardar(icono_grande, os.path.join(res, "drawable", "logo_alcaldia.png"))

for carpeta, px in tamaños.items():
    destino = os.path.join(res, carpeta)
    os.makedirs(destino, exist_ok=True)
    img = hacer_cuadrado(px)
    for nombre in ("ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png"):
        guardar(img, os.path.join(destino, nombre))

file_paths_src = os.path.join(
    raiz_app, "android_override", "app", "src", "main", "res", "xml", "file_paths.xml"
)
file_paths_dst = os.path.join(res, "xml", "file_paths.xml")
if os.path.isfile(file_paths_src):
    os.makedirs(os.path.dirname(file_paths_dst), exist_ok=True)
    shutil.copyfile(file_paths_src, file_paths_dst)

parchear_manifest(os.path.join(raiz_app, "android", "app", "src", "main", "AndroidManifest.xml"))
print("Icono de la Alcaldia aplicado.")

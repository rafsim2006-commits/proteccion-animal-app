#!/usr/bin/env python3
"""Reemplaza el icono por defecto de Capacitor por el logo de la Alcaldia."""
import os
import shutil
import sys
import urllib.request

raiz_app = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
logo = os.path.join(raiz_app, "www", "logo.png")
logo_url = "https://alcaldiadeplaza.com/proteccion_animal/assets/img/logo.png"


def asegurar_logo():
    if os.path.isfile(logo) and os.path.getsize(logo) > 1000:
        print("Logo local encontrado:", logo, os.path.getsize(logo), "bytes")
        return
    os.makedirs(os.path.dirname(logo), exist_ok=True)
    print("Descargando logo desde el hosting...")
    try:
        urllib.request.urlretrieve(logo_url, logo)
    except Exception as exc:
        print("ERROR: no se pudo obtener el logo:", exc)
        sys.exit(1)
    if not os.path.isfile(logo) or os.path.getsize(logo) < 1000:
        print("ERROR: el archivo de logo esta vacio o danado")
        sys.exit(1)
    print("Logo descargado:", os.path.getsize(logo), "bytes")


def preparar_cuadrado(origen):
    """Crea un PNG cuadrado con fondo blanco para que el icono no se recorte."""
    destino = os.path.join(raiz_app, "www", "logo_icono.png")
    try:
        from PIL import Image
    except ImportError:
        print("Pillow no esta instalado; se usara el PNG original.")
        return origen

    img = Image.open(origen).convert("RGBA")
    lado = max(img.size[0], img.size[1], 512)
    canvas = Image.new("RGBA", (lado, lado), (255, 255, 255, 255))
    x = (lado - img.size[0]) // 2
    y = (lado - img.size[1]) // 2
    canvas.paste(img, (x, y), img)
    canvas = canvas.resize((512, 512), Image.LANCZOS)
    canvas.save(destino, "PNG")
    print("Icono cuadrado generado: 512x512")
    return destino


def borrar_si_existe(ruta):
    if os.path.isfile(ruta):
        os.remove(ruta)
        print("Eliminado:", ruta)


def escribir(ruta, contenido):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(contenido)


asegurar_logo()
icono = preparar_cuadrado(logo)

res = os.path.join(raiz_app, "android", "app", "src", "main", "res")
if not os.path.isdir(res):
    print("ERROR: no existe android/app/src/main/res")
    sys.exit(1)

nombres_icono = (
    "ic_launcher.png",
    "ic_launcher_round.png",
    "ic_launcher_foreground.png",
    "ic_launcher.webp",
    "ic_launcher_round.webp",
    "ic_launcher_foreground.webp",
    "ic_launcher.xml",
    "ic_launcher_round.xml",
    "ic_launcher_foreground.xml",
    "ic_launcher_background.xml",
    "ic_launcher_background.png",
    "ic_launcher_background.webp",
)

for nombre_dir in os.listdir(res):
    carpeta = os.path.join(res, nombre_dir)
    if not os.path.isdir(carpeta):
        continue
    if not (
        nombre_dir.startswith("mipmap")
        or nombre_dir.startswith("drawable")
    ):
        continue
    for archivo in os.listdir(carpeta):
        if archivo in nombres_icono or archivo.startswith("ic_launcher"):
            borrar_si_existe(os.path.join(carpeta, archivo))

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
        shutil.copyfile(icono, os.path.join(destino, nombre))

drawable = os.path.join(res, "drawable")
os.makedirs(drawable, exist_ok=True)
shutil.copyfile(icono, os.path.join(drawable, "logo_alcaldia.png"))

escribir(
    os.path.join(drawable, "ic_launcher_bg.xml"),
    """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FFFFFF"/>
</shape>
""",
)

escribir(
    os.path.join(drawable, "ic_launcher_fg.xml"),
    """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:top="14dp"
        android:bottom="14dp"
        android:left="14dp"
        android:right="14dp">
        <bitmap
            android:gravity="fill"
            android:src="@drawable/logo_alcaldia" />
    </item>
</layer-list>
""",
)

adaptativo = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_bg"/>
    <foreground android:drawable="@drawable/ic_launcher_fg"/>
    <monochrome android:drawable="@drawable/ic_launcher_fg"/>
</adaptive-icon>
"""
anydpi = os.path.join(res, "mipmap-anydpi-v26")
escribir(os.path.join(anydpi, "ic_launcher.xml"), adaptativo)
escribir(os.path.join(anydpi, "ic_launcher_round.xml"), adaptativo)

file_paths_src = os.path.join(
    raiz_app, "android_override", "app", "src", "main", "res", "xml", "file_paths.xml"
)
file_paths_dst = os.path.join(res, "xml", "file_paths.xml")
if os.path.isfile(file_paths_src):
    os.makedirs(os.path.dirname(file_paths_dst), exist_ok=True)
    shutil.copyfile(file_paths_src, file_paths_dst)
    print("file_paths.xml copiado.")

print("Icono de la Alcaldia aplicado.")

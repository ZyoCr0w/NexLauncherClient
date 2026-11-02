"""Genera cosmeticos.json: el catálogo de cosméticos 3D de NexLauncher (lo leen NexClient y el launcher).

Espacio de modelo de Minecraft: píxeles, Y hacia abajo, origen en el cuello, el frente hacia -Z (la espalda en +Z).
  cabeza: la cabeza va de y=-8 a 0, x/z de -4 a 4.   cuerpo: y de 0 a 12, x de -4 a 4, z de -2 a 2.
  jugador: igual que cuerpo pero sin girar con él (auras); los pies quedan en y=24.
  escudo: espacio del modelo del escudo (placa de x -6..6, y -11..11, z -2..-1).
python3 generar.py  ->  cosmeticos.json
"""
import json
import os

import festivos
import nuevos


def anim(**k):
    return k


# ---------------------------------------------------------------- cabeza
def aureola(color=None, arcoiris=False, oscura=False):
    piezas = []
    if oscura:
        piezas.append({"anillo": {"radio": 4.6, "n": 22, "tam": [1.5, 0.55, 1.0]}, "color": "#1C1226"})
        piezas.append({"anillo": {"centro": [0, -0.45, 0], "radio": 4.6, "n": 22, "tam": [1.6, 0.2, 0.5]},
                       "color": "#A259FF", "brillo": True, "parpadeo": {"frec": 1.2, "min": 0.55, "desfase": 0.5}})
    else:
        p = {"anillo": {"radio": 4.6, "n": 22, "tam": [1.5, 0.55, 1.0]}, "brillo": True,
             "parpadeo": {"frec": 1.4, "min": 0.82, "desfase": 0.45}}
        if arcoiris:
            p.update(arcoiris=True, arcoiris_vel=0.35, desfase=1.0 / 22)
        else:
            p["color"] = color
        piezas.append(p)
    return {"hueso": "cabeza", "grupos": [
        {"id": "aureola", "pivote": [0, -11.2, 0], "rot": [8, 0, 0],
         "anim": {"ry": anim(vel=50), "y": anim(amp=0.45, frec=0.55)}, "piezas": piezas}]}


def corona():
    oro, oro2 = "#F2C14E", "#C9962A"
    piezas = [
        {"caja": [-4.6, -8.9, -4.6, 9.2, 1.6, 0.6], "color": oro},
        {"caja": [-4.6, -8.9, 4.0, 9.2, 1.6, 0.6], "color": oro},
        {"caja": [-4.6, -8.9, -4.0, 0.6, 1.6, 8.0], "color": oro},
        {"caja": [4.0, -8.9, -4.0, 0.6, 1.6, 8.0], "color": oro},
    ]
    for x in (-4.0, -0.5, 3.0):  # puntas en cada lado
        for z, ancho in ((-4.6, 0.6), (4.0, 0.6)):
            piezas.append({"caja": [x, -10.6, z, 1.0, 1.8, ancho], "color": oro2})
    for z in (-3.0, 0.5):
        for x in (-4.6, 4.0):
            piezas.append({"caja": [x, -10.6, z, 0.6, 1.8, 1.0], "color": oro2})
    gemas = [("#E53935", -0.6, -4.85), ("#1E88E5", -3.4, -4.85), ("#43A047", 2.2, -4.85), ("#E53935", -0.6, 4.25)]
    for i, (c, x, z) in enumerate(gemas):
        piezas.append({"caja": [x, -8.6, z, 1.2, 1.0, 0.6], "color": c, "brillo": True,
                       "parpadeo": {"frec": 0.7, "min": 0.55, "desfase": i * 1.7}})
    return {"hueso": "cabeza", "grupos": [{"id": "corona", "anim": {"y": anim(amp=0.12, frec=0.5)}, "piezas": piezas}]}


def cuernos():
    return {"hueso": "cabeza", "grupos": [
        {"id": "c1", "espejo": True, "pivote": [2.6, -7.6, -1.2], "rot": [-12, 0, 28],
         "piezas": [{"caja": [-1.1, -2.6, -1.1, 2.2, 2.8, 2.2], "color": "#2E1B1B"}]},
        {"id": "c2", "padre": "c1", "espejo": True, "pivote": [0, -2.5, 0], "rot": [-24, 0, -18],
         "piezas": [{"caja": [-0.8, -2.4, -0.8, 1.6, 2.6, 1.6], "color": "#3D2323"}]},
        {"id": "c3", "padre": "c2", "espejo": True, "pivote": [0, -2.3, 0], "rot": [-30, 0, -20],
         "piezas": [{"caja": [-0.55, -2.0, -0.55, 1.1, 2.2, 1.1], "color": "#FF5A2A", "brillo": True,
                     "parpadeo": {"frec": 0.9, "min": 0.5}}]},
    ]}


def orejas(zorro=False):
    if zorro:
        filas = [".w.", ".o.", "oio", "oio", "ooo"]
        paleta = {"o": "#E8762B", "i": "#FFF3E0", "w": "#FFFFFF"}
        tam, y = 1.15, -5.6
    else:
        filas = [".o.", "oio", "oio", "ooo"]
        paleta = {"o": "#3A3A3A", "i": "#F48FB1"}
        tam, y = 1.1, -4.4
    return {"hueso": "cabeza", "grupos": [
        {"id": "oreja", "espejo": True, "pivote": [2.7, -8.0, -0.3], "rot": [0, 0, 14],
         "anim": {"rz": anim(amp=5, frec=0.22, agachado=-35), "rx": anim(amp=3, frec=0.31, fase=1.0)},
         "piezas": [{"pixeles": {"origen": [-1.3, y, -0.3], "plano": "xy", "filas": filas, "paleta": paleta,
                                 "tam": tam, "prof": 0.6}}]}]}


def nube():
    gris, blanco = "#CFD8DC", "#FFFFFF"
    piezas = [
        {"caja": [-4.5, -14.0, -3.0, 9.0, 2.6, 6.0], "color": gris},
        {"caja": [-3.0, -15.6, -2.2, 4.5, 2.0, 4.5], "color": blanco},
        {"caja": [0.2, -16.2, -2.6, 4.0, 2.6, 5.0], "color": blanco},
        {"caja": [-5.6, -13.4, -2.0, 2.0, 1.8, 4.0], "color": gris},
        {"caja": [3.8, -13.6, -1.8, 2.2, 2.0, 3.6], "color": gris},
    ]
    return {"hueso": "cabeza", "grupos": [{"id": "nube", "anim": {"y": anim(amp=0.5, frec=0.4), "x": anim(amp=0.4, frec=0.25)},
                                           "piezas": piezas}],
            "emisores": [{"tipo": "caida", "n": 12, "area": [-4, 4, -11.5, -1, -2.5, 2.5], "vida": 0.55,
                          "tam": [0.3, 1.3, 0.3], "colores": ["#81D4FA", "#4FC3F7"], "alfa": 0.85, "brillo": False}]}


# ---------------------------------------------------------------- espalda
ALA_ANGEL = [
    "..########.",
    ".##########",
    "###########",
    "#g#g#g#g###",
    ".#########g",
    "..g#g#g####",
    "...########",
    "....g#g#g##",
    ".....######",
    "......#g#g#",
    ".......####",
    "........###",
    ".........##",
]


def alas_plumas(filas, paleta, brillo=False, parpadeo=None, arcoiris=False, rot=(6, -24, -14), ry=None, extra=None,
                tam=1.15):
    p = {"pixeles": {"origen": [0, -5.0, 0], "plano": "xy", "filas": filas, "paleta": paleta, "tam": tam, "prof": 0.6}}
    if brillo:
        p["brillo"] = True
    if parpadeo:
        p["parpadeo"] = parpadeo
    if arcoiris:
        p.update(arcoiris=True, arcoiris_vel=0.3, desfase=0.04)
    piezas = [p] + (extra or [])
    return {"hueso": "cuerpo", "grupos": [
        {"id": "ala", "espejo": True, "pivote": [1.0, 2.0, 2.4], "rot": list(rot),
         "anim": {"ry": ry or anim(amp=9, frec=0.45, amp_mov=0.7, frec_mov=0.55, aire=28),
                  "rz": anim(amp=3, frec=0.45, fase=1.3)},
         "piezas": piezas}]}


def alas_dragon():
    filas = [
        "bbbbbbbbbbbbb",
        "mmmbmmmmbmmmb",
        "mmmbmmmmbmmb.",
        "mmbmmmmbmmb..",
        "mmbmmmbmmmb..",
        "mbmmmbmmmb...",
        "mbmmbmmmb....",
        "bmmbmmmb.....",
        "bmbmmb.......",
        "b.b.b........",
    ]
    return alas_plumas(filas, {"b": "#3E0B0B", "m": "#B3261E"}, rot=(6, -22, -18),
                       ry=anim(amp=13, frec=0.32, amp_mov=0.6, frec_mov=0.4, aire=32))


def alas_mariposa():
    filas = [
        ".#######...",
        "##########.",
        "###########",
        "###########",
        ".#########.",
        "..#######..",
        "...####....",
        "...#####...",
        "..#######..",
        "..######...",
        "...####....",
    ]
    borde = []
    return alas_plumas(filas, {"#": "#FFFFFF"}, arcoiris=True, brillo=False, rot=(0, -20, -6), tam=1.05,
                       ry=anim(amp=26, frec=1.1, amp_mov=0.3, frec_mov=0.5, aire=10), extra=borde)


def alas_fuego():
    filas = [
        "..11111111.",
        ".1222222221",
        "12233333221",
        "12334443321",
        ".1234443321",
        "..123443321",
        "...12343321",
        "....1234321",
        ".....123321",
        "......12321",
        ".......1221",
        "........121",
        ".........11",
    ]
    paleta = {"1": "#FFF176", "2": "#FFB300", "3": "#FF6D00", "4": "#DD2C00"}
    return alas_plumas(filas, paleta, brillo=True, parpadeo={"frec": 5.0, "min": 0.72, "desfase": 0.9})


def jetpack():
    gris, oscuro, rojo = "#4E5D6C", "#22272D", "#E53935"
    piezas = [
        {"caja": [-3.8, 1.0, 2.1, 3.2, 7.5, 2.6], "color": gris},
        {"caja": [0.6, 1.0, 2.1, 3.2, 7.5, 2.6], "color": gris},
        {"caja": [-3.8, 0.4, 2.3, 3.2, 0.8, 2.2], "color": rojo},
        {"caja": [0.6, 0.4, 2.3, 3.2, 0.8, 2.2], "color": rojo},
        {"caja": [-0.6, 2.0, 2.1, 1.2, 5.0, 1.6], "color": oscuro},
        {"caja": [-3.1, 8.5, 2.6, 1.8, 1.0, 1.6], "color": oscuro},
        {"caja": [1.3, 8.5, 2.6, 1.8, 1.0, 1.6], "color": oscuro},
    ]
    for x in (-3.0, 1.4):  # llamas (solo en el aire)
        piezas.append({"caja": [x, 9.5, 2.7, 1.6, 2.6, 1.4], "color": "#FF9800", "brillo": True, "cuando": "aire",
                       "parpadeo": {"frec": 9.0, "min": 0.6}})
        piezas.append({"caja": [x + 0.35, 9.6, 2.95, 0.9, 1.6, 0.9], "color": "#FFF59D", "brillo": True,
                       "cuando": "aire"})
    return {"hueso": "cuerpo", "grupos": [{"id": "jetpack", "piezas": piezas}]}


# ---------------------------------------------------------------- cuerpo
def cola(segmentos, colores, tams, base_rot=(-35, 0, 0), curva=-8, sway=14, puntas=None):
    grupos = []
    padre = None
    for i in range(segmentos):
        w = tams[i]
        g = {"id": f"s{i}", "pivote": [0, 0, 0] if i else [0, 10.6, 2.0],
             "rot": list(base_rot) if i == 0 else [curva, 0, 0],
             "anim": {"ry": anim(amp=sway if i == 0 else sway * 0.5, frec=0.55, fase=-0.5 * i, amp_mov=0.6, frec_mov=1.2),
                      "rx": anim(mov=-14 if i == 0 else -4, agachado=12 if i == 0 else 0)},
             "piezas": [{"caja": [-w / 2, -w / 2, 0, w, w, 3.0], "color": colores[i]}]}
        if i:
            g["padre"] = padre
            g["pivote"] = [0, 0, 2.8]
        if puntas and i < segmentos - 1:
            g["piezas"].append({"caja": [-0.4, -w / 2 - 0.9, 0.8, 0.8, 1.0, 1.2], "color": puntas})
        grupos.append(g)
        padre = g["id"]
    return {"hueso": "cuerpo", "grupos": grupos}


def bufanda():
    rojo, blanco = "#D32F2F", "#FAFAFA"
    piezas = [
        {"caja": [-4.6, -0.2, -2.6, 9.2, 1.8, 0.6], "color": rojo},
        {"caja": [-4.6, -0.2, 2.0, 9.2, 1.8, 0.6], "color": rojo},
        {"caja": [-4.6, -0.2, -2.0, 0.6, 1.8, 4.0], "color": rojo},
        {"caja": [4.0, -0.2, -2.0, 0.6, 1.8, 4.0], "color": rojo},
        {"caja": [-1.6, -0.2, -2.8, 0.8, 1.8, 0.3], "color": blanco},
        {"caja": [1.0, -0.2, -2.8, 0.8, 1.8, 0.3], "color": blanco},
    ]
    cola_b = [{"caja": [-1.0, 0, 0, 2.0, 5.5, 0.5], "color": rojo},
              {"caja": [-1.0, 1.4, -0.05, 2.0, 0.7, 0.6], "color": blanco},
              {"caja": [-1.0, 3.4, -0.05, 2.0, 0.7, 0.6], "color": blanco}]
    return {"hueso": "cuerpo", "grupos": [
        {"id": "cuello", "piezas": piezas},
        {"id": "punta", "pivote": [1.8, 1.0, 2.6], "rot": [10, 0, 0],
         "anim": {"rx": anim(amp=6, frec=1.4, amp_mov=1.5, frec_mov=1.5, mov=-45, aire=-25)}, "piezas": cola_b}]}


def orbitas():
    return {"hueso": "jugador", "emisores": [
        {"tipo": "orbita", "n": 3, "radio": 9.5, "y": 6, "vel": 110, "tam": 1.5, "bob": 1.2,
         "colores": ["#55FFFF", "#FF55FF", "#FFFF55"], "brillo": True}]}


# ---------------------------------------------------------------- auras / ki
def aura(colores, brillo=True, n=56, vida=0.8, tam=(3.0, 0.6), alfa=(0.42, 0.0), subida=16.0, extra=None, estirar=2.2):
    em = [{"tipo": "subida", "n": n, "radio": [2.6, 6.2], "y": [-6, 24], "vida": vida, "subida": subida,
           "tam": list(tam), "colores": colores, "alfa": list(alfa), "brillo": brillo, "mov": 0.6, "golpe": 0.9,
           "estirar": estirar}]
    return {"hueso": "jugador", "emisores": em + (extra or [])}


def rastro(colores, tam, sube):
    return {"hueso": "jugador", "solo_vista": True, "emisores": [
        {"tipo": "subida", "n": 10, "radio": [1.0, 4.5], "y": [22, 24], "vida": 0.9, "subida": sube,
         "tam": [tam, tam * 0.4], "colores": colores, "alfa": [1.0, 0.2], "brillo": True}]}


# ---------------------------------------------------------------- escudos (pixel art de 12 × 22 en la placa)
def escudo(filas, paleta, brillo_chars=(), parpadeo=None, borde="#000000"):
    piezas = [{"caja": [-6, -11, -2, 12, 22, 1], "color": paleta.get("fondo", "#333333")},
              {"caja": [-1, -3, -1, 2, 6, 6], "color": "#5D4037"}]
    normal = {k: v for k, v in paleta.items() if k not in brillo_chars and k != "fondo"}
    brilla = {k: v for k, v in paleta.items() if k in brillo_chars}
    if normal:
        piezas.append({"pixeles": {"origen": [-6, -11, -2.4], "plano": "xy", "filas": filas, "paleta": normal,
                                   "tam": 1.0, "prof": 0.3}})
    if brilla:
        p = {"pixeles": {"origen": [-6, -11, -2.45], "plano": "xy", "filas": filas, "paleta": brilla, "tam": 1.0,
                         "prof": 0.35}, "brillo": True}
        if parpadeo:
            p["parpadeo"] = parpadeo
        piezas.append(p)
    return {"hueso": "escudo", "grupos": [{"id": "escudo", "piezas": piezas}]}


def marco(interior):
    """12 × 22 con borde de 1 píxel ('b') alrededor de las filas de 10 × 20 que se le pasan."""
    filas = ["b" * 12]
    for f in interior:
        filas.append("b" + f.ljust(10, ".")[:10] + "b")
    while len(filas) < 21:
        filas.append("b" + "." * 10 + "b")
    filas.append("b" * 12)
    return filas


ESCUDO_N = marco(["", "", "", "", ".nn....nn.", ".nnn...nn.", ".nnnn..nn.", ".nn.nn.nn.", ".nn..nnnn.", ".nn...nnn.",
                  ".nn....nn.", ".nn....nn.", "", "", "", "", ".gggggggg.", "", "", ""])
ESCUDO_DRAGON = marco(["", "...gg.....", "..gggg....", ".gg..gg...", ".g....gggg", ".gg...g..g", "..gggg....",
                       "....gg....", "...gggg...", "..gg..gg..", ".gg....gg.", "gg......gg", "", "", ".rrrrrrrr.",
                       "", "", "", ""])
ESCUDO_DORADO = marco(["", "", "....ww....", "....ww....", "....ww....", ".wwwwwwww.", ".wwwwwwww.", "....ww....",
                       "....ww....", "....ww....", "....ww....", "....ww....", "....ww....", "", "", ".rr....rr.",
                       "..rrrrrr..", "", ""])
ESCUDO_HIELO = marco(["", "....c.....", "...ccc....", "....c.....", "..c.c.c...", "...ccc....", ".ccccccc..",
                      "...ccc....", "..c.c.c...", "....c.....", "...ccc....", "....c.....", "", ".c......c.",
                      "ccc....ccc", ".c......c.", "", "", ""])
ESCUDO_FUEGO = marco(["", "", "....f.....", "...ff.....", "...fff....", "..ffyf....", "..fyyff...", ".ffyyyf...",
                      ".fyyyyff..", ".fyyyyyf..", ".ffyyyff..", "..ffyff...", "...fff....", "", "", "", "", "", ""])
ESCUDO_OBSIDIANA = marco(["..p.......", "...p......", "...p...p..", "....p.p...", ".....p....", ".....p....",
                          "....p.p...", "...p...p..", "..p.....p.", ".p.......p", "..p.....p.", "...p...p..",
                          "....p.p...", ".....p....", ".....p....", "....p.....", "...p......", "..p.......", "", ""])


def catalogo():
    c = {
        "_info": "Catálogo de cosméticos de NexLauncher (generado con generar.py; no se edita a mano).",
        # cabeza
        "aureola_dorada": aureola("#FFD54A"),
        "aureola_arcoiris": aureola(arcoiris=True),
        "aureola_oscura": aureola(oscura=True),
        "corona": corona(),
        "cuernos": cuernos(),
        "orejas_gato": orejas(),
        "orejas_zorro": orejas(zorro=True),
        "nube": nube(),
        # espalda
        "alas_angel": alas_plumas(ALA_ANGEL, {"#": "#FFFFFF", "g": "#D9E2EC"}),
        "alas_dragon": alas_dragon(),
        "alas_mariposa": alas_mariposa(),
        "alas_fuego": alas_fuego(),
        "jetpack": jetpack(),
        # cuerpo
        "cola_zorro": cola(4, ["#E8762B", "#E8762B", "#F08A3C", "#FFFFFF"], [2.6, 3.2, 3.0, 2.2]),
        "cola_gato": cola(6, ["#3A3A3A"] * 6, [1.2] * 6, base_rot=(-55, 0, 0), curva=12, sway=18),
        "cola_dragon": cola(5, ["#5A0F0F", "#6B1414", "#7A1A1A", "#8A2020", "#A12828"], [3.2, 2.8, 2.3, 1.8, 1.3],
                            base_rot=(-30, 0, 0), curva=6, sway=12, puntas="#2B0808"),
        "bufanda": bufanda(),
        "orbitas": orbitas(),
        # auras
        "ki_blanco": aura(["#FFFFFF", "#E8F4FF", "#D6ECFF"]),
        "ki_dorado": aura(["#FFE066", "#FFC107", "#FFF4B0"]),
        "ki_azul": aura(["#4FC3F7", "#81D4FA", "#E1F5FE"]),
        "ki_rayos": aura(["#E1F5FE", "#B3E5FC", "#FFFFFF"], n=40, extra=[
            {"tipo": "rayo", "n": 5, "radio": [4, 8], "y": [-6, 20], "largo": 9, "segmentos": 4, "frec": 11,
             "prob": 0.55, "zigzag": 3.5, "tam": 0.45, "colores": ["#FFFFFF", "#B3E5FC", "#82B1FF"], "brillo": True}]),
        "aura_oscura": aura(["#1A0F24", "#2D1A40", "#4A2470"], brillo=False, n=46, vida=1.5, tam=(3.4, 1.2),
                            alfa=(0.6, 0.0), subida=10, estirar=1.3),
        "aura_fuego": aura(["#FFD54F", "#FF9800", "#FF5722", "#D84315"], n=60, vida=0.65, tam=(2.6, 0.3), subida=18,
                           alfa=(0.6, 0.0), estirar=1.8),
        "aura_arcoiris": aura(["#FF5252", "#FFAB40", "#FFFF00", "#69F0AE", "#40C4FF", "#E040FB"], n=56),
        "aura_hielo": aura(["#E0F7FA", "#80DEEA", "#B2EBF2"], n=46, vida=1.4, tam=(2.2, 0.8), subida=8, estirar=1.0,
                           alfa=(0.6, 0.0), extra=[
            {"tipo": "orbita", "n": 6, "radio": 7.5, "y": 12, "vel": 35, "tam": 1.2, "bob": 2.0,
             "colores": ["#E0F7FA", "#80DEEA"], "brillo": True}]),
        # rastros: en el juego son partículas de Minecraft; esto es solo para la vista previa de NexLauncher
        "chispas": rastro(["#FFF59D", "#FFEB3B", "#FFFFFF"], 0.5, 6),
        "corazones": rastro(["#E53935", "#FF5252"], 1.2, 9),
        "notas": rastro(["#69F0AE", "#40C4FF", "#FF4081", "#FFD740"], 1.2, 9),
        "pisadas_fuego": rastro(["#FF9800", "#FF5722", "#FFD54F"], 0.6, 4),
        # escudos
        "escudo_nex": escudo(ESCUDO_N, {"fondo": "#141416", "b": "#2BD96B", "n": "#2BD96B", "g": "#1E8E4A"},
                             brillo_chars=("n",), parpadeo={"frec": 0.6, "min": 0.75}),
        "escudo_dragon": escudo(ESCUDO_DRAGON, {"fondo": "#5A0F0F", "b": "#2B0808", "g": "#F2C14E", "r": "#B3261E"}),
        "escudo_dorado": escudo(ESCUDO_DORADO, {"fondo": "#E0B23C", "b": "#9C7A1E", "w": "#FFFFFF", "r": "#C62828"}),
        "escudo_hielo": escudo(ESCUDO_HIELO, {"fondo": "#B3E5FC", "b": "#4FC3F7", "c": "#FFFFFF"}, brillo_chars=("c",),
                               parpadeo={"frec": 0.8, "min": 0.7, "desfase": 0.3}),
        "escudo_fuego": escudo(ESCUDO_FUEGO, {"fondo": "#2B1B12", "b": "#BF360C", "f": "#FF6D00", "y": "#FFD54F"},
                               brillo_chars=("f", "y"), parpadeo={"frec": 6.0, "min": 0.7, "desfase": 0.8}),
        "escudo_obsidiana": escudo(ESCUDO_OBSIDIANA, {"fondo": "#140A1F", "b": "#2E1A47", "p": "#B34DFF"},
                                   brillo_chars=("p",), parpadeo={"frec": 0.9, "min": 0.45, "desfase": 0.25}),
    }
    # NexClient 4.5: alas articuladas y de plumas, capas 3D, bandanas y escudos con forma propia
    c.update(nuevos.alas_nuevas())
    c.update(nuevos.capas_nuevas())
    c.update(nuevos.bandanas_nuevas())
    c.update(nuevos.escudos_nuevos())
    # NexClient 4.8: de temporada (Día de Muertos, Fiestas Patrias y Navidad), con mascotas y cosas en la mano
    c.update(festivos.festivos())
    c.update(festivos.mascotas())
    return c


if __name__ == "__main__":
    aqui = os.path.dirname(os.path.abspath(__file__))
    texto = json.dumps(catalogo(), ensure_ascii=False, separators=(",", ":"))
    destinos = [os.path.join(aqui, "cosmeticos.json"),
                os.path.join(aqui, "..", "fabric-1.21.1", "src", "main", "resources", "assets", "nexclient", "cosmeticos.json"),
                os.path.join(aqui, "..", "forge-1.8.9", "src", "main", "resources", "assets", "nexclient", "cosmeticos.json")]
    for ruta in destinos:
        if os.path.isdir(os.path.dirname(ruta)):
            with open(ruta, "w", encoding="utf-8") as f:
                f.write(texto)
            print(ruta, len(texto.encode()), "bytes")
    # NexLauncher (si está al lado): el mismo catálogo como módulo, así va dentro del .exe sin tocar los .bat
    launcher = os.path.join(aqui, "..", "..", "NexLauncher", "nexlauncher")
    if len(os.sys.argv) > 1:
        launcher = os.sys.argv[1]
    if os.path.isdir(launcher):
        with open(os.path.join(launcher, "cosmeticos_datos.py"), "w", encoding="utf-8") as f:
            f.write('"""Catálogo de cosméticos 3D (generado por NexLauncherClient/cosmeticos/generar.py)."""\n')
            f.write("DATOS = " + repr(texto) + "\n")
        print(launcher, "cosmeticos_datos.py")

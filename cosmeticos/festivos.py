"""Cosméticos de temporada de NexClient 4.8: quien entra durante la festividad se los queda para siempre (NexSkins
guarda cuáles consiguió cada cuenta; los creadores tienen todos).

  muertos  (25 oct – 3 nov):            alebrijes de mascota y corona de cempasúchil
  patrias  (1 – 16 sep y 4 – 20 nov):   águila de mascota, sombrero de charro, sarape, maracas y jarrito
  navidad  (1 dic – 6 ene):             Rodolfo el reno, gorro navideño, cuernos de reno, bufanda, bastón de dulce y nieve

Categorías nuevas:
  mascota: hueso «jugador» (no gira con el cuerpo): flota junto al hombro izquierdo.
  mano:    hueso «mano» = el brazo izquierdo; coordenadas desde su pivote (el hombro): el brazo va de y -2 a 10,
           x de -1 a 3, z de -2 a 2; el puño está en (1, 10, 0) y lo que se agarra apunta al frente (-Z).
"""
import math


def anim(**k):
    return k


def caja(x, y, z, w, h, d, color, **extra):
    p = {"caja": [round(x, 3), round(y, 3), round(z, 3), round(w, 3), round(h, 3), round(d, 3)], "color": color}
    p.update(extra)
    return p


def anillo(radio, n, tam, color, centro=(0, 0, 0), **extra):
    p = {"anillo": {"centro": list(centro), "radio": radio, "n": n, "tam": list(tam)}, "color": color}
    p.update(extra)
    return p


def _temporada(d, t):
    d["temporada"] = t
    return d


# Las mascotas flotan aquí (desde el cuello): a la izquierda del jugador, a la altura de la cabeza y un poco atrás
MASCOTA = [14.0, -6.0, 5.0]
FLOTAR = {"y": anim(amp=1.3, frec=0.45), "ry": anim(amp=9, frec=0.21), "rx": anim(mov=-8, aire=-6)}


def _alas(pivote, plumas, colores, largo=7, rot_z=-18, aleteo=26, frec=1.5, padre="mascota"):
    """Un par de alas (sin «espejo»: el espejo de los grupos voltea alrededor del jugador, no de la mascota).
    Cada ala es una placa horizontal de plumas que sale hacia el costado y aletea."""
    grupos = []
    for lado in (1, -1):
        filas = []
        for j in range(plumas):          # j: de adelante hacia atrás
            f = ""
            for i in range(largo):       # i: desde el cuerpo hacia la punta
                if i >= largo - 1 - (abs(j - plumas // 2) // 2):
                    f += "."             # punta redondeada
                else:
                    f += "abcdefgh"[(i + j) % len(colores)]
            filas.append(f if lado > 0 else f[::-1])
        tam = 0.95
        ancho = largo * tam
        x0 = 0 if lado > 0 else -ancho
        grupos.append({"id": "ala_%s" % ("i" if lado > 0 else "d"), "padre": padre,
                       "pivote": [pivote[0] * lado, pivote[1], pivote[2]], "rot": [0, 0, rot_z * lado],
                       "anim": {"rz": anim(amp=aleteo * lado, frec=frec, amp_mov=0.4, frec_mov=0.6)},
                       "piezas": [{"pixeles": {"origen": [x0, 0, -plumas * tam / 2], "plano": "xz", "filas": filas,
                                               "paleta": {"abcdefgh"[k]: c for k, c in enumerate(colores)},
                                               "tam": tam, "prof": 0.45}}]})
    return grupos


# ---------------------------------------------------------------- Día de Muertos
def alebrije(cuerpo, franjas, cabeza, hocico, cuernos, alas, cola, ojos="#FFFFFF"):
    """Alebrije: un dragón-jaguar de colores con alas, franjas y puntos, que flota y mira alrededor."""
    piezas = [caja(-2, -2, -4, 4, 4, 8, cuerpo)]
    for k, z in enumerate((-3.2, -1.4, 0.4, 2.2)):        # franjas alrededor del cuerpo
        piezas.append(caja(-2.12, -2.12, z, 4.24, 4.24, 0.7, franjas[k % len(franjas)]))
    for k, (x, y, z) in enumerate(((-2.2, -0.6, -2.4), (2.0, -0.6, -2.4), (-2.2, 0.8, 1.2), (2.0, 0.8, 1.2),
                                   (-2.2, -1.2, 3.0), (2.0, -1.2, 3.0))):  # puntos a los lados
        piezas.append(caja(x, y, z, 0.25, 0.9, 0.9, franjas[(k + 1) % len(franjas)]))
    for k, z in enumerate((-3.4, -1.6, 0.2, 2.0, 3.4)):    # cresta de púas
        piezas.append(caja(-0.4, -3.0 - (k % 2) * 0.5, z, 0.8, 1.2 + (k % 2) * 0.5, 0.8, franjas[k % len(franjas)]))
    patas = []
    for k, (x, z) in enumerate(((-1.9, -3.2), (0.9, -3.2), (-1.9, 2.0), (0.9, 2.0))):
        patas.append({"id": "pata%d" % k, "padre": "mascota", "pivote": [x + 0.5, 2, z + 0.5],
                      "anim": {"rx": anim(amp=14, frec=0.9, fase=k * 1.6)},
                      "piezas": [caja(-0.5, 0, -0.5, 1, 2.2, 1, cuerpo), caja(-0.6, 1.8, -0.7, 1.2, 0.6, 1.3, hocico)]})
    cabeza_p = [caja(-2.2, -2.6, -3.8, 4.4, 4, 4, cabeza),
                caja(-1.4, -0.9, -5.6, 2.8, 2.1, 1.9, hocico),          # hocico
                caja(-1.0, 0.9, -5.4, 2.0, 0.5, 1.4, "#E4007C"),          # lengua
                caja(-2.3, -2.0, -3.4, 0.2, 1.3, 1.3, ojos), caja(2.1, -2.0, -3.4, 0.2, 1.3, 1.3, ojos),
                caja(-2.4, -1.7, -3.1, 0.2, 0.7, 0.7, "#111111"), caja(2.2, -1.7, -3.1, 0.2, 0.7, 0.7, "#111111"),
                caja(-1.9, -2.7, -3.9, 1.0, 0.4, 0.4, franjas[0]), caja(0.9, -2.7, -3.9, 1.0, 0.4, 0.4, franjas[1])]
    for lado in (1, -1):                                          # cuernitos que se curvan hacia atrás
        cabeza_p.append(caja(1.0 * lado - 0.5, -4.0, -1.8, 1.0, 1.6, 1.0, cuernos))
        cabeza_p.append(caja(1.2 * lado - 0.4, -4.6, -0.9, 0.8, 0.9, 1.2, cuernos))
        cabeza_p.append(caja(2.2 if lado > 0 else -2.7, -1.8, -1.4, 0.5, 2.4, 0.6, franjas[2 % len(franjas)]))  # orejitas
    colas = [{"id": "cola1", "padre": "mascota", "pivote": [0, -0.5, 4], "rot": [-18, 0, 0],
              "anim": {"ry": anim(amp=22, frec=0.7)}, "piezas": [caja(-1, -1, 0, 2, 2, 3, cuerpo)]},
             {"id": "cola2", "padre": "cola1", "pivote": [0, 0, 3], "rot": [-22, 0, 0],
              "anim": {"ry": anim(amp=26, frec=0.7, fase=0.8)}, "piezas": [caja(-0.75, -0.75, 0, 1.5, 1.5, 2.6, franjas[0])]},
             {"id": "cola3", "padre": "cola2", "pivote": [0, 0, 2.6], "rot": [-28, 0, 0],
              "anim": {"ry": anim(amp=30, frec=0.7, fase=1.6)},
              "piezas": [caja(-0.5, -0.5, 0, 1, 1, 2, franjas[1]),
                         caja(-1.0, -1.4, 1.6, 2.0, 2.0, 1.6, cola, brillo=True,
                              parpadeo={"frec": 2.2, "min": 0.6})]}]
    grupos = [{"id": "mascota", "pivote": MASCOTA, "anim": FLOTAR, "piezas": piezas},
              {"id": "cabeza", "padre": "mascota", "pivote": [0, -1, -4],
               "anim": {"ry": anim(amp=18, frec=0.17), "rx": anim(amp=6, frec=0.33, fase=1.0)}, "piezas": cabeza_p}]
    grupos += patas + colas + _alas([1.9, -1.8, -0.5], 6, alas)
    return {"hueso": "jugador", "grupos": grupos}


def corona_cempasuchil():
    flores = {"id": "flores", "anim": {"y": anim(amp=0.1, frec=0.5)}, "piezas": [
        anillo(5.0, 10, [2.0, 1.9, 1.3], "#FF8F1F", centro=(0, -8.3, 0)),
        anillo(5.55, 10, [1.0, 1.0, 0.5], "#FFC21A", centro=(0, -8.3, 0), brillo=True,
               parpadeo={"frec": 0.6, "min": 0.75, "desfase": 0.6}),
    ]}
    hojas = {"id": "hojas", "rot": [0, 18, 0], "piezas": [
        anillo(4.85, 10, [1.6, 1.0, 0.8], "#2E7D32", centro=(0, -7.4, 0)),
        anillo(5.1, 10, [1.2, 1.2, 1.0], "#E4007C", centro=(0, -8.6, 0)),   # unas flores rosas entre las naranjas
    ]}
    return {"hueso": "cabeza", "grupos": [flores, hojas]}


# ---------------------------------------------------------------- Fiestas Patrias
def aguila():
    cafe, cafe2, blanco, pico, verde = "#5D3A1A", "#7A4E26", "#F2EFE9", "#F2B630", "#2E7D32"
    piezas = [caja(-1.8, -2.2, -3.4, 3.6, 3.8, 6.4, cafe),
              caja(-1.6, -0.6, -3.6, 3.2, 2.0, 2.4, cafe2),                 # pecho
              caja(-1.4, 1.6, -1.0, 2.8, 1.2, 3.0, cafe2)]
    for k in range(3):                                                     # cola en abanico
        piezas.append(caja(-1.8 + k * 1.2, -1.2, 3.0, 1.2, 0.5, 2.6 + (k == 1) * 0.6, blanco if k == 1 else cafe2))
    for x in (-1.3, 0.5):                                                  # garras
        piezas += [caja(x, 1.6, -1.6, 0.8, 1.8, 0.8, pico), caja(x - 0.1, 3.2, -2.2, 1.0, 0.4, 1.2, "#3B2A12")]
    cabeza = [caja(-1.5, -3.2, -2.6, 3.0, 3.0, 3.0, blanco),
              caja(-0.7, -2.0, -4.0, 1.4, 1.2, 1.5, pico),                 # pico
              caja(-0.5, -0.8, -4.0, 1.0, 0.6, 0.9, pico),
              caja(-1.6, -2.6, -2.2, 0.15, 0.7, 0.7, "#111111"), caja(1.45, -2.6, -2.2, 0.15, 0.7, 0.7, "#111111"),
              caja(-1.6, -2.75, -2.35, 0.2, 0.3, 1.0, pico), caja(1.4, -2.75, -2.35, 0.2, 0.3, 1.0, pico)]
    vibora = [caja(-2.8, -0.5, -4.4, 2.2, 0.7, 0.7, verde), caja(0.6, -0.5, -4.4, 2.0, 0.7, 0.7, verde),
              caja(2.4, -0.2, -4.5, 0.7, 1.6, 0.7, verde), caja(-3.2, -0.3, -4.5, 0.7, 1.4, 0.7, verde),
              caja(-3.3, 0.9, -4.6, 0.9, 0.6, 0.9, "#1B5E20")]
    grupos = [{"id": "mascota", "pivote": MASCOTA, "anim": FLOTAR, "piezas": piezas},
              {"id": "cabeza", "padre": "mascota", "pivote": [0, -1.6, -2.8],
               "anim": {"ry": anim(amp=22, frec=0.15), "rx": anim(amp=5, frec=0.4)}, "piezas": cabeza},
              {"id": "vibora", "padre": "cabeza", "anim": {"rz": anim(amp=8, frec=1.3)}, "piezas": vibora}]
    grupos += _alas([1.7, -1.6, -0.6], 7, [cafe, cafe2, cafe, "#3E2510"], largo=9, rot_z=-12, aleteo=30, frec=1.2)
    return {"hueso": "jugador", "grupos": grupos}


def sombrero_charro():
    negro, oro, oro2 = "#1C1414", "#FFD54A", "#E0B23A"
    # ala: disco de 20 px con el borde dorado y un bordado de grecas
    r = 10
    filas = []
    for j in range(2 * r):
        f = ""
        for i in range(2 * r):
            d = math.hypot(i + 0.5 - r, j + 0.5 - r)
            if d > r:
                f += "."
            elif d > r - 1.1:
                f += "o"
            elif 6.3 < d < 7.4 and (int(math.degrees(math.atan2(j + 0.5 - r, i + 0.5 - r))) // 15) % 2 == 0:
                f += "g"
            else:
                f += "n"
        filas.append(f)
    ala = {"pixeles": {"origen": [-r, -9.3, -r], "plano": "xz", "filas": filas,
                       "paleta": {"n": negro, "o": oro, "g": oro2}, "tam": 1.0, "prof": 0.55}}
    piezas = [ala, anillo(9.4, 34, [1.9, 1.0, 0.6], oro, centro=(0, -9.7, 0))]   # orilla levantada
    # copa: cono redondeado, más ancha abajo
    copa = [caja(-3.5, -11.6, -3.5, 7.0, 2.5, 7.0, negro), caja(-2.9, -13.2, -2.9, 5.8, 1.7, 5.8, negro),
            caja(-2.2, -14.5, -2.2, 4.4, 1.4, 4.4, negro), caja(-1.4, -15.3, -1.4, 2.8, 0.9, 2.8, negro)]
    for k, (rad, y, c) in enumerate(((4.05, -9.75, "#2E9E4F"), (4.05, -10.35, "#F5F5F5"), (4.05, -10.95, "#D62828"))):
        copa.append(anillo(rad, 18, [1.55, 0.62, 0.5], c, centro=(0, y, 0)))   # listón tricolor
    for k, (rad, y) in enumerate(((3.55, -12.3), (2.9, -13.6), (2.15, -14.7))):
        copa.append(anillo(rad, 16, [1.4, 1.2, 0.5], negro, centro=(0, y, 0)))
    copa += [caja(-0.8, -15.8, -0.8, 1.6, 0.6, 1.6, negro), caja(-0.8, -12.1, -3.9, 1.6, 1.4, 0.4, oro, brillo=True,
                                                              parpadeo={"frec": 0.7, "min": 0.65})]
    for k in range(10):                                                    # botones dorados en el ala
        a = math.radians(k * 36 + 18)
        piezas.append(caja(math.cos(a) * 8.3 - 0.4, -9.75, math.sin(a) * 8.3 - 0.4, 0.8, 0.45, 0.8, oro, brillo=True,
                           parpadeo={"frec": 0.8, "min": 0.6, "desfase": 0.9}))
    return {"hueso": "cabeza", "grupos": [{"id": "sombrero", "anim": {"rz": anim(mov=3, amp=1.2, frec=0.6)},
                                           "piezas": piezas + copa}]}


def sarape():
    """Sarape de Saltillo en diagonal sobre el pecho y la espalda, con flecos abajo."""
    colores = ["#D62828", "#F77F00", "#FCBF49", "#2E9E4F", "#1D7CC9", "#7B2CBF", "#E4007C", "#1A1A1A"]
    letras = "abcdefgh"
    cols, filas_n, tam = 15, 13, 0.62
    filas = []
    for j in range(filas_n):
        f = ""
        for i in range(cols):
            if j == filas_n - 1:
                f += "h" if i % 2 == 0 else "."   # flecos
            else:
                f += letras[((i + j) // 2) % len(colores)]
        filas.append(f)
    ancho = cols * tam
    pal = {letras[k]: c for k, c in enumerate(colores)}
    piezas = [{"pixeles": {"origen": [-ancho / 2, -0.25, -2.62], "plano": "xy", "filas": filas, "paleta": pal,
                           "tam": tam, "prof": 0.32}},
              {"pixeles": {"origen": [-ancho / 2, -0.25, 2.3], "plano": "xy", "filas": filas, "paleta": pal,
                           "tam": tam, "prof": 0.32}},
              caja(-ancho / 2, -0.55, -2.62, ancho, 0.4, 5.24, colores[0])]   # sobre los hombros
    return {"hueso": "cuerpo", "grupos": [{"id": "sarape", "anim": {"rx": anim(mov=-4, amp=0.8, frec=0.9)},
                                           "piezas": piezas}]}


def maracas():
    rojo, verde, blanco, madera = "#D62828", "#2E9E4F", "#F5F5F5", "#A0662B"
    piezas = [caja(0.4, 9.3, -6.2, 1.2, 1.2, 5.4, madera),
              caja(-0.9, 8.0, -10.0, 3.8, 3.8, 3.9, rojo),
              caja(-0.6, 7.7, -9.7, 3.2, 4.4, 3.3, rojo),
              caja(-1.2, 8.3, -9.7, 4.4, 3.2, 3.3, rojo),
              caja(-0.95, 7.95, -8.0, 3.9, 3.9, 0.55, blanco),       # franjas
              caja(-0.95, 7.95, -9.2, 3.9, 3.9, 0.55, verde)]
    for k, (x, y) in enumerate(((0.2, 7.6), (2.0, 9.2), (-1.3, 9.6), (1.0, 11.9))):
        piezas.append(caja(x, y, -9.9, 0.6, 0.6, 0.4, "#FCBF49"))
    return {"hueso": "mano", "grupos": [{"id": "maraca", "pivote": [1, 10, 0],
                                         "anim": {"rx": anim(amp=9, frec=4.5, amp_mov=0.5)},
                                         "piezas": [dict(p, caja=[p["caja"][0] - 1, p["caja"][1] - 10] + p["caja"][2:])
                                                    for p in piezas]}]}


def jarrito():
    barro, barro2, crema, ponche = "#B5562A", "#8E3F1C", "#F3E2C7", "#7A1F12"
    piezas = [caja(-1.3, -2.3, -5.0, 4.6, 4.4, 4.2, barro),
              caja(-1.5, -2.6, -5.2, 5.0, 0.6, 4.6, barro2),           # boca
              caja(-1.0, -2.45, -4.7, 4.0, 0.2, 3.6, ponche),          # el ponche adentro
              caja(-1.0, 2.1, -4.7, 4.0, 0.4, 3.6, barro2),
              caja(3.3, -1.6, -3.6, 0.9, 2.8, 1.4, barro)]             # asa
    for k in range(4):                                                 # florecitas pintadas
        piezas.append(caja(-1.4 + k * 1.15, -0.4, -5.08, 0.6, 0.6, 0.1, crema if k % 2 else "#FCBF49"))
    vapor = {"id": "vapor", "pivote": [0.8, -2.8, -2.9], "anim": {"y": anim(amp=0.6, frec=0.8), "ry": anim(vel=40)},
             "piezas": [caja(-0.3, -1.6, -0.3, 0.6, 1.0, 0.6, "#FFFFFF", alfa=0.45),
                        caja(0.2, -3.0, 0.1, 0.5, 0.9, 0.5, "#FFFFFF", alfa=0.3)]}
    return {"hueso": "mano", "grupos": [{"id": "jarrito", "pivote": [0, 10, 0], "anim": {"rx": anim(mov=6)},
                                         "piezas": piezas}, dict(vapor, padre="jarrito")]}


# ---------------------------------------------------------------- Navidad
def rodolfo():
    cafe, cafe2, panza, cuerno, rojo, oro = "#7A4A2A", "#5C361E", "#C79A6B", "#D7B98E", "#E53935", "#F2C14E"
    piezas = [caja(-2, -2.4, -4, 4, 4, 8, cafe),
              caja(-1.6, 0.6, -3, 3.2, 1.2, 6, panza),
              caja(-0.7, -2.0, 3.8, 1.4, 1.4, 1.2, "#F5F5F5"),            # colita
              caja(-2.1, -2.5, -4.1, 4.2, 1.0, 1.4, rojo),                 # collar
              caja(-0.6, -1.7, -4.6, 1.2, 1.2, 0.8, oro, brillo=True, parpadeo={"frec": 1.1, "min": 0.6})]
    patas = []
    for k, (x, z) in enumerate(((-1.9, -3.4), (0.9, -3.4), (-1.9, 2.4), (0.9, 2.4))):
        patas.append({"id": "pata%d" % k, "padre": "mascota", "pivote": [x + 0.5, 1.6, z + 0.5], "rot": [0, 0, 0],
                      "anim": {"rx": anim(amp=28, frec=1.4, fase=(0 if k in (0, 3) else math.pi))},
                      "piezas": [caja(-0.5, 0, -0.5, 1, 3, 1, cafe2), caja(-0.55, 2.6, -0.55, 1.1, 0.6, 1.1, "#2B1B10")]})
    cabeza = [caja(-1.7, -3.2, -2.8, 3.4, 3.0, 3.0, cafe),
              caja(-1.2, -1.8, -4.6, 2.4, 1.8, 1.9, panza),
              caja(-0.65, -1.5, -5.3, 1.3, 1.3, 0.8, rojo, brillo=True, parpadeo={"frec": 1.6, "min": 0.45}),  # ¡la nariz!
              caja(-1.8, -2.6, -2.4, 0.15, 0.7, 0.7, "#111111"), caja(1.65, -2.6, -2.4, 0.15, 0.7, 0.7, "#111111"),
              caja(-2.6, -3.2, -1.2, 1.0, 0.6, 0.9, cafe2), caja(1.6, -3.2, -1.2, 1.0, 0.6, 0.9, cafe2)]   # orejas
    for lado in (1, -1):                                              # cornamenta
        cabeza += [caja(0.9 * lado - 0.3, -5.6, -1.2, 0.6, 2.6, 0.6, cuerno),
                   caja((1.3 if lado > 0 else -2.9), -6.0, -1.2, 1.6, 0.6, 0.6, cuerno),
                   caja(2.6 * lado - 0.3, -7.4, -1.2, 0.6, 2.0, 0.6, cuerno),
                   caja(0.9 * lado - 0.3, -7.2, -1.2, 0.6, 1.6, 0.6, cuerno),
                   caja(1.7 * lado - 0.3, -8.0, -0.6, 0.6, 1.5, 0.6, cuerno)]
    grupos = [{"id": "mascota", "pivote": MASCOTA, "anim": dict(FLOTAR, rx=anim(amp=4, frec=0.7, mov=-8)), "piezas": piezas},
              {"id": "cabeza", "padre": "mascota", "pivote": [0, -1.6, -3.4],
               "anim": {"ry": anim(amp=14, frec=0.2), "rx": anim(amp=6, frec=0.7)}, "piezas": cabeza}]
    return {"hueso": "jugador", "grupos": grupos + patas}


def gorro_navideno():
    rojo, rojo2, blanco = "#D7263D", "#B01E31", "#F7F7F7"
    banda = [caja(-4.9, -9.2, -4.9, 9.8, 2.2, 0.7, blanco), caja(-4.9, -9.2, 4.2, 9.8, 2.2, 0.7, blanco),
             caja(-4.9, -9.2, -4.2, 0.7, 2.2, 8.4, blanco), caja(4.2, -9.2, -4.2, 0.7, 2.2, 8.4, blanco),
             caja(-4.3, -9.4, -4.3, 8.6, 0.6, 8.6, rojo)]
    grupos = [{"id": "banda", "piezas": banda},
              {"id": "g1", "pivote": [0, -9.2, 0], "piezas": [caja(-4.0, -2.4, -4.0, 8.0, 2.6, 8.0, rojo)]}]
    tams = [(6.6, -14), (5.2, -20), (3.8, -24), (2.6, -26)]
    padre = "g1"
    for k, (t, rz) in enumerate(tams):
        gid = "g%d" % (k + 2)
        grupos.append({"id": gid, "padre": padre, "pivote": [0, -2.2, 0], "rot": [6, 0, rz],
                       "anim": {"rz": anim(amp=4 + k * 1.5, frec=0.5, fase=k * 0.4, mov=-4 - k * 2, aire=6)},
                       "piezas": [caja(-t / 2, -2.4, -t / 2, t, 2.6, t, rojo if k % 2 == 0 else rojo2)]})
        padre = gid
    grupos.append({"id": "pompon", "padre": padre, "pivote": [0, -2.2, 0],
                   "piezas": [caja(-1.5, -2.8, -1.5, 3.0, 3.0, 3.0, blanco)]})
    return {"hueso": "cabeza", "grupos": grupos}


def cuernos_reno():
    cuerno, cuerno2 = "#C9A27A", "#A97F55"
    g = []
    for lado in (1, -1):
        s = "d" if lado > 0 else "i"
        g += [{"id": "rama" + s, "pivote": [2.6 * lado, -8.4, 0], "rot": [0, 0, 22 * lado],
               "anim": {"rz": anim(amp=2, frec=0.4)},
               "piezas": [caja(-0.5, -4.2, -0.5, 1.0, 4.4, 1.0, cuerno),
                          caja(0.5 if lado > 0 else -1.5, -3.0, -0.45, 1.0, 0.9, 0.9, cuerno2)]},
              {"id": "punta" + s, "padre": "rama" + s, "pivote": [0, -4.2, 0], "rot": [0, 0, -35 * lado],
               "piezas": [caja(-0.45, -3.0, -0.45, 0.9, 3.2, 0.9, cuerno),
                          caja(-0.4, -2.2, -0.4 - 1.6, 0.8, 0.8, 1.8, cuerno2)]},
              {"id": "diente" + s, "padre": "rama" + s, "pivote": [0, -2.0, 0], "rot": [0, 0, 50 * lado],
               "piezas": [caja(-0.4, -2.4, -0.4, 0.8, 2.5, 0.8, cuerno2)]}]
    nariz = {"id": "cascabel", "piezas": [caja(-0.6, -8.9, -4.9, 1.2, 1.2, 1.2, "#F2C14E", brillo=True,
                                                 parpadeo={"frec": 1.0, "min": 0.7})]}
    return {"hueso": "cabeza", "grupos": g + [nariz]}


def bufanda_navidena():
    rojo, verde, blanco = "#D7263D", "#1E7B3C", "#F7F7F7"
    piezas = []
    franja = [rojo, blanco, verde, blanco]
    for k in range(4):                                                  # vuelta al cuello, a franjas
        x = -4.7 + k * 2.35
        c = franja[k % 4]
        piezas += [caja(x, -0.3, -2.75, 2.35, 2.0, 0.7, c), caja(x, -0.3, 2.05, 2.35, 2.0, 0.7, c)]
    piezas += [caja(-4.7, -0.3, -2.05, 0.7, 2.0, 4.1, verde), caja(4.0, -0.3, -2.05, 0.7, 2.0, 4.1, rojo)]
    cola = []
    for k in range(5):
        cola.append(caja(-1.1, k * 1.25, 0, 2.2, 1.25, 0.6, franja[k % 4]))
    cola += [caja(-1.1 + i * 0.55, 6.25, 0.05, 0.3, 0.9, 0.5, blanco) for i in range(4)]   # flecos
    return {"hueso": "cuerpo", "grupos": [
        {"id": "cuello", "piezas": piezas},
        {"id": "punta", "pivote": [-2.0, 1.2, -2.9], "rot": [-12, 0, 0],
         "anim": {"rx": anim(amp=6, frec=1.4, amp_mov=1.5, frec_mov=1.5, mov=-40, aire=-25)},
         "piezas": [dict(p, caja=[p["caja"][0], p["caja"][1], -p["caja"][2] - p["caja"][5]] + p["caja"][3:]) for p in cola]}]}


def baston_dulce():
    rojo, blanco = "#E53935", "#FAFAFA"
    piezas = []
    for k in range(7):                                                  # palo hacia el frente, a franjas
        piezas.append(caja(0.35, 9.35, -1.2 - (k + 1) * 1.15, 1.3, 1.3, 1.15, rojo if k % 2 == 0 else blanco))
    gancho = [(0.35, 8.2, -9.55), (0.35, 7.05, -9.55), (0.35, 6.0, -9.0), (0.35, 5.6, -7.85), (0.35, 6.0, -6.8),
              (0.35, 7.05, -6.5)]
    for k, (x, y, z) in enumerate(gancho):
        piezas.append(caja(x, y, z, 1.3, 1.15, 1.15, blanco if k % 2 == 0 else rojo))
    return {"hueso": "mano", "grupos": [{"id": "baston", "pivote": [1, 10, 0], "anim": {"rx": anim(mov=8, amp=2, frec=0.6)},
                                         "piezas": [dict(p, caja=[p["caja"][0] - 1, p["caja"][1] - 10] + p["caja"][2:])
                                                    for p in piezas]}]}


def nieve():
    return {"hueso": "jugador", "emisores": [
        {"tipo": "caida", "n": 46, "area": [-13, 13, -16, 24, -13, 13], "vida": 2.6, "tam": [0.7, 0.7, 0.7],
         "colores": ["#FFFFFF", "#E3F2FD", "#FFFFFF"], "alfa": 0.9, "brillo": True},
        {"tipo": "orbita", "n": 4, "radio": 10, "y": 4, "vel": 45, "tam": 0.9, "bob": 2.0,
         "colores": ["#B3E5FC", "#FFFFFF"], "brillo": True}]}


# ---------------------------------------------------------------- mascotas de todo el año
def _patas(color, pies, posiciones, alto=2.2, amp=16, frec=0.9):
    g = []
    for k, (x, z) in enumerate(posiciones):
        g.append({"id": "pata%d" % k, "padre": "mascota", "pivote": [x + 0.5, 1.8, z + 0.5],
                  "anim": {"rx": anim(amp=amp, frec=frec, fase=(0 if k in (0, 3) else math.pi))},
                  "piezas": [caja(-0.5, 0, -0.5, 1, alto, 1, color), caja(-0.55, alto - 0.4, -0.6, 1.1, 0.5, 1.2, pies)]})
    return g


def gatito():
    naranja, rayas, blanco, rosa = "#F59E42", "#C46A1C", "#FFF3E0", "#F48FB1"
    piezas = [caja(-1.8, -1.6, -3.4, 3.6, 3.4, 6.6, naranja), caja(-1.4, 0.6, -2.8, 2.8, 1.2, 5.0, blanco)]
    for z in (-2.2, -0.4, 1.4):
        piezas.append(caja(-1.9, -1.7, z, 3.8, 0.6, 0.7, rayas))
    cabeza = [caja(-2.0, -3.4, -3.0, 4.0, 3.4, 3.2, naranja), caja(-1.0, -1.4, -3.4, 2.0, 1.1, 0.5, blanco),
              caja(-0.35, -1.6, -3.55, 0.7, 0.45, 0.2, rosa),
              caja(-1.5, -2.6, -3.15, 0.8, 0.8, 0.2, "#7CB342"), caja(0.7, -2.6, -3.15, 0.8, 0.8, 0.2, "#7CB342"),
              caja(-1.2, -2.5, -3.2, 0.3, 0.6, 0.15, "#111111"), caja(0.95, -2.5, -3.2, 0.3, 0.6, 0.15, "#111111"),
              caja(-1.9, -4.6, -1.8, 1.3, 1.3, 1.0, naranja), caja(0.6, -4.6, -1.8, 1.3, 1.3, 1.0, naranja),
              caja(-1.6, -4.2, -1.85, 0.7, 0.7, 0.2, rosa), caja(0.9, -4.2, -1.85, 0.7, 0.7, 0.2, rosa)]
    cola = [{"id": "cola1", "padre": "mascota", "pivote": [0, -1.0, 3.0], "rot": [-55, 0, 0],
             "anim": {"rz": anim(amp=20, frec=0.5)}, "piezas": [caja(-0.5, -0.5, 0, 1, 1, 3, naranja)]},
            {"id": "cola2", "padre": "cola1", "pivote": [0, 0, 3.0], "rot": [-40, 0, 0],
             "anim": {"rz": anim(amp=25, frec=0.5, fase=0.6)}, "piezas": [caja(-0.5, -0.5, 0, 1, 1, 2.4, rayas)]}]
    grupos = [{"id": "mascota", "pivote": MASCOTA, "anim": FLOTAR, "piezas": piezas},
              {"id": "cabeza", "padre": "mascota", "pivote": [0, -0.8, -3.2],
               "anim": {"ry": anim(amp=16, frec=0.18), "rz": anim(amp=5, frec=0.27)}, "piezas": cabeza}]
    return {"hueso": "jugador", "grupos": grupos + cola + _patas(naranja, blanco, ((-1.6, -3.0), (0.6, -3.0), (-1.6, 1.6), (0.6, 1.6)))}


def perrito():
    cafe, crema, oreja, rosa = "#C68B59", "#F5DEB3", "#7B4A2A", "#E57373"
    piezas = [caja(-1.9, -1.8, -3.6, 3.8, 3.6, 7.0, cafe), caja(-1.5, 0.4, -3.0, 3.0, 1.4, 5.4, crema),
              caja(-1.95, -1.9, -3.7, 3.9, 1.0, 1.2, "#D32F2F"),                         # collar
              caja(-0.5, -1.0, -3.95, 1.0, 1.0, 0.4, "#FFD54A", brillo=True)]           # placa
    cabeza = [caja(-2.0, -3.6, -3.0, 4.0, 3.6, 3.4, cafe), caja(-1.2, -1.6, -4.8, 2.4, 1.8, 2.0, crema),
              caja(-0.5, -1.9, -5.0, 1.0, 0.7, 0.4, "#222222"),                            # nariz
              caja(-0.6, -0.1, -4.6, 1.2, 1.0, 0.4, rosa),                                 # lengua
              caja(-1.3, -2.8, -3.1, 0.7, 0.7, 0.2, "#111111"), caja(0.6, -2.8, -3.1, 0.7, 0.7, 0.2, "#111111")]
    orejas = []
    for lado in (1, -1):
        orejas.append({"id": "oreja" + ("d" if lado > 0 else "i"), "padre": "cabeza", "pivote": [2.0 * lado, -3.4, -1.5],
                       "rot": [0, 0, 18 * lado], "anim": {"rz": anim(amp=8 * lado, frec=0.9, mov=10 * lado)},
                       "piezas": [caja(-0.4 if lado > 0 else -0.6, 0, -1.0, 1.0, 3.0, 2.0, oreja)]})
    cola = {"id": "cola", "padre": "mascota", "pivote": [0, -1.4, 3.3], "rot": [-50, 0, 0],
            "anim": {"ry": anim(amp=35, frec=2.6)}, "piezas": [caja(-0.45, -0.45, 0, 0.9, 0.9, 3.0, cafe)]}
    grupos = [{"id": "mascota", "pivote": MASCOTA, "anim": FLOTAR, "piezas": piezas},
              {"id": "cabeza", "padre": "mascota", "pivote": [0, -1.0, -3.4],
               "anim": {"ry": anim(amp=14, frec=0.2), "rz": anim(amp=6, frec=0.31)}, "piezas": cabeza}]
    return {"hueso": "jugador",
            "grupos": grupos + orejas + [cola] + _patas(cafe, crema, ((-1.7, -3.2), (0.7, -3.2), (-1.7, 1.8), (0.7, 1.8)))}


def buho():
    cafe, cafe2, crema, ojo, pico = "#8D6E63", "#5D4037", "#EFE3C8", "#FFC107", "#F57C00"
    piezas = [caja(-2.4, -3.2, -2.2, 4.8, 6.0, 4.4, cafe), caja(-1.8, -1.2, -2.35, 3.6, 3.6, 0.3, crema)]
    for k, (x, y) in enumerate(((-1.2, -0.6), (0.4, -0.6), (-0.4, 0.6), (-1.2, 1.6), (0.4, 1.6))):
        piezas.append(caja(x, y, -2.45, 0.8, 0.4, 0.15, cafe2))                           # plumitas del pecho
    piezas += [caja(-2.0, -6.4, -2.0, 4.0, 3.4, 4.0, cafe),                                 # cabeza
               caja(-1.8, -5.6, -2.15, 1.6, 1.6, 0.2, ojo), caja(0.2, -5.6, -2.15, 1.6, 1.6, 0.2, ojo),
               caja(-1.3, -5.1, -2.25, 0.7, 0.7, 0.15, "#111111"), caja(0.7, -5.1, -2.25, 0.7, 0.7, 0.15, "#111111"),
               caja(-0.35, -4.2, -2.6, 0.7, 0.9, 0.6, pico),
               caja(-2.0, -7.4, -1.0, 1.0, 1.2, 1.0, cafe2), caja(1.0, -7.4, -1.0, 1.0, 1.2, 1.0, cafe2),   # penachos
               caja(-1.0, 2.8, -1.2, 0.8, 0.6, 1.2, pico), caja(0.2, 2.8, -1.2, 0.8, 0.6, 1.2, pico)]       # patas
    alas = []
    for lado in (1, -1):
        alas.append({"id": "ala" + ("d" if lado > 0 else "i"), "padre": "mascota", "pivote": [2.4 * lado, -2.6, 0],
                     "anim": {"rz": anim(amp=-35 * lado, frec=1.3)},
                     "piezas": [caja(0 if lado > 0 else -0.6, 0, -1.8, 0.6, 4.6, 3.6, cafe2)]})
    return {"hueso": "jugador", "grupos": [{"id": "mascota", "pivote": [MASCOTA[0], MASCOTA[1] + 1, MASCOTA[2]],
                                            "anim": dict(FLOTAR, rz=anim(amp=4, frec=0.6)), "piezas": piezas}] + alas}


def fantasma():
    blanco = "#F4F6FF"
    piezas = [caja(-2.6, -4.0, -2.4, 5.2, 6.4, 4.8, blanco, alfa=0.78),
              caja(-2.2, -4.8, -2.0, 4.4, 0.9, 4.0, blanco, alfa=0.78),
              caja(-1.6, -2.6, -2.55, 1.0, 1.4, 0.2, "#1A1A2E"), caja(0.6, -2.6, -2.55, 1.0, 1.4, 0.2, "#1A1A2E"),
              caja(-0.6, -0.4, -2.55, 1.2, 1.0, 0.2, "#1A1A2E"),
              caja(-2.0, -1.0, -2.5, 0.8, 0.5, 0.1, "#FF9EC7", alfa=0.8), caja(1.2, -1.0, -2.5, 0.8, 0.5, 0.1, "#FF9EC7", alfa=0.8)]
    flecos = []
    for k, x in enumerate((-2.6, -0.85, 0.9)):                       # orilla de abajo que ondula
        flecos.append({"id": "fleco%d" % k, "padre": "mascota", "pivote": [x + 0.85, 2.4, 0],
                       "anim": {"rx": anim(amp=14, frec=1.1, fase=k * 1.3)},
                       "piezas": [caja(-0.85, 0, -2.4, 1.7, 1.6 + (k % 2) * 0.6, 4.8, blanco, alfa=0.7)]})
    return {"hueso": "jugador", "grupos": [{"id": "mascota", "pivote": MASCOTA,
                                            "anim": {"y": anim(amp=1.8, frec=0.35), "ry": anim(amp=12, frec=0.15)},
                                            "piezas": piezas}] + flecos}


def axolote():
    rosa, rosa2, branquia, ojo = "#F8A5C2", "#F78FB3", "#E84393", "#2D3436"
    piezas = [caja(-1.6, -1.4, -3.8, 3.2, 2.6, 7.0, rosa), caja(-1.2, 0.6, -3.2, 2.4, 0.8, 5.6, rosa2)]
    cabeza = [caja(-2.2, -2.0, -3.4, 4.4, 2.8, 3.4, rosa), caja(-1.8, -1.2, -3.55, 0.6, 0.6, 0.2, ojo),
              caja(1.2, -1.2, -3.55, 0.6, 0.6, 0.2, ojo), caja(-0.8, 0.2, -3.5, 1.6, 0.3, 0.15, branquia)]
    branquias = []
    for lado in (1, -1):
        for k, y in enumerate((-2.0, -1.1, -0.2)):
            branquias.append({"id": "br%s%d" % ("d" if lado > 0 else "i", k), "padre": "cabeza",
                              "pivote": [2.2 * lado, y, -1.4], "rot": [0, 25 * lado, (-30 + k * 30) * lado],
                              "anim": {"rz": anim(amp=10 * lado, frec=1.2, fase=k * 0.8)},
                              "piezas": [caja(0 if lado > 0 else -2.2, -0.3, -0.3, 2.2, 0.6, 0.6, branquia)]})
    cola = {"id": "cola", "padre": "mascota", "pivote": [0, -0.4, 3.2], "anim": {"ry": anim(amp=24, frec=0.9)},
            "piezas": [caja(-0.3, -1.6, 0, 0.6, 3.0, 4.2, rosa2)]}
    grupos = [{"id": "mascota", "pivote": MASCOTA, "anim": FLOTAR, "piezas": piezas},
              {"id": "cabeza", "padre": "mascota", "pivote": [0, 0, -3.8], "anim": {"ry": anim(amp=10, frec=0.25)},
               "piezas": cabeza}]
    return {"hueso": "jugador",
            "grupos": grupos + branquias + [cola] + _patas(rosa, rosa2, ((-1.6, -2.8), (0.6, -2.8), (-1.6, 1.6), (0.6, 1.6)),
                                                          alto=1.6, amp=24, frec=1.1)}


def mascotas():
    """Mascotas de todo el año."""
    return {
        "dragoncito": alebrije("#C62828", ["#FF7043", "#FFB300", "#8E1B1B", "#FF7043"], "#D32F2F", "#FFB300",
                               "#3E2723", ["#8E1B1B", "#B71C1C", "#FF7043", "#8E1B1B", "#B71C1C"], "#FF6D00"),
        "gatito": gatito(),
        "perrito": perrito(),
        "buho": buho(),
        "fantasma": fantasma(),
        "axolote": axolote(),
    }


def festivos():
    c = {
        # Día de Muertos
        "alebrije": alebrije("#E4007C", ["#19B5C8", "#FFC21A", "#FF8F1F", "#2FB36B"], "#8E3CCB", "#FFC21A", "#FF8F1F",
                             ["#19B5C8", "#FFC21A", "#E4007C", "#2FB36B", "#FF8F1F"], "#FF8F1F"),
        "alebrije_noche": alebrije("#2A1B5C", ["#00E5FF", "#FF2E97", "#B9F227", "#FFD60A"], "#00B3C7", "#FF2E97",
                                   "#B9F227", ["#00E5FF", "#FF2E97", "#B9F227", "#7C4DFF", "#FFD60A"], "#00E5FF"),
        "corona_cempasuchil": corona_cempasuchil(),
        # Fiestas Patrias
        "aguila": aguila(),
        "sombrero_charro": sombrero_charro(),
        "sarape": sarape(),
        "maracas": maracas(),
        "jarrito": jarrito(),
        # Navidad
        "rodolfo": rodolfo(),
        "gorro_navideno": gorro_navideno(),
        "cuernos_reno": cuernos_reno(),
        "bufanda_navidena": bufanda_navidena(),
        "baston_dulce": baston_dulce(),
        "nieve": nieve(),
    }
    temporadas = {"alebrije": "muertos", "alebrije_noche": "muertos", "corona_cempasuchil": "muertos",
                  "aguila": "patrias", "sombrero_charro": "patrias", "sarape": "patrias", "maracas": "patrias",
                  "jarrito": "patrias", "rodolfo": "navidad", "gorro_navideno": "navidad", "cuernos_reno": "navidad",
                  "bufanda_navidena": "navidad", "baston_dulce": "navidad", "nieve": "navidad"}
    return {k: _temporada(v, temporadas[k]) for k, v in c.items()}

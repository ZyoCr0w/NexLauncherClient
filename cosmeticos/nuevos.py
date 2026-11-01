"""Cosméticos de NexClient 4.5: alas de dragón articuladas, alas de ángel de plumas redondeadas, capas 3D que se
curvan al moverse, bandanas animadas y escudos con forma propia. Lo usa generar.py.

Todo se dibuja con «pixeles» (planos de cubitos) y «caja»: las formas se calculan aquí (polígonos, círculos,
elipses) y se pasan a filas de caracteres con una paleta.
"""
import math


def anim(**k):
    return k


# ---------------------------------------------------------------- utilidades de dibujo
def _dentro(poli, x, y):
    """Punto dentro de un polígono (regla par-impar)."""
    d = False
    n = len(poli)
    for i in range(n):
        x1, y1 = poli[i]
        x2, y2 = poli[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            d = not d
    return d


class Lienzo:
    """Rejilla de píxeles de lado «tam» en unidades del modelo (Y hacia abajo)."""

    def __init__(self, x0, y0, x1, y1, tam):
        self.x0, self.y0, self.tam = x0, y0, tam
        self.w = max(1, int(math.ceil((x1 - x0) / tam)))
        self.h = max(1, int(math.ceil((y1 - y0) / tam)))
        self.p = [["."] * self.w for _ in range(self.h)]

    def centro(self, i, j):
        return self.x0 + (i + 0.5) * self.tam, self.y0 + (j + 0.5) * self.tam

    def pintar(self, ch, prueba):
        for j in range(self.h):
            for i in range(self.w):
                x, y = self.centro(i, j)
                r = prueba(x, y, i, j)
                if r:
                    self.p[j][i] = r if isinstance(r, str) else ch

    def poligono(self, poli, ch):
        self.pintar(ch, lambda x, y, i, j: _dentro(poli, x, y))

    def elipse(self, cx, cy, rx, ry, ch):
        self.pintar(ch, lambda x, y, i, j: ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1)

    def filas(self, solo=None):
        """Filas como texto; con «solo» se dejan únicamente esos caracteres (para una capa aparte)."""
        out = []
        for fila in self.p:
            out.append("".join(c if (solo is None or c in solo) else "." for c in fila))
        return out

    def pieza(self, paleta, z=0.0, prof=0.4, plano="xy", solo=None, **extra):
        f = self.filas(solo)
        if not any(c != "." for fila in f for c in fila):
            return None
        p = {"pixeles": {"origen": [round(self.x0, 3), round(self.y0, 3), z], "plano": plano, "filas": f,
                         "paleta": {k: v for k, v in paleta.items() if solo is None or k in solo},
                         "tam": self.tam, "prof": prof}}
        p.update(extra)
        return p


def _rot2(x, y, grados):
    a = math.radians(grados)
    return x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a)


def _piezas(*ps):
    return [p for p in ps if p]


# ---------------------------------------------------------------- alas de dragón articuladas
def alas_dragon_nuevas(hueso_c, membrana, membrana2, brillo=False, alfa=1.0, parpadeo=None, garra=None):
    """Brazo → mano → tres dedos, con la membrana estirada entre ellos y un borde festoneado (como un murciélago).
    El brazo sube hacia afuera; la mano se dobla un poco al aletear (la punta va con retraso)."""
    brazo_l, mano_l = 8.0, 6.5
    dedos = [(52, 12.5), (92, 11.0), (128, 9.0)]  # ángulo respecto a la mano (hacia abajo) y largo
    w = (mano_l, 0.0)
    puntas = [(w[0] + l * math.cos(math.radians(a)), w[1] + l * math.sin(math.radians(a))) for a, l in dedos]

    def comba(a, b, hacia, k=0.28):  # punto medio del borde, metido hacia «hacia» (borde festoneado)
        mx, my = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
        return mx + (hacia[0] - mx) * k, my + (hacia[1] - my) * k

    # membrana de la mano (en el espacio de la mano)
    codo = (0.0, 0.0)
    poli_mano = [codo, w, puntas[0], comba(puntas[0], puntas[1], w), puntas[1], comba(puntas[1], puntas[2], w),
                 puntas[2], comba(puntas[2], codo, w, 0.22)]
    lm = Lienzo(-1.0, -0.5, 16, 19, 0.5)
    lm.poligono(poli_mano, "m")
    lm.pintar("n", lambda x, y, i, j: lm.p[j][i] == "m" and math.hypot(x - w[0], y - w[1]) > 6.5)
    # membrana del brazo hasta el cuerpo (en el espacio del brazo, que va girado -38° respecto a la raíz)
    giro_brazo, giro_mano = -46.0, -8.0
    p3 = _rot2(*puntas[2], giro_mano)
    p3 = (p3[0] + brazo_l, p3[1])
    cuerpo = _rot2(0.0, 9.0, -giro_brazo)  # punto de la espalda (raíz: 0, 9) visto desde el brazo
    poli_brazo = [(0.0, 0.0), (brazo_l, 0.0), p3, comba(p3, cuerpo, (brazo_l * 0.5, 0.0), 0.25), cuerpo]
    lb = Lienzo(-2.5, -0.5, 18, 20, 0.5)
    lb.poligono(poli_brazo, "m")
    lb.pintar("n", lambda x, y, i, j: lb.p[j][i] == "m" and math.hypot(x - brazo_l, y) > 7.5)

    pal = {"m": membrana, "n": membrana2}
    extra_m = {"alfa": alfa}
    if brillo:
        extra_m["brillo"] = True
    if parpadeo:
        extra_m["parpadeo"] = parpadeo
    hueso = {"color": hueso_c}
    grupos = [
        {"id": "ala", "espejo": True, "pivote": [1.0, 1.2, 2.4], "rot": [4, -22, 0],
         "anim": {"ry": anim(amp=20, frec=0.38, amp_mov=0.4, frec_mov=0.25, aire=26),
                  "rz": anim(amp=5, frec=0.38, fase=0.6)}, "piezas": []},
        {"id": "brazo", "padre": "ala", "espejo": True, "pivote": [0, 0, 0], "rot": [0, 0, giro_brazo],
         "piezas": _piezas({"caja": [-0.6, -0.65, -0.55, brazo_l + 0.6, 1.3, 1.1], **hueso},
                           lb.pieza(pal, z=-0.15, prof=0.3, **extra_m))},
        {"id": "mano", "padre": "brazo", "espejo": True, "pivote": [brazo_l, 0, 0], "rot": [0, 0, giro_mano],
         "anim": {"rz": anim(amp=7, frec=0.38, fase=-0.9)},
         "piezas": _piezas({"caja": [0, -0.5, -0.5, mano_l, 1.0, 1.0], **hueso},
                           {"caja": [mano_l - 0.4, -1.6, -0.45, 0.9, 1.4, 0.9], "color": garra or hueso_c},
                           lm.pieza(pal, z=-0.15, prof=0.3, **extra_m))},
    ]
    for k, (ang, largo) in enumerate(dedos):
        grupos.append({"id": f"dedo{k}", "padre": "mano", "espejo": True, "pivote": [mano_l, 0, 0], "rot": [0, 0, ang],
                       "piezas": [{"caja": [0, -0.32, -0.35, largo, 0.64, 0.7], **hueso},
                                  {"caja": [largo - 0.2, -0.4, -0.4, 0.9, 0.8, 0.8], "color": garra or hueso_c}]})
    return {"hueso": "cuerpo", "grupos": grupos}


# ---------------------------------------------------------------- alas de ángel de plumas redondeadas
def alas_angel_nuevas(blanco, claro, gris, sombra, brillo=False, arcoiris=False, tam=0.6):
    """Hileras de plumas ovaladas: abajo las largas (primarias) y arriba las cortas, una encima de otra, con un
    borde gris en cada pluma: se ve redondeado y en capas, como un ala de verdad."""
    lz = Lienzo(-0.5, -9.0, 19.5, 15.0, tam)
    # el borde de arriba del ala: un arco que sube hacia afuera
    def arriba(x):
        return -1.5 - 6.5 * math.sin(min(1.0, x / 16.0) * math.pi * 0.62)
    hileras = [  # (largo de cada pluma, ancho, cuántas, de qué tan abajo empieza, color)
        (11.0, 2.3, 8, 6.0, claro),
        (8.0, 2.3, 8, 3.6, blanco),
        (5.5, 2.2, 8, 1.6, blanco),
        (3.2, 2.0, 7, 0.0, blanco),
    ]
    marcas = {"c": claro, "w": blanco}
    for largo, ancho, n, baja, color in hileras:
        ch = "c" if color == claro else "w"
        for k in range(n - 1, -1, -1):
            u = (k + 0.5) / n
            x = 0.8 + u * 15.5
            y = arriba(x) + baja + largo * 0.5
            ang = math.radians(18 + 30 * u)  # las de afuera se inclinan más
            rx, ry = ancho * 0.5, largo * 0.5

            def prueba(px, py, i, j, x=x, y=y, ang=ang, rx=rx, ry=ry, ch=ch):
                dx, dy = px - x, py - y
                a = dx * math.cos(ang) + dy * math.sin(ang)
                b = -dx * math.sin(ang) + dy * math.cos(ang)
                d = (a / rx) ** 2 + (b / ry) ** 2
                if d <= 1:
                    return "g" if d > 0.55 else ch
                return None
            lz.pintar(ch, prueba)
    # sombra en el nacimiento del ala (junto al cuerpo)
    lz.pintar("s", lambda x, y, i, j: lz.p[j][i] != "." and x < 1.6)
    pal = {"w": blanco, "c": claro, "g": gris, "s": sombra}
    extra = {}
    if brillo:
        extra["brillo"] = True
    if arcoiris:
        extra.update(arcoiris=True, arcoiris_vel=0.25, desfase=0.025)
    return {"hueso": "cuerpo", "grupos": [
        {"id": "ala", "espejo": True, "pivote": [1.0, 1.6, 2.4], "rot": [6, -24, -22],
         "anim": {"ry": anim(amp=10, frec=0.42, amp_mov=0.6, frec_mov=0.4, aire=26), "rz": anim(amp=3, frec=0.42, fase=1.2)},
         "piezas": _piezas(lz.pieza(pal, z=0.0, prof=0.5, **extra))}]}


# ---------------------------------------------------------------- capas 3D que se curvan
def capa_modelo(dibujo, paleta, brillan=(), parpadeo=None, arcoiris=False, cuello="#1A1A1A"):
    """10 × 16 píxeles en 4 tramos encadenados: al caminar el primero se abre hacia atrás y los demás lo siguen con
    un poco de retraso (la tela ondea). Esquinas de abajo redondeadas."""
    assert len(dibujo) == 16 and all(len(f) == 10 for f in dibujo), "la capa es de 10 × 16"
    filas = [list(f) for f in dibujo]
    for j, i in ((15, 0), (15, 9), (15, 1), (15, 8), (14, 0), (14, 9)):  # esquinas redondeadas
        filas[j][i] = "."
    dibujo = ["".join(f) for f in filas]
    normales = {k: v for k, v in paleta.items() if k not in brillan}
    brillantes = {k: v for k, v in paleta.items() if k in brillan}
    grupos = []
    for t in range(4):
        tramo = dibujo[t * 4:(t + 1) * 4]
        piezas = []
        base = {"pixeles": {"origen": [-5, 0, 0], "plano": "xy", "filas": tramo, "paleta": normales, "tam": 1.0,
                            "prof": 0.38}}
        if arcoiris:
            base.update(arcoiris=True, arcoiris_vel=0.35, desfase=0.03)
        piezas.append(base)
        if brillantes and any(c in brillantes for f in tramo for c in f):
            b = {"pixeles": {"origen": [-5, 0, -0.04], "plano": "xy", "filas": tramo, "paleta": brillantes,
                             "tam": 1.0, "prof": 0.46}, "brillo": True}
            if parpadeo:
                b["parpadeo"] = parpadeo
            piezas.append(b)
        if t == 0:
            piezas.append({"caja": [-5.2, -0.6, -0.3, 10.4, 1.0, 0.9], "color": cuello})  # el cuello
        g = {"id": f"tramo{t}", "piezas": piezas}
        if t == 0:
            g.update(pivote=[0, 0.2, 2.35], rot=[5, 0, 0],
                     anim={"rx": anim(amp=1.5, frec=0.5, mov=38, aire=22, agachado=22)})
        else:
            g.update(padre=f"tramo{t - 1}", pivote=[0, 4, 0], rot=[3, 0, 0],
                     anim={"rx": anim(amp=2.5, frec=0.8, fase=-0.9 * t, mov=7 * t, amp_mov=1.0)})
        grupos.append(g)
    return {"hueso": "cuerpo", "capa_modelo": True, "grupos": grupos}


def _capa_dibujo(fondo, borde=None, emblema=None, abajo=None):
    filas = [[fondo] * 10 for _ in range(16)]
    if borde:
        for j in range(16):
            filas[j][0] = filas[j][9] = borde
        for i in range(10):
            filas[15][i] = borde
    for (j, f) in (emblema or []):
        for i, c in enumerate(f):
            if c != ".":
                filas[j][i] = c
    if abajo:
        abajo(filas)
    return ["".join(f) for f in filas]


def capas_nuevas():
    real = _capa_dibujo("r", "o", [(0, "wkwwkwwkww"), (1, "wwwkwwkwwk")])
    estrellas = _capa_dibujo("n", None, [(2, "..s....s.."), (4, "....s....."), (5, ".s......s."), (7, "...s..s..."),
                                          (9, "s.......s."), (10, "....s....."), (12, ".s....s..."), (14, "...s....s.")])
    # la capa se mira desde atrás: el dibujo va al revés para que la N se lea bien
    nex = _capa_dibujo("k", "g", [(j, f[::-1]) for j, f in [(3, "..nn...nn."), (4, "..nnn..nn."), (5, "..nn.n.nn."),
                                                             (6, "..nn..nnn."), (7, "..nn...nn.")]])
    llamas = []
    for j in range(16):
        f = ""
        for i in range(10):
            onda = math.sin(i * 1.3 + j * 0.4) * 1.6
            nivel = j + onda
            f += "y" if nivel > 12 else "a" if nivel > 7.5 else "r" if nivel > 3 else "d"
        llamas.append(f)
    return {
        "capa_real": capa_modelo(real, {"r": "#B71C1C", "o": "#F2C14E", "w": "#FAFAFA", "k": "#212121"},
                                 brillan=("o",), parpadeo={"frec": 0.6, "min": 0.8}),
        "capa_estelar": capa_modelo(estrellas, {"n": "#141A3A", "s": "#FFFFFF"}, brillan=("s",),
                                    parpadeo={"frec": 1.1, "min": 0.25, "desfase": 1.7}),
        "capa_nex": capa_modelo(nex, {"k": "#0E0E10", "g": "#1E8E4A", "n": "#2BD96B"}, brillan=("n", "g"),
                                parpadeo={"frec": 0.7, "min": 0.65, "desfase": 0.35}),
        "capa_llamas": capa_modelo(llamas, {"d": "#4A0E0E", "r": "#C62828", "a": "#FF6D00", "y": "#FFD54F"},
                                   brillan=("a", "y"), parpadeo={"frec": 3.2, "min": 0.6, "desfase": 0.55}),
        "capa_arcoiris": capa_modelo(_capa_dibujo("x"), {"x": "#FFFFFF"}, arcoiris=True),
    }


# ---------------------------------------------------------------- bandanas animadas
def bandana(banda, llama=None, punta=None, raya=None, puntos=None, arcoiris=False, colas_largas=False, oscura=None):
    """Una tira alrededor de la frente, con llamas que suben y se mueven (parpadeo que corre de lado a lado), y un
    nudo atrás con dos puntas que se mecen al caminar.

    Va por fuera de la segunda capa de la skin (el «sombrero», que sobresale 0.5 de la cabeza): si no, los píxeles
    de esa capa se encimaban con la tira."""
    cols, tam = 16, 0.6
    dentro, prof = 4.52, 0.28  # cara interior justo fuera del sombrero (4.5); la exterior queda en 4.8
    y0 = -8.2
    filas = []
    for j in range(6):
        f = ""
        for i in range(cols):
            if j < 2:  # puntas de las llamas sobre la tira
                alto = 0.5 + 0.5 * math.sin(i * 1.7)
                f += ("t" if (j == 1 and alto > 0.25) or (j == 0 and alto > 0.8) else ".") if llama else "."
            else:
                c = "b"
                if llama and ((j == 2 and i % 3 != 1) or (j == 3 and i % 4 == 0)):
                    c = "t"
                if raya and j == 4:
                    c = "r"
                if puntos and (i + j) % 4 == 0 and j in (3, 5):
                    c = "p"
                f += c
        filas.append(f)
    pal_base = {"b": banda}
    if raya:
        pal_base["r"] = raya
    if puntos:
        pal_base["p"] = puntos
    pal_llama = {"t": llama} if llama else {}

    def caras(paleta, extra, dz=0.0):
        out = []
        ancho = cols * tam
        lado = ancho / 2
        fuera = dentro + prof
        for origen, plano in (([-lado, y0, -fuera - dz], "xy"), ([-lado, y0, dentro + dz], "xy"),
                              ([-fuera - dz, y0, -lado], "zy"), ([dentro + dz, y0, -lado], "zy")):
            p = {"pixeles": {"origen": origen, "plano": plano, "filas": filas, "paleta": paleta, "tam": tam,
                             "prof": prof}}
            p.update(extra)
            out.append(p)
        return out
    extra_base = {"arcoiris": True, "arcoiris_vel": 0.4, "desfase": 0.05} if arcoiris else {}
    piezas = caras(pal_base, extra_base)
    if pal_llama:
        piezas += caras(pal_llama, {"brillo": True, "parpadeo": {"frec": 2.6, "min": 0.45, "desfase": 0.9}}, 0.02)
    largo = 4.8 if colas_largas else 3.4
    color_cola = oscura or banda
    cola = {"caja": [-0.7, 0, -0.15, 1.4, largo, 0.3], "color": color_cola}
    if arcoiris:
        cola.update(arcoiris=True, arcoiris_vel=0.4)
    grupos = [
        {"id": "banda", "piezas": piezas},
        {"id": "nudo", "pivote": [2.2, -5.8, 5.0], "piezas": [{"caja": [-0.9, -0.8, -0.2, 1.8, 1.6, 0.8],
                                                                "color": color_cola}]},
        {"id": "cola1", "padre": "nudo", "pivote": [0.3, 0.4, 0.4], "rot": [28, 0, 18],
         "anim": {"rx": anim(amp=5, frec=0.9, mov=35, aire=-15), "rz": anim(amp=7, frec=0.7)}, "piezas": [cola]},
        {"id": "cola2", "padre": "nudo", "pivote": [-0.3, 0.4, 0.4], "rot": [38, 0, -12],
         "anim": {"rx": anim(amp=6, frec=0.8, fase=1.4, mov=40, aire=-15), "rz": anim(amp=8, frec=0.6, fase=0.7)},
         "piezas": [dict(cola, caja=[-0.6, 0, -0.15, 1.2, largo * 0.85, 0.3])]},
    ]
    return {"hueso": "cabeza", "grupos": grupos}


def bandanas_nuevas():
    return {
        "bandana_fuego": bandana("#FF6D00", llama="#FFD54F", oscura="#E65100"),
        "bandana_fuego_azul": bandana("#2962FF", llama="#80D8FF", oscura="#1A47C9"),
        "bandana_ninja": bandana("#141414", raya="#D32F2F", colas_largas=True),
        "bandana_arcoiris": bandana("#FFFFFF", arcoiris=True),
        "bandana_nex": bandana("#0E0E10", llama="#2BD96B", oscura="#0E0E10"),
        "bandana_pirata": bandana("#C62828", puntos="#FAFAFA", oscura="#9E1B1B"),
    }


# ---------------------------------------------------------------- escudos con forma propia
TAM_E = 0.75  # píxel de los escudos: la placa de 16 × 30 ocupa casi lo mismo que la de Minecraft


def _lienzo_escudo():
    return Lienzo(-6.0, -11.25, 6.0, 11.25, TAM_E)


def _escudo(lz, paleta, brillan=(), parpadeo=None, grupos_extra=None, trasera="#141416"):
    """Frente (dibujo), reverso del mismo contorno (grosor) y el asa."""
    normales = {k: v for k, v in paleta.items() if k not in brillan}
    brillantes = {k: v for k, v in paleta.items() if k in brillan}
    contorno = Lienzo(lz.x0, lz.y0, lz.x0 + lz.w * lz.tam, lz.y0 + lz.h * lz.tam, lz.tam)
    contorno.p = [["z" if c != "." else "." for c in fila] for fila in lz.p]
    piezas = _piezas(contorno.pieza({"z": trasera}, z=-2.0, prof=0.7),
                     lz.pieza(normales, z=-2.45, prof=0.45, solo=set(normales)),
                     {"caja": [-1, -3, -1.3, 2, 6, 6], "color": "#4E342E"})
    if brillantes:
        b = lz.pieza(brillantes, z=-2.55, prof=0.55, solo=set(brillantes), brillo=True)
        if b and parpadeo:
            b["parpadeo"] = parpadeo
        piezas += _piezas(b)
    return {"hueso": "escudo", "grupos": [{"id": "escudo", "piezas": piezas}] + (grupos_extra or [])}


def _cometa(x, y, ancho=5.6, arriba=-10.4, cintura=2.0, abajo=10.8):
    """Escudo de cometa (heater): arriba casi recto, lados que bajan y terminan en punta redondeada."""
    if y < arriba + 0.15 * (x / ancho) ** 2 * 1.5 or y > abajo:
        return False
    if y <= cintura:
        return abs(x) <= ancho
    k = (y - cintura) / (abajo - cintura)
    return abs(x) <= ancho * (1 - k ** 1.7) ** 0.9


def escudo_cometa(fondo, borde, gema, simbolo=None, simbolo_c=None, brilla_simbolo=True):
    lz = _lienzo_escudo()
    lz.pintar("f", lambda x, y, i, j: _cometa(x, y))
    lz.pintar("b", lambda x, y, i, j: lz.p[j][i] == "f" and not _cometa(x, y, 4.85, -9.65, 1.7, 9.6))
    lz.elipse(0, -7.6, 1.1, 1.3, "g")
    lz.elipse(0, 7.4, 0.9, 1.1, "g")
    if simbolo:
        simbolo(lz)
    pal = {"f": fondo, "b": borde, "g": gema}
    brillan = ["g"]
    if simbolo_c:
        pal["s"] = simbolo_c
        if brilla_simbolo:
            brillan.append("s")
    return _escudo(lz, pal, brillan=brillan, parpadeo={"frec": 0.8, "min": 0.6, "desfase": 0.15})


def _letra_n(lz):
    lz.pintar("s", lambda x, y, i, j: (-1.6 <= y <= 3.2) and (abs(x + 2.0) < 0.8 or abs(x - 2.0) < 0.8 or
                                                          abs((x + 2.0) - (y + 1.6) * 4.0 / 4.8) < 0.7))


def _cruz(lz):
    lz.pintar("s", lambda x, y, i, j: (abs(x) < 0.9 and -6 <= y <= 7) or (abs(y + 1.5) < 0.9 and abs(x) < 4.2))


def escudo_runas():
    """Disco morado con un anillo de runas que gira despacio y un cristal en el centro."""
    lz = _lienzo_escudo()
    lz.pintar("f", lambda x, y, i, j: x * x + y * y <= 6.0 ** 2)
    lz.pintar("b", lambda x, y, i, j: 5.2 ** 2 < x * x + y * y <= 6.0 ** 2)
    lz.pintar("c", lambda x, y, i, j: abs(x) + abs(y) * 0.8 <= 1.9)
    runas = Lienzo(-6.0, -6.0, 6.0, 6.0, TAM_E)
    runas.pintar("r", lambda x, y, i, j: 3.4 ** 2 <= x * x + y * y <= 4.7 ** 2 and (i * 7 + j * 13) % 5 in (0, 2))
    anillo = {"id": "runas", "pivote": [0, 0, 0], "anim": {"rz": anim(vel=25)},
              "piezas": _piezas(runas.pieza({"r": "#E1BEFF"}, z=-2.6, prof=0.5, brillo=True,
                                            parpadeo={"frec": 1.3, "min": 0.4, "desfase": 0.6}))}
    return _escudo(lz, {"f": "#2A1452", "b": "#7B3FD1", "c": "#7FFFF4"}, brillan=("b", "c"),
                   parpadeo={"frec": 0.9, "min": 0.6}, grupos_extra=[anillo], trasera="#120A24")


def escudo_cristal():
    """Estrella de hielo de ocho puntas que late."""
    lz = _lienzo_escudo()

    def estrella(x, y, escala=1.0):
        r = math.hypot(x, y * 0.8)
        a = math.atan2(y * 0.8, x)
        lim = escala * (3.4 + 3.0 * abs(math.cos(4 * a)) ** 6)
        return r <= lim
    lz.pintar("f", lambda x, y, i, j: estrella(x, y))
    lz.pintar("c", lambda x, y, i, j: estrella(x, y, 0.62))
    lz.pintar("w", lambda x, y, i, j: estrella(x, y, 0.3))
    return _escudo(lz, {"f": "#29B6F6", "c": "#81D4FA", "w": "#FFFFFF"}, brillan=("c", "w"),
                   parpadeo={"frec": 0.9, "min": 0.55, "desfase": 0.12}, trasera="#0D47A1")


def escudo_cabeza_dragon():
    """La cabeza de un dragón vista de frente: cuernos, ojos que brillan y una gema en la frente."""
    lz = _lienzo_escudo()
    cara = [(-5.4, -8.6), (-3.6, -6.2), (-4.8, -3.0), (-5.6, 0.4), (-4.2, 4.2), (-2.8, 8.0), (-1.4, 10.6),
            (0, 11.0), (1.4, 10.6), (2.8, 8.0), (4.2, 4.2), (5.6, 0.4), (4.8, -3.0), (3.6, -6.2), (5.4, -8.6),
            (2.8, -7.0), (1.4, -8.8), (0, -7.4), (-1.4, -8.8), (-2.8, -7.0)]
    lz.poligono(cara, "f")
    lz.pintar("e", lambda x, y, i, j: lz.p[j][i] == "f" and int((y + 11) / 1.5) % 2 == 0 and abs(x) > 2.4)
    for s in (-1, 1):
        lz.poligono([(s * 3.4, -6.4), (s * 5.9, -11.2), (s * 4.6, -6.0)], "h")
        lz.elipse(s * 2.3, -0.6, 1.0, 0.55, "o")
        lz.elipse(s * 0.9, 8.6, 0.35, 0.5, "k")
    lz.poligono([(0, -5.6), (0.9, -4.2), (0, -2.8), (-0.9, -4.2)], "g")
    return _escudo(lz, {"f": "#4A1F7A", "e": "#3A165F", "h": "#B0B0B8", "o": "#E040FB", "g": "#EA80FC",
                        "k": "#12081F"}, brillan=("o", "g"), parpadeo={"frec": 0.5, "min": 0.35}, trasera="#1C0B30")


def escudo_llama():
    """Con forma de llama: tres lenguas arriba y colores que suben y titilan."""
    lz = _lienzo_escudo()

    def llama(x, y):
        if y > 10.5 or abs(x) > 5.8:
            return False
        if y > 3.0:
            return x * x / 5.8 ** 2 + (y - 3.0) ** 2 / 7.5 ** 2 <= 1
        lenguas = -10.5 + 4.2 * (1 - abs(math.cos(x * 0.82))) + abs(x) * 0.35
        return y >= lenguas
    lz.pintar("r", lambda x, y, i, j: llama(x, y))
    lz.pintar("a", lambda x, y, i, j: llama(x, y) and llama(x * 1.35, y * 1.25 - 1.0))
    lz.pintar("y", lambda x, y, i, j: llama(x, y) and llama(x * 2.0, y * 1.7 - 2.5))
    return _escudo(lz, {"r": "#C62828", "a": "#FF6D00", "y": "#FFD54F"}, brillan=("a", "y"),
                   parpadeo={"frec": 4.0, "min": 0.6, "desfase": 0.5}, trasera="#3E0E0E")


def escudo_brillo_dorado():
    """Escudo de caballero dorado con una cruz roja y un brillo que lo cruza en diagonal."""
    lz = _lienzo_escudo()
    lz.pintar("f", lambda x, y, i, j: _cometa(x, y))
    lz.pintar("b", lambda x, y, i, j: lz.p[j][i] == "f" and not _cometa(x, y, 4.85, -9.65, 1.7, 9.6))
    _cruz(lz)
    lz.pintar("x", lambda x, y, i, j: lz.p[j][i] == "f" and abs((x + y * 0.6) % 7.0 - 3.5) < 0.6)
    return _escudo(lz, {"f": "#E0B23C", "b": "#9C7A1E", "s": "#C62828", "x": "#FFF3C4"}, brillan=("x",),
                   parpadeo={"frec": 0.7, "min": 0.0, "desfase": 0.35}, trasera="#5C4710")


def escudos_nuevos():
    return {
        "escudo_nex": escudo_cometa("#121214", "#2BD96B", "#2BD96B", _letra_n, "#2BD96B"),
        "escudo_noche": escudo_cometa("#1A1F5C", "#0B0D24", "#9C4DFF"),
        "escudo_dragon": escudo_cabeza_dragon(),
        "escudo_dorado": escudo_brillo_dorado(),
        "escudo_hielo": escudo_cristal(),
        "escudo_fuego": escudo_llama(),
        "escudo_obsidiana": escudo_runas(),
    }


def alas_nuevas():
    return {
        "alas_angel": alas_angel_nuevas("#FFFFFF", "#E9EDF2", "#9AA5B1", "#7D8894"),
        "alas_angel_oscuro": alas_angel_nuevas("#3A3A44", "#2C2C34", "#14141A", "#101014"),
        "alas_angel_arcoiris": alas_angel_nuevas("#FFFFFF", "#FFFFFF", "#D0D0D0", "#BBBBBB", arcoiris=True, tam=0.75),
        "alas_dragon": alas_dragon_nuevas("#2B0808", "#8E1B1B", "#B3261E"),
        "alas_dragon_negro": alas_dragon_nuevas("#B8B8BE", "#1A1A1F", "#26262C", garra="#E0E0E0"),
        "alas_dragon_hielo": alas_dragon_nuevas("#E0F7FA", "#4FC3F7", "#81D4FA", brillo=True, alfa=0.78,
                                                parpadeo={"frec": 0.6, "min": 0.8}),
        "alas_dragon_ender": alas_dragon_nuevas("#1A0F24", "#7B1FA2", "#B34DFF", brillo=True,
                                                parpadeo={"frec": 0.7, "min": 0.6}, garra="#E1BEFF"),
    }

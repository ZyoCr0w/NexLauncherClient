set -e
cd "$(dirname "$0")"
rm -rf build/a build/h && mkdir -p build/a build/h
javac --release 8 -nowarn -d build/a src/nexlauncher/agente/Agente.java src/nexlauncher/agente/Parche.java
javac --release 8 -nowarn -d build/h src/nexlauncher/agente/Hooks.java
printf "Premain-Class: nexlauncher.agente.Agente\n" > build/M.MF
cd build && rm -f nexskins-agente.jar nexskins-hooks.jar
jar cfm nexskins-agente.jar M.MF -C a .
jar cf nexskins-hooks.jar -C h .
ls -la *.jar

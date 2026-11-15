"""Host renderer and preset schema tests; does not emulate Android media codecs."""
import os, subprocess, urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=Path(os.environ.get('JAVA_HOME',str(Path.home()/'.jdks/corretto-21.0.13')))/'bin'
OUT=ROOT/'build/test-classes'
SRC=ROOT/'app/src/main/java/ru/ascii/wallpaper'
def run(args): subprocess.run([str(a) for a in args],cwd=ROOT,check=True)
OUT.mkdir(parents=True,exist_ok=True)
core=['AsciiScene','EffectOptions','RippleField','MediaTone','GridGeometry','LockClockPolicy','TallDigits']
tests=['SceneTest','FlexibleSettingsTest','NewFeaturesTest','LockClockTest','TallDigitsTest','NewModesTest']
run([JAVA/'javac.exe','-encoding','UTF-8','-d',OUT]+[SRC/(n+'.java') for n in core]+[ROOT/'tests'/(n+'.java') for n in tests])
for name in tests: run([JAVA/'java.exe','-Djava.awt.headless=true','-cp',OUT,name])
jar=ROOT/'.toolchain/json-test.jar'
if not jar.exists(): urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar',jar)
android=next((ROOT/'.toolchain/platform').rglob('android.jar'))
cp=os.pathsep.join(map(str,[jar,android,OUT]))
run([JAVA/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',OUT]+[SRC/(n+'.java') for n in ['PresetStore','Settings','DeviceProfile']]+[ROOT/'tests/PresetValidationTest.java'])
run([JAVA/'java.exe','-cp',cp,'ru.ascii.wallpaper.PresetValidationTest'])

"""Build a signed APK with the official SDK, without third-party app dependencies."""
from pathlib import Path
import os
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / 'build'
DIST = ROOT / 'dist'
SDK = ROOT / '.toolchain'
JAVA = Path(os.environ.get('JAVA_HOME', str(Path.home() / '.jdks/corretto-21.0.13')))

def run(args):
    args = [str(a) for a in args]
    print('> ' + Path(args[0]).name + ' ' + ' '.join(args[1:3]), flush=True)
    subprocess.run(args, check=True, cwd=ROOT)

def main():
    android = next((SDK / 'platform').rglob('android.jar'))
    bt = next((SDK / 'build-tools').rglob('aapt2.exe')).parent
    for folder in [BUILD, DIST, BUILD / 'generated', BUILD / 'classes', BUILD / 'dex']:
        folder.mkdir(parents=True, exist_ok=True)
    os.environ['JAVA_HOME'] = str(JAVA)
    os.environ['PATH'] = str(JAVA / 'bin') + os.pathsep + os.environ['PATH']
    source = ROOT / 'app/src/main'
    run([bt / 'aapt2.exe', 'compile', '--dir', source / 'res', '-o', BUILD / 'resources.zip'])
    run([bt / 'aapt2.exe', 'link', '-o', BUILD / 'base.apk', '-I', android,
         '--manifest', source / 'AndroidManifest.xml', '--java', BUILD / 'generated', '-A', source / 'assets',
         '--min-sdk-version', '26', '--target-sdk-version', '37', BUILD / 'resources.zip'])
    sources = list((source / 'java').rglob('*.java')) + list((BUILD / 'generated').rglob('*.java'))
    run([JAVA / 'bin/javac.exe', '-encoding', 'UTF-8', '--release', '8', '-classpath', android,
         '-d', BUILD / 'classes'] + sources)
    run([JAVA / 'bin/jar.exe', 'cf', BUILD / 'classes.jar', '-C', BUILD / 'classes', '.'])
    run([bt / 'd8.bat', '--release', '--lib', android, '--min-api', '26', '--output', BUILD / 'dex', BUILD / 'classes.jar'])
    # Repack once: appending with Python can change aapt2 ZIP flags without
    # updating matching local headers on asset entries.
    with zipfile.ZipFile(BUILD / 'base.apk') as resources, zipfile.ZipFile(BUILD / 'unsigned.apk', 'w') as apk:
        for entry in resources.infolist():
            apk.writestr(entry.filename, resources.read(entry), compress_type=entry.compress_type)
        for dex in (BUILD / 'dex').glob('*.dex'):
            apk.write(dex, dex.name, compress_type=zipfile.ZIP_DEFLATED)
    run([bt / 'zipalign.exe', '-f', '-p', '4', BUILD / 'unsigned.apk', BUILD / 'aligned.apk'])
    key = SDK / 'ascii-local.jks'
    if not key.exists():
        run([JAVA / 'bin/keytool.exe', '-genkeypair', '-keystore', key, '-storepass', 'android',
             '-keypass', 'android', '-alias', 'ascii', '-keyalg', 'RSA', '-keysize', '3072',
             '-validity', '10000', '-dname', 'CN=ASCII Flow Local, O=Personal, C=RU'])
    output = DIST / 'ASCII-Flow-1.3.apk'
    run([bt / 'apksigner.bat', 'sign', '--ks', key, '--ks-key-alias', 'ascii',
         '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', output, BUILD / 'aligned.apk'])
    run([bt / 'apksigner.bat', 'verify', '--verbose', output])
    run([bt / 'aapt2.exe', 'dump', 'badging', output])
    import hashlib
    (DIST / 'SHA256.txt').write_text(hashlib.sha256(output.read_bytes()).hexdigest() + '  ' + output.name + '\n')
    print('APK: ' + str(output), flush=True)

if __name__ == '__main__':
    main()

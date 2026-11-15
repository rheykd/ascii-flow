"""Download official Android SDK packages, checking repository SHA-1 digests."""
import concurrent.futures
import hashlib
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1] / '.toolchain'
BASE = 'https://dl.google.com/android/repository/'

def install(item):
    name, url, digest = item
    destination = ROOT / name
    if (destination / '.ready').exists():
        return str(destination)
    archive = ROOT / (name + '.zip')
    print('Downloading ' + url, flush=True)
    urllib.request.urlretrieve(BASE + url, archive)
    assert hashlib.sha1(archive.read_bytes()).hexdigest() == digest, 'Checksum mismatch: ' + url
    destination.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive) as z:
        z.extractall(destination)
    (destination / '.ready').touch()
    return str(destination)

if __name__ == '__main__':
    ROOT.mkdir(exist_ok=True)
    repo = ET.fromstring(urllib.request.urlopen(BASE + 'repository2-3.xml').read())
    wanted = {'platforms;android-37.0': 'platform', 'build-tools;37.0.0': 'build-tools', 'platform-tools': 'platform-tools'}
    jobs = []
    for package in repo:
        path = package.get('path')
        if path not in wanted or package.find('channelRef').get('ref') != 'channel-0':
            continue
        for archive in package.findall('./archives/archive'):
            if archive.findtext('host-os') not in (None, 'windows'):
                continue
            jobs.append((wanted[path], archive.findtext('complete/url'), archive.findtext('complete/checksum')))
    assert len(jobs) == 3, 'Expected three SDK packages'
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        for result in pool.map(install, jobs):
            print('Ready: ' + result, flush=True)

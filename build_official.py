"""Build registered Java hooks and original resources, without Gradle/NDK.
This is an OFFICIAL-APK legacy-Xposed research variant; no native repack layer.
Requires Python 3.11+, JDK and Android SDK.
The signing key persists in .work/zhiliao-signing. On Windows its password is
protected with current-user DPAPI; elsewhere set ZH_RESEARCH_KEYSTORE_PASSWORD.
"""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request
import secrets
import zipfile
from xml.etree import ElementTree as ET

HERE = Path(__file__).resolve().parent
ROOT = HERE
WORK = ROOT / '.work' / 'zhiliao-build'
VERSION = '26.10.08'
VERSION_CODE = '202610083'
SDK_VALUE = os.environ.get('ANDROID_SDK_ROOT', os.environ.get('ANDROID_HOME', ''))
SDK = Path(SDK_VALUE) if SDK_VALUE else None
ANDROID = 'http://schemas.android.com/apk/res/android'
PASSWORD = 'ZH_RESEARCH_KEYSTORE_PASSWORD'
SIGNING = ROOT / '.work' / 'zhiliao-signing'


def dpapi(data, decrypt=False):
    """Protect local signing credentials without logging or plaintext files."""
    import ctypes
    from ctypes import wintypes
    class Blob(ctypes.Structure):
        _fields_ = [('size', wintypes.DWORD), ('data', ctypes.c_void_p)]
    buffer = ctypes.create_string_buffer(data)
    source = Blob(len(data), ctypes.cast(buffer, ctypes.c_void_p))
    output = Blob()
    crypt = ctypes.WinDLL('crypt32', use_last_error=True)
    kernel = ctypes.WinDLL('kernel32', use_last_error=True)
    kernel.LocalFree.argtypes = [ctypes.c_void_p]
    fn = crypt.CryptUnprotectData if decrypt else crypt.CryptProtectData
    if not fn(ctypes.byref(source), None, None, None, None, 1, ctypes.byref(output)):
        raise ctypes.WinError(ctypes.get_last_error())
    try:
        return ctypes.string_at(output.data, output.size)
    finally:
        kernel.LocalFree(output.data)


def prepare_signing():
    SIGNING.mkdir(parents=True, exist_ok=True)
    protected = SIGNING / 'password.dpapi'
    if not os.environ.get(PASSWORD):
        if os.name != 'nt':
            raise RuntimeError('Set ZH_RESEARCH_KEYSTORE_PASSWORD in the environment')
        if protected.exists():
            os.environ[PASSWORD] = dpapi(protected.read_bytes(), decrypt=True).decode('utf-8')
        elif (SIGNING / 'research.jks').exists():
            raise RuntimeError('Signing credential missing; supply the original password')
        else:
            os.environ[PASSWORD] = secrets.token_urlsafe(32)
    if os.name == 'nt' and not protected.exists():
        protected.write_bytes(dpapi(os.environ[PASSWORD].encode('utf-8')))


def run(command):
    result = subprocess.run([str(x) for x in command], capture_output=True, text=True,
                            encoding='utf-8', errors='replace')
    if result.returncode:
        # Secrets are passed through environment references, never through CLI values.
        raise RuntimeError(f'{Path(str(command[0])).name} failed:\n{result.stdout}\n{result.stderr}')
    return result.stdout


def main():
    if SDK is None or not (SDK / 'platforms' / 'android-36' / 'android.jar').is_file():
        raise RuntimeError('Set ANDROID_SDK_ROOT to an SDK with platform android-36')
    prepare_signing()
    tools = SDK / 'build-tools' / '36.1.0'
    if not tools.is_dir():
        tools = SDK / 'build-tools' / '36.0.0'
    if not tools.is_dir(): raise RuntimeError('Install Android build-tools 36.0.0 or 36.1.0')
    exe = '.exe' if os.name == 'nt' else ''
    bat = '.bat' if os.name == 'nt' else ''
    android = SDK / 'platforms' / 'android-36' / 'android.jar'
    source = HERE / 'app' / 'src' / 'main'
    WORK.mkdir(parents=True, exist_ok=True)
    api = ROOT / '.work' / 'api-82.jar'
    if not api.exists():
        with urllib.request.urlopen('https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar', timeout=60) as r:
            api.write_bytes(r.read())
    directories = {name: WORK / name for name in ('classes', 'dex', 'generated-java', 'generated-res')}
    for d in directories.values():
        if d.exists(): shutil.rmtree(d)
        d.mkdir()
    # Never modify the original manifest/resources just to supply Gradle-generated values.
    ET.register_namespace('android', ANDROID)
    manifest = ET.parse(source / 'AndroidManifest.xml')
    node = manifest.getroot()
    node.set('package', 'com.shatyuka.zhiliao')
    node.set('{' + ANDROID + '}versionCode', VERSION_CODE)
    node.set('{' + ANDROID + '}versionName', VERSION)
    ET.SubElement(node, 'uses-sdk', {'{' + ANDROID + '}minSdkVersion': '26',
                                   '{' + ANDROID + '}targetSdkVersion': '36'})
    manifest_file = WORK / 'AndroidManifest.xml'
    manifest.write(manifest_file, encoding='utf-8', xml_declaration=True)
    values = directories['generated-res'] / 'values'
    values.mkdir()
    resources = ET.Element('resources')
    ET.SubElement(resources, 'string', {'name': 'app_version'}).text = VERSION
    ET.SubElement(resources, 'string', {'name': 'app_name'}).text = '知了'
    ET.ElementTree(resources).write(values / 'build.xml', encoding='utf-8', xml_declaration=True)
    res_zip, generated_zip = WORK / 'resources.zip', WORK / 'generated.zip'
    run([tools / ('aapt2' + exe), 'compile', '--dir', source / 'res', '-o', res_zip])
    run([tools / ('aapt2' + exe), 'compile', '--dir', directories['generated-res'], '-o', generated_zip])
    unsigned = WORK / 'unsigned.apk'
    run([tools / ('aapt2' + exe), 'link', '-o', unsigned, '--manifest', manifest_file,
         '-I', android, '--java', directories['generated-java'], '--auto-add-overlay',
         '-R', res_zip, '-R', generated_zip, '-A', source / 'assets'])
    java = list((source / 'java').rglob('*.java')) + list(directories['generated-java'].rglob('*.java'))
    cp = os.pathsep.join([str(android), str(api)])
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp,
         '-d', directories['classes'], *java])
    jar = WORK / 'module.jar'
    run(['jar', 'cf', jar, '-C', directories['classes'], '.'])
    run([tools / ('d8' + bat), '--min-api', '26', '--lib', android, '--classpath', api,
         '--output', directories['dex'], jar])
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as apk:
        for dex in directories['dex'].glob('*.dex'): apk.write(dex, dex.name)
    aligned = WORK / 'aligned.apk'
    run([tools / ('zipalign' + exe), '-f', '4', unsigned, aligned])
    key = SIGNING / 'research.jks'
    output = ROOT / 'zhiliao-11.10.0-arm64-r8.apk'
    if not key.exists():
        run(['keytool', '-genkeypair', '-keystore', key, '-storetype', 'JKS', '-alias', 'research',
             '-storepass:env', PASSWORD, '-keypass:env', PASSWORD, '-keyalg', 'RSA', '-keysize', '2048',
             '-validity', '3650', '-dname', 'CN=Local Research Only'])
    run([tools / ('apksigner' + bat), 'sign', '--ks', key, '--ks-key-alias', 'research',
         '--ks-pass', 'env:' + PASSWORD, '--key-pass', 'env:' + PASSWORD, '--out', output, aligned])
    run([tools / ('apksigner' + bat), 'verify', output])
    print('BUILD OK:', output)
    print(f'Compiled {len(java)} Java files with all original resources; no native libraries.')
    print('Java compilation, DEX packaging and signature verified; runtime NOT verified.')

if __name__ == '__main__':
    try: main()
    except Exception as e:
        print(e, file=sys.stderr)
        sys.exit(1)

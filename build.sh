#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

ANDROID_JAR="libs/android.jar"
if [ ! -f "$ANDROID_JAR" ]; then
    if [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platforms/android-34/android.jar" ]; then
        ANDROID_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
    elif [ -n "$ANDROID_SDK_ROOT" ] && [ -f "$ANDROID_SDK_ROOT/platforms/android-34/android.jar" ]; then
        ANDROID_JAR="$ANDROID_SDK_ROOT/platforms/android-34/android.jar"
    elif [ -f "/data/data/com.termux/files/usr/share/java/android.jar" ]; then
        ANDROID_JAR="/data/data/com.termux/files/usr/share/java/android.jar"
    else
        echo "[❌] android.jar not found! Please place android.jar in libs/ or set \$ANDROID_HOME."
        exit 1
    fi
fi

if [ ! -f debug.keystore ]; then
    echo "[i] Generating debug.keystore..."
    keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

echo "=== [1/6] Cleaning up and creating build directories ==="
rm -rf gen classes dex compiled_res.zip base.apk unaligned.apk aligned.apk HyperIcons.apk HyperIcons.apk.idsig
mkdir -p gen classes dex

echo "=== [2/6] Compiling resources with aapt2 and generating R.java ==="
aapt2 compile --dir res -o compiled_res.zip
aapt2 link -I "$ANDROID_JAR" --manifest AndroidManifest.xml --java gen/ -o base.apk compiled_res.zip

echo "=== [3/6] Compiling Java sources with javac ==="
javac -cp "$ANDROID_JAR:libs/api-82.jar" -d classes -source 8 -target 8 \
    gen/com/midori/hypericons/R.java \
    src/com/midori/hypericons/IconConfigProvider.java \
    src/com/midori/hypericons/MainActivity.java \
    src/com/midori/hypericons/ExperimentalActivity.java \
    src/com/midori/hypericons/CellularFeatureHook.java \
    src/com/midori/hypericons/MainHook.java

echo "=== [4/6] Compiling DEX with d8 ==="
d8 --release --min-api 26 --output dex/ classes/com/midori/hypericons/*.class

echo "=== [5/6] Packaging and aligning APK ==="
python3 -c "
import zipfile, shutil, struct

shutil.copyfile('base.apk', 'unaligned.apk')

with zipfile.ZipFile('unaligned.apk', 'a', compression=zipfile.ZIP_DEFLATED) as z:
    z.write('dex/classes.dex', 'classes.dex')
    z.writestr('assets/xposed_init', 'com.midori.hypericons.MainHook\n')
    z.writestr('META-INF/xposed/scope.list', 'com.android.systemui\ncom.android.phone\n')
    z.writestr('META-INF/xposed/java_init.list', 'com.midori.hypericons.MainHook\n')

print('[✓] unaligned.apk packaged with classes.dex & xposed entry points.')

def zipalign(in_path, out_path, alignment=4):
    with zipfile.ZipFile(in_path, 'r') as zin, open(out_path, 'wb') as fout:
        entries = []
        for item in zin.infolist():
            zin.fp.seek(item.header_offset)
            local_hdr = zin.fp.read(30)
            name_len = struct.unpack('<H', local_hdr[26:28])[0]
            extra_len = struct.unpack('<H', local_hdr[28:30])[0]
            name = zin.fp.read(name_len)
            extra = zin.fp.read(extra_len)
            data = zin.fp.read(item.compress_size)
            
            current_pos = fout.tell()
            pad = 0
            if item.compress_type == 0:
                data_offset = current_pos + 30 + len(name) + len(extra)
                remainder = data_offset % alignment
                if remainder != 0:
                    pad = alignment - remainder
                    extra += b'\x00' * pad
            
            new_hdr = bytearray(local_hdr)
            new_hdr[28:30] = struct.pack('<H', len(extra))
            
            new_header_offset = current_pos
            fout.write(new_hdr)
            fout.write(name)
            fout.write(extra)
            fout.write(data)
            
            entries.append((item, new_header_offset, extra))
            
        cd_offset = fout.tell()
        for item, hdr_offset, extra in entries:
            cd_hdr = bytearray(46)
            cd_hdr[0:4] = b'PK\x01\x02'
            cd_hdr[4:6] = struct.pack('<H', item.create_version)
            cd_hdr[6:8] = struct.pack('<H', item.extract_version)
            cd_hdr[8:10] = struct.pack('<H', item.flag_bits)
            cd_hdr[10:12] = struct.pack('<H', item.compress_type)
            cd_hdr[12:16] = struct.pack('<H', (item.date_time[0]-1980)<<9 | item.date_time[1]<<5 | item.date_time[2]) + struct.pack('<H', item.date_time[3]<<11 | item.date_time[4]<<5 | item.date_time[5]//2)
            cd_hdr[16:20] = struct.pack('<I', item.CRC)
            cd_hdr[20:24] = struct.pack('<I', item.compress_size)
            cd_hdr[24:28] = struct.pack('<I', item.file_size)
            cd_hdr[28:30] = struct.pack('<H', len(item.filename))
            cd_hdr[30:32] = struct.pack('<H', len(extra))
            cd_hdr[32:34] = struct.pack('<H', len(item.comment))
            cd_hdr[34:36] = b'\x00\x00'
            cd_hdr[36:38] = struct.pack('<H', item.internal_attr)
            cd_hdr[38:42] = struct.pack('<I', item.external_attr)
            cd_hdr[42:46] = struct.pack('<I', hdr_offset)
            
            fout.write(cd_hdr)
            fout.write(item.filename.encode('utf-8'))
            fout.write(extra)
            fout.write(item.comment)
            
        cd_size = fout.tell() - cd_offset
        eocd = bytearray(22)
        eocd[0:4] = b'PK\x05\x06'
        eocd[8:10] = struct.pack('<H', len(entries))
        eocd[10:12] = struct.pack('<H', len(entries))
        eocd[12:16] = struct.pack('<I', cd_size)
        eocd[16:20] = struct.pack('<I', cd_offset)
        fout.write(eocd)

zipalign('unaligned.apk', 'aligned.apk', 4)
print('[✓] 4-byte zipalign complete.')
"

echo "=== [6/6] Signing APK with apksigner ==="
apksigner sign --ks debug.keystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out HyperIcons.apk aligned.apk

apksigner verify -v HyperIcons.apk

echo "=== Distributing build artifacts ==="
if [ -d "/data/data/com.termux/files/home" ] && [ -w "/data/data/com.termux/files/home" ] && [ "$PROJECT_DIR" != "/data/data/com.termux/files/home" ]; then
    cp -f HyperIcons.apk /data/data/com.termux/files/home/HyperIcons.apk 2>/dev/null || true
fi
if [ -d "/sdcard/Download" ] && [ -w "/sdcard/Download" ]; then
    cp -f HyperIcons.apk /sdcard/Download/HyperIcons.apk 2>/dev/null && echo "[✓] Copied to /sdcard/Download/HyperIcons.apk" || true
else
    echo "[i] Installed artifact ready at: $PROJECT_DIR/HyperIcons.apk"
fi

echo "[SUCCESS] HyperIcons.apk build & sign complete!"

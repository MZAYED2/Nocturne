#!/usr/bin/env bash
set -e

# build_all_wff.sh
# Packages and validates Sage, Lunar, and Emerald WFF Push packages

PROJECT_ROOT="$(pwd)"
DIST_DIR="$PROJECT_ROOT/distribution"
mkdir -p "$DIST_DIR"
STUDIO_NAVY=false
if [[ "${1:-}" == "--studio-navy" ]]; then
    STUDIO_NAVY=true
    shift
fi
WFF_VERSION_CODE="${1:-1007}"
WFF_VERSION_NAME="${2:-1.0.7}"

# Locate build-tools
BUILD_TOOLS_DIR=$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)
if [ -f "$BUILD_TOOLS_DIR/zipalign.exe" ]; then
    ZIPALIGN="$BUILD_TOOLS_DIR/zipalign.exe"
else
    ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
fi
if [ -f "$BUILD_TOOLS_DIR/apksigner.bat" ]; then
    APKSIGNER="$BUILD_TOOLS_DIR/apksigner.bat"
else
    APKSIGNER="$BUILD_TOOLS_DIR/apksigner"
fi
VALIDATOR="$PROJECT_ROOT/tools/validator-push-cli.jar"
BASE_APK="$PROJECT_ROOT/watchface/build/outputs/apk/debug/watchface-debug.apk"

# Locate release keystore credentials from environment or local.properties
LOCAL_KEYPASS=""
LOCAL_STOREPASS=""
LOCAL_ALIAS="nocturne"
LOCAL_PATH=""

if [ -f "$PROJECT_ROOT/local.properties" ]; then
    LOCAL_KEYPASS=$(grep "^nocturne.key.password=" "$PROJECT_ROOT/local.properties" | head -n 1 | cut -d= -f2-)
    LOCAL_STOREPASS=$(grep "^nocturne.keystore.password=" "$PROJECT_ROOT/local.properties" | head -n 1 | cut -d= -f2-)
    LOCAL_ALIAS=$(grep "^nocturne.key.alias=" "$PROJECT_ROOT/local.properties" | head -n 1 | cut -d= -f2-)
    LOCAL_PATH=$(grep "^nocturne.keystore.path=" "$PROJECT_ROOT/local.properties" | head -n 1 | cut -d= -f2-)
fi

KEYSTORE="${NOCTURNE_KEYSTORE_PATH:-${LOCAL_PATH:-$PROJECT_ROOT/.signing/nocturne-release.jks}}"
KEYPASS="${NOCTURNE_KEY_PASSWORD:-$LOCAL_KEYPASS}"
STOREPASS="${NOCTURNE_KEYSTORE_PASSWORD:-$LOCAL_STOREPASS}"
ALIAS="${NOCTURNE_KEY_ALIAS:-${LOCAL_ALIAS:-nocturne}}"

if [ ! -f "$KEYSTORE" ]; then
    KEYSTORE="$HOME/.android/debug.keystore"
    KEYPASS="android"
    STOREPASS="android"
    ALIAS="androiddebugkey"
    if [ ! -f "$KEYSTORE" ]; then
        mkdir -p "$HOME/.android"
        keytool -genkey -v -keystore "$KEYSTORE" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
    fi
fi

# Detect Java 21+ executable
JAVA_CMD="java"
JAVAC_CMD="javac"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
    JAVAC_CMD="$JAVA_HOME/bin/javac"
elif [ -x "/c/Program Files/Android/Android Studio/jbr/bin/java" ]; then
    JAVA_CMD="/c/Program Files/Android/Android Studio/jbr/bin/java"
    JAVAC_CMD="/c/Program Files/Android/Android Studio/jbr/bin/javac"
elif [ -x "C:/Program Files/Android/Android Studio/jbr/bin/java.exe" ]; then
    JAVA_CMD="C:/Program Files/Android/Android Studio/jbr/bin/java.exe"
    JAVAC_CMD="C:/Program Files/Android/Android Studio/jbr/bin/javac.exe"
fi

# Compile PosixZipRepack
"$JAVAC_CMD" -d "$PROJECT_ROOT/tools" "$PROJECT_ROOT/tools/PosixZipRepack.java"

build_wff() {
    local NAME="$1"
    local ID="$2"
    local XML_FILE="$3"
    local OUT_APK="$4"
    local PKG_NAME="$5"
    local VCODE="$6"
    local VNAME="$7"
    local DESC="$8"

    echo "=================================================="
    echo "Building WFF Package: $NAME ($PKG_NAME, v$VNAME Code $VCODE)"
    echo "=================================================="

    ./gradlew :watchface:assembleDebug -PwffStudioNavy="$STUDIO_NAVY" -PwffPackageName="$PKG_NAME" -PwffVersionCode="$VCODE" -PwffVersionName="$VNAME" --no-configuration-cache --quiet --no-daemon

    local TEMP_DIR=$(mktemp -d)
    unzip -q "$BASE_APK" -d "$TEMP_DIR"

    # Copy generated drawables if any
    local GEN_DRAWABLE="$PROJECT_ROOT/watchface/build/generated/refinement/res/drawable"
    if [ -d "$GEN_DRAWABLE" ]; then
        cp -f "$GEN_DRAWABLE"/* "$TEMP_DIR/res/drawable/" 2>/dev/null || true
    fi

    # Replace watchface.xml
    mkdir -p "$TEMP_DIR/res/raw"
    if [[ "$STUDIO_NAVY" != true ]]; then
        cp "$XML_FILE" "$TEMP_DIR/res/raw/watchface.xml"
    fi

    # Remove non-WFF files
    rm -rf "$TEMP_DIR"/*.dex "$TEMP_DIR"/kotlin* "$TEMP_DIR"/META-INF/*.SF "$TEMP_DIR"/META-INF/*.RSA "$TEMP_DIR"/META-INF/*.DSA "$TEMP_DIR"/META-INF/*.EC "$TEMP_DIR"/META-INF/MANIFEST.MF 2>/dev/null || true

    local UNALIGNED=$(mktemp)
    local ALIGNED=$(mktemp)
    local FINAL_APK="$DIST_DIR/$OUT_APK"

    "$JAVA_CMD" -cp "$PROJECT_ROOT/tools" tools.PosixZipRepack "$TEMP_DIR" "$UNALIGNED"
    rm -rf "$TEMP_DIR"

    "$ZIPALIGN" -p -f 4 "$UNALIGNED" "$ALIGNED"
    rm -f "$UNALIGNED"

    "$APKSIGNER" sign --ks "$KEYSTORE" --ks-pass pass:"$STOREPASS" --ks-key-alias "$ALIAS" --key-pass pass:"$KEYPASS" --out "$FINAL_APK" "$ALIGNED"
    rm -f "$ALIGNED"

    echo "Signed: $FINAL_APK"

    # Validate and extract token (caller packageName is com.mohamedzayed.nocturne)
    local VAL_OUTPUT=$("$JAVA_CMD" -jar "$VALIDATOR" -a "$FINAL_APK" -p "com.mohamedzayed.nocturne" 2>&1)
    echo "$VAL_OUTPUT"

    local TOKEN=$(echo "$VAL_OUTPUT" | grep -oE "generated token: [^ ]+" | awk '{print $3}')
    if [ -z "$TOKEN" ]; then
        TOKEN=$(echo "$VAL_OUTPUT" | grep -oE "Validation token: [^ ]+" | awk '{print $3}')
    fi

    test -n "$TOKEN"
    echo "Token for $NAME ($PKG_NAME): $TOKEN"
    if [[ "$STUDIO_NAVY" == true ]]; then
        local SHA=$(sha256sum "$FINAL_APK" | cut -d' ' -f1)
        local SIZE=$(wc -c < "$FINAL_APK")
        printf '{"packageName":"%s","apk":"%s","token":"%s","bytes":%s,"sha256":"%s"}\n' \
            "$PKG_NAME" "$OUT_APK" "$TOKEN" "$SIZE" "$SHA" > "$DIST_DIR/Nocturne-Sage-Navy.json"
    fi
}

if [[ "$STUDIO_NAVY" == true ]]; then
    build_wff "Sage Navy" "sage_navy" "$PROJECT_ROOT/tools/watchface_sage.xml" "Nocturne-Sage-Navy-Push.apk" "com.mohamedzayed.nocturne.watchfacepush.sagenavy" "$WFF_VERSION_CODE" "$WFF_VERSION_NAME" "Studio Navy background"
    exit 0
fi

build_wff "Sage Nocturne" "sage_nocturne" "$PROJECT_ROOT/tools/watchface_sage.xml" "Nocturne-Sage-Push.apk" "com.mohamedzayed.nocturne.watchfacepush.sage" "$WFF_VERSION_CODE" "$WFF_VERSION_NAME" "Ottoman Sage Green heritage dial"
build_wff "Lunar Phase" "lunar_phase" "$PROJECT_ROOT/tools/watchface_lunar.xml" "Nocturne-Lunar-Push.apk" "com.mohamedzayed.nocturne.watchfacepush.lunar" "$WFF_VERSION_CODE" "$WFF_VERSION_NAME" "Large poetic astronomical moon disc"
build_wff "Obsidian Atelier" "obsidian_atelier" "$PROJECT_ROOT/tools/watchface_obsidian.xml" "Nocturne-Obsidian-Push.apk" "com.mohamedzayed.nocturne.watchfacepush.obsidian" "$WFF_VERSION_CODE" "$WFF_VERSION_NAME" "Mechanical luxury dial with small seconds and power reserve"

echo "All 3 Master WFF Packages Built and Validated Successfully!"

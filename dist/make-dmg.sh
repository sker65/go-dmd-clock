#!/bin/bash

# Simple DMG creation script

APP_NAME="Pin2Dmd-Editor"
APP_BUNDLE="${APP_NAME}.app"
DMG_NAME="${APP_NAME}-installer.dmg"
TEMP_DMG="${APP_NAME}-temp.dmg"
BACKGROUND_IMG="dmg_background.png"

DMG_WINDOW_WIDTH=600
DMG_WINDOW_HEIGHT=400
APP_ICON_X=150
APP_ICON_Y=200
APPLICATIONS_ICON_X=450
APPLICATIONS_ICON_Y=200

# Check if app bundle exists
if [ ! -d "$APP_BUNDLE" ]; then
    echo "Error: $APP_BUNDLE not found!"
    exit 1
fi

echo "Creating DMG for $APP_BUNDLE..."

# Remove existing DMG files
rm -f "$DMG_NAME" "$TEMP_DMG"

# Create temporary DMG
echo "Creating temporary DMG..."
hdiutil create -srcfolder "$APP_BUNDLE" -volname "$APP_NAME" -fs HFS+ \
    -fsargs "-c c=64,a=16,e=16" -format UDRW -size 200m "$TEMP_DMG"

# Mount the temporary DMG
echo "Mounting temporary DMG..."
#MOUNT_DIR=$(hdiutil attach -readwrite -noverify -noautoopen "$TEMP_DMG" | \
#    egrep '^/dev/' | sed 1q | awk '{print $3}')

hdiutil attach -readwrite -noverify -noautoopen "$TEMP_DMG"
MOUNT_DIR="/Volumes/$APP_NAME"

echo "Mounted at: $MOUNT_DIR"

# Create Applications symlink for easy installation
echo "Creating Applications symlink..."
ln -s /Applications "$MOUNT_DIR/Applications"

if [ -f "$BACKGROUND_IMG" ]; then
    echo "Step 4: Adding background image..."
    mkdir -p "$MOUNT_DIR/.background"
    cp "$BACKGROUND_IMG" "$MOUNT_DIR/.background/background.png"
else
    echo "Step 4: No background image found, using default"
fi


# Optional: Set custom icon for the DMG volume
if [ -f "dmg_icon.icns" ]; then
    cp dmg_icon.icns "$MOUNT_DIR/.VolumeIcon.icns"
    SetFile -c icnC "$MOUNT_DIR/.VolumeIcon.icns"
    SetFile -a C "$MOUNT_DIR"
fi

# Set the background and window properties (basic version)
echo "Setting DMG window properties..."

# Create .DS_Store for window positioning (optional)
cat > "$MOUNT_DIR/.DS_Store_template" << 'EOF'
# This would contain binary .DS_Store data
# For now, we'll skip custom positioning
EOF

echo "Step 6: Configuring Finder view..."
cat > /tmp/dmg_setup.scpt << EOF
tell application "Finder"
    tell disk "$APP_NAME"
        open
        set current view of container window to icon view
        set toolbar visible of container window to false
        set statusbar visible of container window to false
        set the bounds of container window to {100, 100, $(( 100 + DMG_WINDOW_WIDTH )), $(( 100 + DMG_WINDOW_HEIGHT ))}
        set viewOptions to the icon view options of container window
        set arrangement of viewOptions to not arranged
        set icon size of viewOptions to 128
        set background picture of viewOptions to file ".background:background.png"
        
        -- Position icons
        set position of item "$APP_BUNDLE" of container window to {$APP_ICON_X, $APP_ICON_Y}
        set position of item "Applications" of container window to {$APPLICATIONS_ICON_X, $APPLICATIONS_ICON_Y}
        
        close
        open
        update without registering applications
        delay 2
    end tell
end tell
EOF

# Run the AppleScript
osascript /tmp/dmg_setup.scpt

# Clean up AppleScript
rm /tmp/dmg_setup.scpt

echo "Step 7: Finalizing Finder settings..."
sync
sleep 2

# Unmount the temporary DMG
echo "Unmounting temporary DMG..."
hdiutil detach "$MOUNT_DIR"

# Convert to final compressed DMG
echo "Creating final compressed DMG..."
hdiutil convert "$TEMP_DMG" -format UDZO -imagekey zlib-level=9 -o "$DMG_NAME"

# Clean up
rm -f "$TEMP_DMG"

echo "✓ DMG created successfully: $DMG_NAME"

# Optional: Verify the DMG
echo "Verifying DMG..."
hdiutil verify "$DMG_NAME"

echo "Done! Your DMG is ready for distribution."
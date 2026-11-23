$srcPath = "$PSScriptRoot\..\watchface\src\main\res\raw\watchface.xml"
$lines = [System.IO.File]::ReadAllLines($srcPath)

$sb = [System.Text.StringBuilder]::new()

[void]$sb.AppendLine('<?xml version="1.0" encoding="utf-8"?>')
[void]$sb.AppendLine('<WatchFace clipShape="CIRCLE" width="1000" height="1000">')
[void]$sb.AppendLine('<Metadata key="CLOCK_TYPE" value="ANALOG"/>')
[void]$sb.AppendLine('<Metadata key="STEP_GOAL" value="10000"/>')
[void]$sb.AppendLine('<UserConfigurations>')
[void]$sb.AppendLine('<ListConfiguration id="watch_theme" displayName="theme_selection_label" defaultValue="sage_nocturne">')
[void]$sb.AppendLine('<ListOption id="sage_nocturne" displayName="theme_sage_nocturne"/>')
[void]$sb.AppendLine('<ListOption id="lunar_phase" displayName="theme_lunar_phase"/>')
[void]$sb.AppendLine('</ListConfiguration>')

# Copy existing UserConfigurations (lines 5 to 44)
for ($i = 5; $i -le 43; $i++) {
    [void]$sb.AppendLine($lines[$i])
}
[void]$sb.AppendLine('</UserConfigurations>')
[void]$sb.AppendLine('<Scene backgroundColor="#ff000000">')
[void]$sb.AppendLine('<PartDraw x="0" y="0" width="1000" height="1000" alpha="[CONFIGURATION.watch_theme] == ''lunar_phase'' ? 0 : 255">')
[void]$sb.AppendLine('<Variant mode="AMBIENT" target="alpha" value="0"/>')
[void]$sb.AppendLine('<Rectangle x="0" y="0" width="1000" height="1000">')
[void]$sb.AppendLine('<Fill color="[CONFIGURATION.background.0]"/>')
[void]$sb.AppendLine('</Rectangle>')
[void]$sb.AppendLine('</PartDraw>')

# Active Sage Group
[void]$sb.AppendLine('<Group name="active_sage" x="0" y="0" width="1000" height="1000" alpha="[CONFIGURATION.watch_theme] == ''lunar_phase'' ? 0 : 255">')
[void]$sb.AppendLine('<Variant mode="AMBIENT" target="alpha" value="0"/>')

# Copy theme conditions (lines 50 to 407)
for ($i = 50; $i -le 407; $i++) {
    [void]$sb.AppendLine($lines[$i])
}

# Accurate 29-phase small moon for Sage Nocturne
[void]$sb.AppendLine('<Condition>')
[void]$sb.AppendLine('<Expressions>')
[void]$sb.AppendLine('<Expression name="moon0">[MOON_PHASE_POSITION] &lt; 0.5 || [MOON_PHASE_POSITION] &gt;= 28.5</Expression>')
for ($p = 1; $p -le 27; $p++) {
    $lim = "$p.5"
    [void]$sb.AppendLine("<Expression name=`"moon$p`">[MOON_PHASE_POSITION] &lt; $lim</Expression>")
}
[void]$sb.AppendLine('</Expressions>')
for ($p = 0; $p -le 27; $p++) {
    $pad = $p.ToString("D2")
    [void]$sb.AppendLine("<Compare expression=`"moon$p`">")
    [void]$sb.AppendLine('<PartImage x="392" y="122" width="216" height="216" alpha="255">')
    [void]$sb.AppendLine("<Image resource=`"refined_moon_$pad`"/>")
    [void]$sb.AppendLine('</PartImage>')
    [void]$sb.AppendLine('</Compare>')
}
[void]$sb.AppendLine('<Default>')
[void]$sb.AppendLine('<PartImage x="392" y="122" width="216" height="216" alpha="255">')
[void]$sb.AppendLine('<Image resource="refined_moon_28"/>')
[void]$sb.AppendLine('</PartImage>')
[void]$sb.AppendLine('</Default>')
[void]$sb.AppendLine('</Condition>')
[void]$sb.AppendLine('</Group>')

# Active Lunar Phase Group
[void]$sb.AppendLine('<Group name="active_lunar" x="0" y="0" width="1000" height="1000" alpha="[CONFIGURATION.watch_theme] == ''lunar_phase'' ? 255 : 0">')
[void]$sb.AppendLine('<Variant mode="AMBIENT" target="alpha" value="0"/>')
[void]$sb.AppendLine('<Condition>')
[void]$sb.AppendLine('<Expressions>')
[void]$sb.AppendLine('<Expression name="lmoon0">[MOON_PHASE_POSITION] &lt; 0.5 || [MOON_PHASE_POSITION] &gt;= 28.5</Expression>')
for ($p = 1; $p -le 27; $p++) {
    $lim = "$p.5"
    [void]$sb.AppendLine("<Expression name=`"lmoon$p`">[MOON_PHASE_POSITION] &lt; $lim</Expression>")
}
[void]$sb.AppendLine('</Expressions>')
for ($p = 0; $p -le 27; $p++) {
    $pad = $p.ToString("D2")
    [void]$sb.AppendLine("<Compare expression=`"lmoon$p`">")
    [void]$sb.AppendLine('<PartImage x="40" y="40" width="920" height="920" alpha="255">')
    [void]$sb.AppendLine("<Image resource=`"lunar_moon_$pad`"/>")
    [void]$sb.AppendLine('</PartImage>')
    [void]$sb.AppendLine('</Compare>')
}
[void]$sb.AppendLine('<Default>')
[void]$sb.AppendLine('<PartImage x="40" y="40" width="920" height="920" alpha="255">')
[void]$sb.AppendLine('<Image resource="lunar_moon_28"/>')
[void]$sb.AppendLine('</PartImage>')
[void]$sb.AppendLine('</Default>')
[void]$sb.AppendLine('</Condition>')
[void]$sb.AppendLine('</Group>')

# Copy ComplicationSlots (lines 594 to 764) with alpha configuration
for ($i = 594; $i -le 764; $i++) {
    $line = $lines[$i]
    if ($line -match '<ComplicationSlot slotId="(\d)"') {
        $slotId = $matches[1]
        $line = $line -replace '<ComplicationSlot slotId="\d"', "<ComplicationSlot slotId=`"$slotId`" alpha=`"[CONFIGURATION.watch_theme] == 'lunar_phase' ? 0 : 255`""
    }
    [void]$sb.AppendLine($line)
}

# Active Foreground Sage (lines 766 to 885)
[void]$sb.AppendLine('<Group name="active_foreground_sage" x="0" y="0" width="1000" height="1000" alpha="[CONFIGURATION.watch_theme] == ''lunar_phase'' ? 0 : 255">')
[void]$sb.AppendLine('<Variant mode="AMBIENT" target="alpha" value="0"/>')
for ($i = 768; $i -le 885; $i++) {
    [void]$sb.AppendLine($lines[$i])
}
[void]$sb.AppendLine('</Group>')

# Active Foreground Lunar Phase (Analog clock with luminous & charcoal high-contrast hands)
[void]$sb.AppendLine('<Group name="active_foreground_lunar" x="0" y="0" width="1000" height="1000" alpha="[CONFIGURATION.watch_theme] == ''lunar_phase'' ? 255 : 0">')
[void]$sb.AppendLine('<Variant mode="AMBIENT" target="alpha" value="0"/>')
[void]$sb.AppendLine('<AnalogClock x="0" y="0" width="1000" height="1000">')
[void]$sb.AppendLine('<HourHand resource="lunar_hour" x="480" y="250" width="40" height="280" pivotX="0.5" pivotY="0.8928571428571429"/>')
[void]$sb.AppendLine('<MinuteHand resource="lunar_minute" x="483" y="150" width="34" height="380" pivotX="0.5" pivotY="0.9210526315789473"/>')
[void]$sb.AppendLine('<SecondHand resource="lunar_second" x="492" y="140" width="16" height="395" pivotX="0.5" pivotY="0.9113924050632911"/>')
[void]$sb.AppendLine('</AnalogClock>')
[void]$sb.AppendLine('<PartImage x="477" y="477" width="46" height="46">')
[void]$sb.AppendLine('<Image resource="lunar_hub"/>')
[void]$sb.AppendLine('</PartImage>')
[void]$sb.AppendLine('</Group>')

# Ambient Sage Group (lines 888 to 1126)
[void]$sb.AppendLine('<Group name="ambient_sage" x="0" y="0" width="1000" height="1000" alpha="0">')
[void]$sb.AppendLine('<Variant mode="AMBIENT" target="alpha" value="[CONFIGURATION.watch_theme] == ''lunar_phase'' ? 0 : 255"/>')
for ($i = 891; $i -le 1126; $i++) {
    [void]$sb.AppendLine($lines[$i])
}
[void]$sb.AppendLine('</Group>')

# Ambient Lunar Phase Group (Black background, moonlight numerals & ticks, matching pivot hands)
[void]$sb.AppendLine('<Group name="ambient_lunar" x="0" y="0" width="1000" height="1000" alpha="0">')
[void]$sb.AppendLine('<Variant mode="AMBIENT" target="alpha" value="[CONFIGURATION.watch_theme] == ''lunar_phase'' ? 255 : 0"/>')
[void]$sb.AppendLine('<PartImage x="0" y="0" width="1000" height="1000">')
[void]$sb.AppendLine('<Image resource="lunar_dial_aod"/>')
[void]$sb.AppendLine('</PartImage>')
[void]$sb.AppendLine('<AnalogClock x="0" y="0" width="1000" height="1000">')
[void]$sb.AppendLine('<HourHand resource="lunar_hour_aod" x="480" y="250" width="40" height="280" pivotX="0.5" pivotY="0.8928571428571429"/>')
[void]$sb.AppendLine('<MinuteHand resource="lunar_minute_aod" x="483" y="150" width="34" height="380" pivotX="0.5" pivotY="0.9210526315789473"/>')
[void]$sb.AppendLine('</AnalogClock>')
[void]$sb.AppendLine('<PartImage x="477" y="477" width="46" height="46">')
[void]$sb.AppendLine('<Image resource="lunar_hub"/>')
[void]$sb.AppendLine('</PartImage>')
[void]$sb.AppendLine('</Group>')

[void]$sb.AppendLine('</Scene>')
[void]$sb.AppendLine('</WatchFace>')

[System.IO.File]::WriteAllText($srcPath, $sb.ToString(), [System.Text.Encoding]::UTF8)
Write-Output "Successfully generated updated watchface.xml"

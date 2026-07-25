# Mechanical 26.2 API renames for DupeClient (run on branch 26.2 only).
$roots = @(
    "src/main/java",
    "src/gui-default/java",
    "src/waypoint-default/java",
    "src/macrocycling-default/java",
    "src/screenshot-bootstrap-default/java"
)

$files = foreach ($root in $roots) {
    if (Test-Path $root) {
        Get-ChildItem $root -Recurse -Filter "*.java" -File
    }
}

$replacements = [ordered]@{
    'Minecraft\.getInstance\(\)\.setScreen\(' = 'Minecraft.getInstance().gui.setScreen('
    'Minecraft\.getInstance\(\)\.screen\b' = 'Minecraft.getInstance().gui.screen()'
    'this\.minecraft\.setScreen\(' = 'this.minecraft.gui.setScreen('
    'this\.minecraft\.screen\b' = 'this.minecraft.gui.screen()'
    '\bclient\.setScreen\(' = 'client.gui.setScreen('
    '\bminecraft\.setScreen\(' = 'minecraft.gui.setScreen('
    '\bmc\.setScreen\(' = 'mc.gui.setScreen('
    '\bc\.setScreen\(' = 'c.gui.setScreen('
    '\bclient\.screen\b' = 'client.gui.screen()'
    '\bminecraft\.screen\b' = 'minecraft.gui.screen()'
    '\bmc\.screen\b' = 'mc.gui.screen()'
    '\bc\.screen\b' = 'c.gui.screen()'
    '\.getToastManager\(\)' = '.gui.toastManager()'
    '\.getMainRenderTarget\(\)' = '.gameRenderer.mainRenderTarget()'
    'SystemToast\.multiline\(\s*(\w+)\s*,' = 'SystemToast.add($1.gui.toastManager(),'
    '\.levelRenderer\.allChanged\(\)' = '.levelRenderer.invalidateCompiledGeometry($1.level, $1.options, $1.gameRenderer.getMainCamera(), $1.blockColors)'
}

# Fix invalidateCompiledGeometry - the replacement above is wrong. Handle separately.
$replacements.Remove('.levelRenderer.allChanged()')

$count = 0
foreach ($file in $files) {
    $text = [IO.File]::ReadAllText($file.FullName)
    $original = $text
    foreach ($pair in $replacements.GetEnumerator()) {
        $text = [regex]::Replace($text, $pair.Key, $pair.Value)
    }
    $text = [regex]::Replace(
        $text,
        '(\w+)\.levelRenderer\.allChanged\(\)',
        '/* 26.2: allChanged removed */'
    )
    if ($text -ne $original) {
        [IO.File]::WriteAllText($file.FullName, $text)
        $count++
    }
}
Write-Host "Updated $count files"

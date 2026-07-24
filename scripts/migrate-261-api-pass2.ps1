# Second-pass 26.1 API fixes (run on branch 26.1 only).
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

$safeReplacements = [ordered]@{
    '\.displayClientMessage\(([^,)]+),\s*false\)' = '.sendSystemMessage($1)'
    '\.displayClientMessage\(([^,)]+),\s*true\)' = '.sendOverlayMessage($1)'
    '\.hLine\(' = '.horizontalLine('
    '\.vLine\(' = '.verticalLine('
    'PlayerFaceExtractor\.draw\(' = 'PlayerFaceExtractor.extractRenderState('
    'handleInventoryMouseClick\(' = 'handleContainerInput('
    '\.clickType\(\)' = '.containerInput()'
    'setQuickMovingStack\(' = 'setLastQuickMoved('
    'protected void renderWidget\(' = 'protected void extractWidgetRenderState('
    'public void renderWidget\(' = 'public void extractWidgetRenderState('
    'super\.renderWidget\(' = 'super.extractWidgetRenderState('
    'new CharacterEvent\(([^,]+),\s*[^)]+\)' = 'new CharacterEvent($1)'
    '\.getItem\(\)\.getName\(\)\.getString\(\)' = '.getHoverName().getString()'
    'MathHelper\.' = 'Mth.'
    'import net\.minecraft\.util\.math\.MathHelper;' = 'import net.minecraft.util.Mth;'
    'Util\.getOperatingSystem\(\)\.open\(' = 'Util.getPlatform().openUri('
    '(?<![\w])searchBox\.render\(' = 'searchBox.extractRenderState('
    '(?<![\w])instance\.render\(' = 'instance.extractRenderState('
}

$count = 0
foreach ($file in $files) {
    $text = [IO.File]::ReadAllText($file.FullName)
    $original = $text

    foreach ($pair in $safeReplacements.GetEnumerator()) {
        $text = [regex]::Replace($text, $pair.Key, $pair.Value)
    }

    $isScreen = $text -match 'extends\s+\w*Screen\b'
    if (-not $isScreen) {
        $text = [regex]::Replace($text, '\bextractRenderState\b', 'render')
    } else {
        # Restore Screen lifecycle method names.
        $text = [regex]::Replace($text, '(@Override\s+(?:protected\s+|public\s+)?void\s+)render(\s*\(\s*GuiGraphicsExtractor)', '$1extractRenderState$2')
        $text = [regex]::Replace($text, 'super\.render\(\s*context', 'super.extractRenderState(context')
        $text = [regex]::Replace($text, '(@Override\s+(?:protected\s+|public\s+)?void\s+)renderBackground(\s*\(\s*GuiGraphicsExtractor)', '$1extractBackground$2')
        $text = [regex]::Replace($text, 'super\.renderBackground\(', 'super.extractBackground(')
        $text = [regex]::Replace($text, '(?<![\w\.])renderBackground\(\s*context', 'extractBackground(context')
    }

    if ($text -ne $original) {
        [IO.File]::WriteAllText($file.FullName, $text)
        $count++
    }
}

Write-Host "Updated $count files"

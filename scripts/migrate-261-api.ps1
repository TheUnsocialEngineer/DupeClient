# Mechanical 26.1 API renames for DupeClient sources (run on branch 26.1 only).
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
    'net\.fabricmc\.fabric\.api\.client\.keybinding\.v1\.KeyBindingHelper' = 'net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper'
    'net\.fabricmc\.fabric\.api\.client\.keybinding\.v1' = 'net.fabricmc.fabric.api.client.keymapping.v1'
    'KeyBindingHelper' = 'KeyMappingHelper'
    'registerKeyBinding' = 'registerKeyMapping'
    'net\.fabricmc\.fabric\.api\.client\.command\.v2\.ClientCommandManager' = 'net.fabricmc.fabric.api.client.command.v2.ClientCommands'
    'ClientCommandManager' = 'ClientCommands'
    'net\.fabricmc\.fabric\.api\.client\.rendering\.v1\.world\.WorldRenderEvents' = 'net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents'
    'net\.fabricmc\.fabric\.api\.client\.rendering\.v1\.world\.WorldRenderContext' = 'net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext'
    'WorldRenderEvents' = 'LevelRenderEvents'
    'WorldRenderContext' = 'LevelRenderContext'
    'net\.minecraft\.client\.gui\.GuiGraphics\b' = 'net.minecraft.client.gui.GuiGraphicsExtractor'
    '\bGuiGraphics\b' = 'GuiGraphicsExtractor'
    '\.drawString\(' = '.text('
    'public void renderBackground\(GuiGraphicsExtractor' = 'public void extractBackground(GuiGraphicsExtractor'
    'public void render\(GuiGraphicsExtractor' = 'public void extractRenderState(GuiGraphicsExtractor'
    'void render\(GuiGraphicsExtractor' = 'void extractRenderState(GuiGraphicsExtractor'
    'super\.renderBackground\(' = 'super.extractBackground('
    'super\.render\(' = 'super.extractRenderState('
    'method = "renderBackground"' = 'method = "extractBackground"'
    'method = "render"' = 'method = "extractRenderState"'
    'method = "renderWithTooltipAndSubtitles"' = 'method = "extractRenderStateWithTooltipAndSubtitles"'
    'method = "renderContents"' = 'method = "extractContents"'
    'Lnet/minecraft/client/gui/GuiGraphics;' = 'Lnet/minecraft/client/gui/GuiGraphicsExtractor;'
    'HudRenderCallback' = 'HudElementRegistry'
}

$count = 0
foreach ($file in $files) {
    $text = [IO.File]::ReadAllText($file.FullName)
    $original = $text
    foreach ($pair in $replacements.GetEnumerator()) {
        $text = [regex]::Replace($text, $pair.Key, $pair.Value)
    }
    if ($text -ne $original) {
        [IO.File]::WriteAllText($file.FullName, $text)
        $count++
    }
}
Write-Host "Updated $count files"

package app.revanced.patches.youtube.layout.seekbar

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.misc.mapping.get
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.mapping.resourceMappings
import app.revanced.patches.youtube.layout.theme.lithoColorHookPatch
import app.revanced.patches.youtube.layout.theme.lithoColorOverrideHook
import app.revanced.patches.youtube.misc.integrations.integrationsPatch
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Element

private const val INTEGRATIONS_CLASS_DESCRIPTOR = "Lapp/revanced/integrations/youtube/patches/theme/SeekbarColorPatch;"

val seekbarColorPatch = bytecodePatch(
    description = "Hide or set a custom seekbar color",
) {
    dependsOn(
        integrationsPatch,
        lithoColorHookPatch,
        seekbarColorResourcePatch,
    )

    val playerSeekbarColorResult by playerSeekbarColorFingerprint
    val shortsSeekbarColorResult by shortsSeekbarColorFingerprint
    val setSeekbarClickedColorResult by setSeekbarClickedColorFingerprint

    execute { context ->
        fun MutableMethod.addColorChangeInstructions(resourceId: Long) {
            val registerIndex = indexOfFirstWideLiteralInstructionValueOrThrow(resourceId) + 2
            val colorRegister = getInstruction<OneRegisterInstruction>(registerIndex).registerA
            addInstructions(
                registerIndex + 1,
                """
                    invoke-static { v$colorRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getVideoPlayerSeekbarColor(I)I
                    move-result v$colorRegister
                """,
            )
        }

        playerSeekbarColorResult.mutableMethod.apply {
            addColorChangeInstructions(inlineTimeBarColorizedBarPlayedColorDarkId)
            addColorChangeInstructions(inlineTimeBarPlayedNotHighlightedColorId)
        }

        shortsSeekbarColorResult.mutableMethod.apply {
            addColorChangeInstructions(reelTimeBarPlayedColorId)
        }

        setSeekbarClickedColorResult.mutableMethod.let {
            val setColorMethodIndex = setSeekbarClickedColorResult.scanResult.patternScanResult!!.startIndex + 1
            val method = context.navigate(it).at(setColorMethodIndex).mutable()

            method.apply {
                val colorRegister = getInstruction<TwoRegisterInstruction>(0).registerA
                addInstructions(
                    0,
                    """
                        invoke-static { v$colorRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getVideoPlayerSeekbarClickedColor(I)I
                        move-result v$colorRegister
                    """,
                )
            }
        }

        lithoColorOverrideHook(INTEGRATIONS_CLASS_DESCRIPTOR, "getLithoColor")
    }
}

internal var reelTimeBarPlayedColorId = -1L
    private set
internal var inlineTimeBarColorizedBarPlayedColorDarkId = -1L
    private set
internal var inlineTimeBarPlayedNotHighlightedColorId = -1L
    private set

internal val seekbarColorResourcePatch = resourcePatch {
    dependsOn(
        settingsPatch,
        resourceMappingPatch,
    )

    execute { context ->
        reelTimeBarPlayedColorId = resourceMappings[
            "color",
            "reel_time_bar_played_color",
        ]
        inlineTimeBarColorizedBarPlayedColorDarkId = resourceMappings[
            "color",
            "inline_time_bar_colorized_bar_played_color_dark",
        ]
        inlineTimeBarPlayedNotHighlightedColorId = resourceMappings[
            "color",
            "inline_time_bar_played_not_highlighted_color",
        ]

        // Edit the resume playback drawable and replace the progress bar with a custom drawable
        context.document["res/drawable/resume_playback_progressbar_drawable.xml"].use { document ->

            val layerList = document.getElementsByTagName("layer-list").item(0) as Element
            val progressNode = layerList.getElementsByTagName("item").item(1) as Element
            if (!progressNode.getAttributeNode("android:id").value.endsWith("progress")) {
                throw PatchException("Could not find progress bar")
            }
            val scaleNode = progressNode.getElementsByTagName("scale").item(0) as Element
            val shapeNode = scaleNode.getElementsByTagName("shape").item(0) as Element
            val replacementNode = document.createElement(
                "app.revanced.integrations.youtube.patches.theme.ProgressBarDrawable",
            )
            scaleNode.replaceChild(replacementNode, shapeNode)
        }
    }
}
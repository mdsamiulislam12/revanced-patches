package app.revanced.patches.reddit.customclients.joeyforreddit.api

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import app.revanced.patcher.fingerprint.methodFingerprint

internal val authUtilityUserAgentFingerprint = methodFingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("Ljava/lang/String;")
    opcodes(Opcode.APUT_OBJECT)
    custom { method, classDef ->
        classDef.sourceFile == "AuthUtility.java"
    }
}

internal val getClientIdFingerprint = methodFingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("L")
    opcodes(
        Opcode.CONST,               // R.string.valuable_cid
        Opcode.INVOKE_STATIC,       // StringMaster.decrypt
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT
    )
    custom { _, classDef ->
        classDef.sourceFile == "AuthUtility.java"
    }
}
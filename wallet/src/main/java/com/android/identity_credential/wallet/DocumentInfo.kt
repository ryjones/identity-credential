package com.android.identity_credential.wallet

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant

@Immutable
data class DocumentInfo(
    // The identifier of for the document
    val documentId: String,

    // If set, indicate that attention from the user is needed to remediate a problem
    // with the document.
    val attentionNeeded: Boolean,

    // Display string for the document
    val name: String,

    // Display string for the type of the document
    val typeName: String,

    // Document artwork.
    val documentArtwork: Bitmap,

    // Display string for the issuer of the document.
    val issuerName: String,

    // Display string for issuer's description of the document.
    val issuerDocumentDescription: String,

    // The logo from the issuer.
    val issuerLogo: Bitmap,

    // Point in time that information for the document was last refreshed from the issuer
    val lastRefresh: Instant,

    // Human-readable string explaining the user what state the document is in.
    val status: String,

    // Data attributes
    val attributes: Map<String, String>,

    // Data attribute: Portrait of the document holder, if available
    val attributePortrait: Bitmap?,

    // Data attribute: Signature or usual mark of the holder, if available
    val attributeSignatureOrUsualMark: Bitmap?,

    // A list of the underlying credentials
    val credentialInfos: List<CredentialInfo>
)
